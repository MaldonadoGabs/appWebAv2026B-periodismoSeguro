package com.periodismo.seguro.noticias.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.periodismo.seguro.noticias.ArchivoImagen;
import com.periodismo.seguro.noticias.ImagenAdjunta;
import com.periodismo.seguro.noticias.NoticiaCreada;
import com.periodismo.seguro.noticias.NoticiaPublica;
import com.periodismo.seguro.noticias.NoticiasApi;
import com.periodismo.seguro.noticias.SolicitudNoticia;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

/** HU-09: crear noticias con encabezado, subtitulo, texto e imagenes, sin exponer la identidad real. */
@SpringBootTest
@TestPropertySource(properties = "app.imagenes.directorio=target/imagenes-prueba")
@RecordApplicationEvents
class CrearNoticiaTest {
    private static final Set<String> PRIVADOS = Set.of("correo", "nombreReal", "claveHash", "ip");

    @Autowired NoticiasApi api;
    @Autowired NoticiaRepository noticias;
    @Autowired ImagenNoticiaRepository imagenesNoticia;
    @Autowired ApplicationEvents eventos;

    private static byte[] png() throws IOException {
        BufferedImage imagen = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        ImageIO.write(imagen, "png", salida);
        return salida.toByteArray();
    }

    private static SolicitudNoticia solicitud(String cuerpo, List<ImagenAdjunta> imagenes) {
        return new SolicitudNoticia("Encabezado de prueba", "Subtitulo de prueba", cuerpo, "otros", imagenes);
    }

    @Test
    void crearGuardaEncabezadoSubtituloTextoYSeudonimo() {
        String id = api.crear("cuenta-1", "Condor Silente 482", solicitud("<p>Texto de la nota</p>", List.of()));

        Noticia guardada = noticias.findById(id).orElseThrow();
        assertEquals("Encabezado de prueba", guardada.getTitulo());
        assertEquals("Subtitulo de prueba", guardada.getSubtitulo());
        assertEquals("<p>Texto de la nota</p>", guardada.getCuerpo());
        assertEquals("cuenta-1", guardada.getAutorCuentaId());
        assertEquals("Condor Silente 482", guardada.getAutorSeudonimo());
        assertEquals("otros", guardada.getCategoria());
        assertEquals(guardada.getCreadaEn(), guardada.getCreadaEn().truncatedTo(ChronoUnit.HOURS),
            "HU-26: la hora se redondea a 1 h");
        assertTrue(Arrays.stream(Noticia.class.getDeclaredFields())
            .noneMatch(c -> PRIVADOS.contains(c.getName())));
    }

    @Test
    void lasImagenesSeGuardanEnDiscoConNombreGeneradoYSinElNombreOriginal() throws IOException {
        String id = api.crear("cuenta-1", "Condor Silente 482",
            solicitud("<p>Con foto</p>", List.of(new ImagenAdjunta("image/png", png()))));

        List<ImagenNoticia> guardadas = imagenesNoticia.findByNoticiaIdOrderByOrdenAsc(id);
        assertEquals(1, guardadas.size());
        String nombre = guardadas.get(0).getNombreGuardado();
        assertTrue(nombre.matches("[0-9a-f-]{36}\\.png"), "nombre generado: " + nombre);
        assertFalse(nombre.contains("foto-del-periodista"));

        Path archivo = Paths.get("target/imagenes-prueba").toAbsolutePath().normalize().resolve(nombre);
        assertTrue(Files.isRegularFile(archivo));
        assertNotNull(ImageIO.read(new ByteArrayInputStream(Files.readAllBytes(archivo))),
            "se guardo una imagen re-codificada valida, sin metadatos");

        ArchivoImagen leida = api.imagen(guardadas.get(0).getId()).orElseThrow();
        assertEquals("image/png", leida.tipoContenido());
        assertTrue(leida.contenido().length > 0);
        assertTrue(api.imagen("no-existe").isEmpty());
    }

    @Test
    void seRechazaUnArchivoQueNoEsUnaImagen() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () ->
            api.crear("c", "s", solicitud("<p>x</p>",
                List.of(new ImagenAdjunta("image/png", "esto no es una imagen".getBytes(StandardCharsets.UTF_8))))));
        assertTrue(error.getMessage().contains("imagen"));
    }

    @Test
    void seRechazaUnFormatoNoPermitidoYElExcesoDeImagenes() throws IOException {
        byte[] bytes = png();
        assertThrows(IllegalArgumentException.class, () ->
            api.crear("c", "s", solicitud("<p>x</p>", List.of(new ImagenAdjunta("image/gif", bytes)))));

        List<ImagenAdjunta> demasiadas = IntStream.range(0, 6)
            .mapToObj(i -> new ImagenAdjunta("image/png", bytes)).toList();
        assertThrows(IllegalArgumentException.class, () ->
            api.crear("c", "s", solicitud("<p>x</p>", demasiadas)));
    }

    @Test
    void obtenerDevuelveSoloLaVistaPublicaConLasImagenesEnOrden() throws IOException {
        String id = api.crear("cuenta-1", "Condor Silente 482",
            solicitud("<p>Nota</p>", List.of(new ImagenAdjunta("image/png", png()))));

        NoticiaPublica publica = api.obtener(id).orElseThrow();
        assertEquals("Encabezado de prueba", publica.titulo());
        assertEquals("Condor Silente 482", publica.autorSeudonimo());
        assertEquals(1, publica.imagenesIds().size());
        assertTrue(Arrays.stream(NoticiaPublica.class.getRecordComponents())
            .noneMatch(c -> PRIVADOS.contains(c.getName())));
        assertTrue(api.obtener("no-existe").isEmpty());
    }

    @Test
    void alCrearSePublicaNoticiaCreada() {
        String id = api.crear("cuenta-1", "Condor Silente 482", solicitud("<p>Nota</p>", List.of()));

        assertEquals(1, eventos.stream(NoticiaCreada.class)
            .filter(e -> e.noticiaId().equals(id)).count());
    }
}
