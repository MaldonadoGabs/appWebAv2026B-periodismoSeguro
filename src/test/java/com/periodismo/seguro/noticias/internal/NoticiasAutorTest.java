package com.periodismo.seguro.noticias.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.periodismo.seguro.noticias.CategoriaNoticia;
import com.periodismo.seguro.noticias.NoticiasApi;
import com.periodismo.seguro.noticias.ResumenNoticia;
import com.periodismo.seguro.noticias.SolicitudNoticia;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/** HU-01 y HU-08: categorias validadas en el servicio, "mis notas" filtrado por autor y propiedad de la noticia. */
@SpringBootTest
@TestPropertySource(properties = {
    "app.imagenes.directorio=target/imagenes-prueba-autor",
    "spring.datasource.url=jdbc:h2:mem:autorHu01;DB_CLOSE_DELAY=-1"
})
class NoticiasAutorTest {
    @Autowired NoticiasApi api;

    private static SolicitudNoticia solicitud(String titulo, String categoria) {
        return new SolicitudNoticia(titulo, "Subtitulo", "<p>Texto</p>", categoria, List.of());
    }

    @Test
    void elCatalogoTieneNueveCategoriasConIdsUnicos() {
        List<CategoriaNoticia> categorias = api.categorias();
        assertEquals(9, categorias.size());
        assertEquals(9, categorias.stream().map(CategoriaNoticia::id).distinct().count());
        assertTrue(categorias.stream().anyMatch(c -> c.id().equals("derechos-humanos")
            && c.nombre().equals("Derechos Humanos")));
    }

    @Test
    void crearGuardaLaCategoriaYLaDevuelveConSuNombre() {
        String autor = "autor-" + UUID.randomUUID();
        String id = api.crear(autor, "Condor Silente 482", solicitud("Con categoria", "cultura"));

        assertEquals("Cultura", api.obtener(id).orElseThrow().categoria());
        ResumenNoticia resumen = api.listarDeAutor(autor, 0, 10).get(0);
        assertEquals("Cultura", resumen.categoria());
    }

    @Test
    void unaCategoriaQueNoExisteSeRechaza() {
        assertThrows(IllegalArgumentException.class, () ->
            api.crear("autor-" + UUID.randomUUID(), "Condor Silente 482", solicitud("Inventada", "inventada")));
    }

    @Test
    void unaNoticiaSinCategoriaDeLasLlamadasAnterioresSigueSiendoValida() {
        String id = api.crear("autor-" + UUID.randomUUID(), "Condor Silente 482",
            new SolicitudNoticia("Sin categoria", "Subtitulo", "<p>Texto</p>", List.of()));
        assertNull(api.obtener(id).orElseThrow().categoria());
    }

    @Test
    void misNotasSoloIncluyeLasNoticiasDelAutorYRespetaLaPaginacion() {
        String ana = "ana-" + UUID.randomUUID();
        String beto = "beto-" + UUID.randomUUID();
        api.crear(ana, "Seudo Ana", solicitud("Primera de Ana", "politica"));
        api.crear(ana, "Seudo Ana", solicitud("Segunda de Ana", "politica"));
        api.crear(beto, "Seudo Beto", solicitud("Nota de Beto", "politica"));

        assertEquals(2, api.contarDeAutor(ana));
        assertEquals(2, api.listarDeAutor(ana, 0, 50).size());
        assertTrue(api.listarDeAutor(ana, 0, 50).stream().noneMatch(n -> n.titulo().contains("Beto")));

        assertEquals(1, api.listarDeAutor(ana, 0, 1).size());
        assertEquals(1, api.listarDeAutor(ana, 1, 1).size());
        assertTrue(api.listarDeAutor(ana, 5, 1).isEmpty());
    }

    @Test
    void sinAutorValidoNoSeDevuelveNada() {
        assertEquals(0, api.contarDeAutor(null));
        assertTrue(api.listarDeAutor(null, 0, 10).isEmpty());
        assertTrue(api.listarDeAutor("cuenta-que-no-existe-" + UUID.randomUUID(), 0, 10).isEmpty());
    }

    @Test
    void esAutorSoloEsVerdaderoParaLaCuentaQueCreoLaNoticia() {
        String autor = "autor-" + UUID.randomUUID();
        String id = api.crear(autor, "Seudo", solicitud("Propia", "otros"));

        assertTrue(api.esAutor(autor, id));
        assertFalse(api.esAutor("otra-cuenta", id));
        assertFalse(api.esAutor(null, id));
        assertFalse(api.esAutor(autor, "no-existe"));
    }
}
