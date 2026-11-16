package com.periodismo.seguro.noticias.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.periodismo.seguro.noticias.NoticiaPublica;
import com.periodismo.seguro.noticias.NoticiasApi;
import com.periodismo.seguro.noticias.SolicitudNoticia;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/** HU-13: solo el autor edita su noticia, con validaciones y fecha de modificacion redondeada. */
@SpringBootTest
@TestPropertySource(properties = {
    "app.imagenes.directorio=target/imagenes-prueba-editar",
    "spring.datasource.url=jdbc:h2:mem:editarHu13;DB_CLOSE_DELAY=-1"
})
class EditarNoticiaTest {
    @Autowired NoticiasApi api;
    @Autowired NoticiaRepository noticias;

    private static SolicitudNoticia solicitud(String titulo, String cuerpo, String categoria) {
        return new SolicitudNoticia(titulo, "Subtitulo", cuerpo, categoria, List.of());
    }

    @Test
    void elAutorActualizaTodosLosCamposYRegistraLaFechaDeModificacion() {
        String autor = "autor-" + UUID.randomUUID();
        String id = api.crear(autor, "Condor Silente 482", solicitud("Titulo original", "<p>Original</p>", "politica"));

        NoticiaPublica antes = api.obtener(id).orElseThrow();
        assertNull(antes.modificadaEn());

        assertTrue(api.editar(autor, id, solicitud("Titulo editado", "<p>Editado</p>", "economia")));

        NoticiaPublica despues = api.obtener(id).orElseThrow();
        assertEquals("Titulo editado", despues.titulo());
        assertEquals("<p>Editado</p>", despues.cuerpo());
        assertEquals("Economía", despues.categoria());
        assertEquals("Condor Silente 482", despues.autorSeudonimo(), "el seudonimo no cambia");
        assertNotNull(despues.modificadaEn(), "se registra la fecha de edicion");
        assertTrue(despues.modificadaEn().matches("\\d{2}/\\d{2}/\\d{4} · \\d{2}:00 UTC"),
            "hora de edicion redondeada a 1 h: " + despues.modificadaEn());
        // La fecha de publicacion original se conserva.
        assertEquals(antes.publicadaEn(), despues.publicadaEn());
    }

    @Test
    void otroAutorNoPuedeEditarUnaNoticiaAjena() {
        String autor = "autor-" + UUID.randomUUID();
        String id = api.crear(autor, "Seudo", solicitud("Titulo", "<p>Texto</p>", "otros"));

        assertFalse(api.editar("otro-autor", id, solicitud("Hackeado", "<p>Inyectado</p>", "politica")));
        assertEquals("Titulo", api.obtener(id).orElseThrow().titulo());
        assertNull(api.obtener(id).orElseThrow().modificadaEn());
    }

    @Test
    void unaNoticiaInexistenteOUnAutorNuloNoSeEdita() {
        assertFalse(api.editar("autor", "no-existe", solicitud("Titulo", "<p>Texto</p>", "otros")));
        assertFalse(api.editar(null, "no-existe", solicitud("Titulo", "<p>Texto</p>", "otros")));
        assertFalse(api.editar("autor", "no-existe", null));
    }

    @Test
    void lasValidacionesSeAplicanYNoModificanLaNoticiaSiFaltanDatos() {
        String autor = "autor-" + UUID.randomUUID();
        String id = api.crear(autor, "Seudo", solicitud("Titulo original", "<p>Texto</p>", "otros"));

        assertThrows(IllegalArgumentException.class, () ->
            api.editar(autor, id, solicitud("", "<p>Texto</p>", "otros")));
        assertThrows(IllegalArgumentException.class, () ->
            api.editar(autor, id, solicitud("Titulo", "<p>Texto</p>", "categoria-inventada")));
        assertThrows(IllegalArgumentException.class, () ->
            api.editar(autor, id, solicitud("Titulo", "", "otros")));

        assertEquals("Titulo original", api.obtener(id).orElseThrow().titulo());
        assertNull(api.obtener(id).orElseThrow().modificadaEn());
    }

    @Test
    void laEdicionSeReflejaEnElResumenDelFeed() {
        String autor = "autor-" + UUID.randomUUID();
        String id = api.crear(autor, "Seudo", solicitud("Viejo titulo", "<p>Viejo</p>", "otros"));
        api.editar(autor, id, solicitud("Nuevo titulo", "<p>Nuevo contenido</p>", "cultura"));

        var resumen = api.listarDeAutor(autor, 0, 10).get(0);
        assertEquals("Nuevo titulo", resumen.titulo());
        assertEquals("Nuevo contenido", resumen.extracto());
        assertEquals("Cultura", resumen.categoria());
    }
}
