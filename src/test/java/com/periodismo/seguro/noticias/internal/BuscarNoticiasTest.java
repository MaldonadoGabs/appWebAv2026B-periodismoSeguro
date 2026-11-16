package com.periodismo.seguro.noticias.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.periodismo.seguro.noticias.NoticiaPublica;
import com.periodismo.seguro.noticias.NoticiasApi;
import com.periodismo.seguro.noticias.ResumenNoticia;
import com.periodismo.seguro.noticias.SolicitudNoticia;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/** HU-11: busqueda por titulo/palabra clave y filtro por categoria, integrada con paginacion. */
@SpringBootTest
@TestPropertySource(properties = {
    "app.imagenes.directorio=target/imagenes-prueba-busqueda",
    "spring.datasource.url=jdbc:h2:mem:busquedaHu11;DB_CLOSE_DELAY=-1"
})
class BuscarNoticiasTest {
    @Autowired NoticiasApi api;

    private String crear(String titulo, String cuerpo, String categoria) {
        return api.crear("autor-" + UUID.randomUUID(), "Condor Silente 482",
            new SolicitudNoticia(titulo, "Subtitulo", cuerpo, categoria, List.of()));
    }

    @Test
    void buscaPorTituloIgnorandoMayusculas() {
        String autor = "autor-" + UUID.randomUUID();
        api.crear(autor, "Seudo", new SolicitudNoticia("Cambio climatico en el norte",
            "Sub", "<p>Texto</p>", "medio-ambiente", List.of()));
        api.crear(autor, "Seudo", new SolicitudNoticia("Elecciones municipales",
            "Sub", "<p>Texto</p>", "politica", List.of()));

        List<ResumenNoticia> resultados = api.buscar("CLIMATICO", null, 0, 10);
        assertEquals(1, resultados.size());
        assertTrue(resultados.get(0).titulo().contains("climatico"));
        assertEquals(1, api.contarBusqueda("climatico", null));
    }

    @Test
    void buscaPorPalabraClaveDelCuerpo() {
        String autor = "autor-" + UUID.randomUUID();
        api.crear(autor, "Seudo", new SolicitudNoticia("Una nota", "Sub",
            "<p>La <strong>cosecha</strong> fue abundante este ano</p>", "economia", List.of()));
        api.crear(autor, "Seudo", new SolicitudNoticia("Otra nota", "Sub",
            "<p>Tema de politica internacional</p>", "politica", List.of()));

        List<ResumenNoticia> resultados = api.buscar("cosecha", null, 0, 10);
        assertEquals(1, resultados.size());
        assertTrue(resultados.get(0).titulo().equals("Una nota"));
    }

    @Test
    void filtraPorCategoria() {
        String autor = "autor-" + UUID.randomUUID();
        String marca = "marca-" + UUID.randomUUID();
        api.crear(autor, "Seudo", new SolicitudNoticia(marca + " cultura uno", "Sub", "<p>Texto</p>", "cultura", List.of()));
        api.crear(autor, "Seudo", new SolicitudNoticia(marca + " cultura dos", "Sub", "<p>Texto</p>", "cultura", List.of()));
        api.crear(autor, "Seudo", new SolicitudNoticia(marca + " economia", "Sub", "<p>Texto</p>", "economia", List.of()));

        // Las pruebas comparten la BD del contexto: se account por texto unico para aislar los datos.
        assertEquals(2, api.buscar(marca, "cultura", 0, 10).size());
        assertEquals(2, api.contarBusqueda(marca, "cultura"));
        assertEquals(1, api.buscar(marca, "economia", 0, 10).size());
    }

    @Test
    void combinaTextoYCategoria() {
        String autor = "autor-" + UUID.randomUUID();
        api.crear(autor, "Seudo", new SolicitudNoticia("Cultura y musica", "Sub", "<p>Texto</p>", "cultura", List.of()));
        api.crear(autor, "Seudo", new SolicitudNoticia("Musica en la calle", "Sub", "<p>Texto</p>", "sociedad", List.of()));

        List<ResumenNoticia> resultados = api.buscar("musica", "cultura", 0, 10);
        assertEquals(1, resultados.size());
        assertEquals("Cultura y musica", resultados.get(0).titulo());
    }

    @Test
    void laBusquedaEstaPaginadaYElTotalEsIndependienteDeLaPagina() {
        String autor = "autor-" + UUID.randomUUID();
        for (int i = 0; i < 15; i++) {
            api.crear(autor, "Seudo", new SolicitudNoticia("Nota repetida " + i, "Sub",
                "<p>Palabra clave unica</p>", "sociedad", List.of()));
        }

        assertEquals(15, api.contarBusqueda("clave unica", null));
        assertEquals(10, api.buscar("clave unica", null, 0, 10).size());
        assertEquals(5, api.buscar("clave unica", null, 1, 10).size());
        assertTrue(api.buscar("clave unica", null, 2, 10).isEmpty());
        // Normalizacion como el feed: pagina negativa y tamano por encima del tope.
        assertEquals(15, api.buscar("clave unica", null, -3, 999).size());
    }

    @Test
    void unaCategoriaInventadaNoFiltraNadaYTampocoUnTextoVacio() {
        api.crear("autor-" + UUID.randomUUID(), "Seudo", new SolicitudNoticia("Nota", "Sub",
            "<p>Texto</p>", "otros", List.of()));

        assertFalse(api.buscar(null, "inventada", 0, 10).isEmpty(),
            "una categoria inventada se ignora y no deja el feed vacio");
        assertFalse(api.buscar("  ", "", 0, 10).isEmpty(), "texto en blanco no filtra");
    }

    @Test
    void losResultadosSonResumenesSinDatosPrivados() {
        crear("Nota publica", "<p>Texto</p>", "otros");
        ResumenNoticia r = api.buscar("publica", null, 0, 10).get(0);
        assertNotNull(r.id());
        assertEquals("Condor Silente 482", r.autorSeudonimo());
        assertFalse(r.toString().contains("autor-"));
        assertFalse(r.toString().contains("@"));
    }
}
