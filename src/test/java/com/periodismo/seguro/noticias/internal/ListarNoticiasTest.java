package com.periodismo.seguro.noticias.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.periodismo.seguro.noticias.ImagenAdjunta;
import com.periodismo.seguro.noticias.NoticiasApi;
import com.periodismo.seguro.noticias.ResumenNoticia;
import com.periodismo.seguro.noticias.SolicitudNoticia;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/** HU-10: feed paginado, orden determinista, extracto en texto plano y tolerancia a imagenes perdidas. */
@SpringBootTest
@TestPropertySource(properties = {
    "app.imagenes.directorio=target/imagenes-prueba-listado",
    // H2 propia: por defecto todas las pruebas comparten mem:periodismo y se pisan entre clases.
    "spring.datasource.url=jdbc:h2:mem:listadohu10;DB_CLOSE_DELAY=-1"
})
class ListarNoticiasTest {
    private static final Set<String> PRIVADOS = Set.of("correo", "nombreReal", "claveHash", "ip", "cuerpo");
    private static final Path DIRECTORIO =
        Paths.get("target/imagenes-prueba-listado").toAbsolutePath().normalize();

    @Autowired NoticiasApi api;
    @Autowired NoticiaRepository noticias;
    @Autowired ImagenNoticiaRepository imagenesNoticia;

    private static byte[] png() throws IOException {
        BufferedImage imagen = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        ImageIO.write(imagen, "png", salida);
        return salida.toByteArray();
    }

    private String crear(String titulo, String cuerpo, List<ImagenAdjunta> imagenes) {
        return api.crear("cuenta-feed", "Condor Silente 482",
            new SolicitudNoticia(titulo, "Subtitulo", cuerpo, "otros", imagenes));
    }

    private ResumenNoticia resumenDe(String id) {
        for (int pagina = 0; pagina < 20; pagina++) {
            List<ResumenNoticia> lote = api.listar(pagina, 50);
            Optional<ResumenNoticia> encontrado = lote.stream().filter(n -> n.id().equals(id)).findFirst();
            if (encontrado.isPresent()) {
                return encontrado.get();
            }
            if (lote.size() < 50) {
                break;
            }
        }
        throw new AssertionError("No se encontro la noticia " + id);
    }

    @Test
    void elFeedOrdenaPorMasRecienteYDesempataPorIdDeFormaEstable() {
        Instant reciente = Instant.parse("2099-01-02T10:00:00Z");
        Instant antigua = Instant.parse("2099-01-01T10:00:00Z");
        noticias.save(new Noticia("a-empate", "c", "Condor Silente 482", "Empate", "Sub", "<p>x</p>", "otros", reciente));
        noticias.save(new Noticia("c-reciente", "c", "Condor Silente 482", "Reciente", "Sub", "<p>x</p>", "otros", reciente));
        noticias.save(new Noticia("b-antigua", "c", "Condor Silente 482", "Antigua", "Sub", "<p>x</p>", "otros", antigua));

        List<String> primera = api.listar(0, 50).stream().map(ResumenNoticia::titulo).toList();
        assertEquals(List.of("Empate", "Reciente", "Antigua"), primera.subList(0, 3));
        assertEquals(primera, api.listar(0, 50).stream().map(ResumenNoticia::titulo).toList(),
            "el orden no cambia entre peticiones");
    }

    @Test
    void elExtractoEsTextoPlanoYElResumenNoExponeDatosPrivados() {
        String id = crear("Titulo del extracto", "<p>Hola <strong>mundo</strong></p><ul><li>Uno</li></ul>", List.of());

        ResumenNoticia resumen = resumenDe(id);
        assertEquals("Hola mundo Uno", resumen.extracto());
        assertFalse(resumen.extracto().contains("<"));
        assertEquals("Condor Silente 482", resumen.autorSeudonimo());
        assertTrue(Arrays.stream(ResumenNoticia.class.getRecordComponents())
            .noneMatch(c -> PRIVADOS.contains(c.getName())));
        assertTrue(resumen.publicadaEn().matches("\\d{2}/\\d{2}/\\d{4} · \\d{2}:00 UTC"),
            "hora redondeada a 1 h: " + resumen.publicadaEn());
    }

    @Test
    void laPaginacionNormalizaValoresYRespetaElTopeDe50() {
        long antes = api.contar();
        for (int i = 0; i < 12; i++) {
            crear("Nota " + i, "<p>Texto</p>", List.of());
        }

        assertEquals(antes + 12, api.contar());
        assertEquals(10, api.listar(0, 10).size());
        assertEquals(Math.min(10, antes + 12 - 10), api.listar(1, 10).size());
        assertEquals(Math.min(antes + 12, 50), api.listar(-5, 999).size());
        assertTrue(api.listar(999, 10).isEmpty());
    }

    @Test
    void siElArchivoDeImagenDesapareceLaMiniaturaSigueDesdeLaBase() throws IOException {
        String id = crear("Nota con foto", "<p>Texto</p>",
            List.of(new ImagenAdjunta("image/png", png())));
        ImagenNoticia imagen = imagenesNoticia.findFirstByNoticiaIdOrderByOrdenAsc(id).orElseThrow();
        assertEquals(imagen.getId(), resumenDe(id).primeraImagenId());

        Files.deleteIfExists(DIRECTORIO.resolve(imagen.getNombreGuardado()));

        assertEquals(imagen.getId(), resumenDe(id).primeraImagenId());
        assertEquals("Nota con foto", resumenDe(id).titulo());
    }
}
