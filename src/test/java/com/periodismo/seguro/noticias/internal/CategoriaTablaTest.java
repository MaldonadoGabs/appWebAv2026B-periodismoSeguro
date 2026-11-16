package com.periodismo.seguro.noticias.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.periodismo.seguro.noticias.CategoriaNoticia;
import com.periodismo.seguro.noticias.NoticiasApi;
import com.periodismo.seguro.noticias.SolicitudNoticia;
import java.util.List;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

/** HU-01: las categorias viven en su propia tabla, se cargan por script y toda nota tiene una. */
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestPropertySource(properties = {
    "app.imagenes.directorio=target/imagenes-prueba-tabla-categorias",
    "spring.datasource.url=jdbc:h2:mem:categoriastabla;DB_CLOSE_DELAY=-1"
})
class CategoriaTablaTest {
    @Autowired NoticiasApi api;
    @Autowired CategoriaNoticiaRepository categorias;
    @Autowired NoticiaRepository noticias;
    @Autowired JdbcTemplate jdbc;

    @Test
    @Order(1)
    void laTablaSeCargaConLasNueveCategoriasInicialesSinDuplicarFilas() {
        assertEquals(9L, jdbc.queryForObject("select count(*) from categorias_noticias", Long.class));
        assertEquals(9, categorias.count());

        List<CategoriaNoticia> catalogo = api.categorias();
        assertEquals(List.of("politica", "economia", "derechos-humanos", "sociedad",
            "seguridad-justicia", "medio-ambiente", "cultura", "internacional", "otros"),
            catalogo.stream().map(CategoriaNoticia::id).toList());
        assertEquals("Derechos Humanos", catalogo.get(2).nombre());
    }

    @Test
    @Order(2)
    void elCatalogoDeLaApiSaleDeLaTablaNoDeUnaListaFija() {
        categorias.save(new CategoriaNoticiaEntity("deportes", "Deportes", 10));
        assertTrue(api.categorias().stream().anyMatch(c -> c.id().equals("deportes")));

        String id = api.crear("autor-tabla", "Condor Silente 482", new SolicitudNoticia(
            "Nota de deportes", "Subtitulo", "<p>Texto</p>", "deportes", List.of()));
        assertEquals("Deportes", api.obtener(id).orElseThrow().categoria());
    }

    @Test
    @Order(3)
    void todaNoticiaSeGuardaConUnaCategoriaValida() {
        String id = api.crear("autor-tabla", "Condor Silente 482", new SolicitudNoticia(
            "Nota con categoria", "Subtitulo", "<p>Texto</p>", "politica", List.of()));
        assertNotNull(noticias.findById(id).orElseThrow().getCategoria());

        assertThrows(IllegalArgumentException.class, () -> api.crear("autor-tabla", "Condor Silente 482",
            new SolicitudNoticia("Sin categoria", "Subtitulo", "<p>Texto</p>", null, List.of())));
        assertThrows(IllegalArgumentException.class, () -> api.crear("autor-tabla", "Condor Silente 482",
            new SolicitudNoticia("Categoria inventada", "Subtitulo", "<p>Texto</p>", "inventada", List.of())));
    }
}
