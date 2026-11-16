package com.periodismo.seguro.noticias.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.periodismo.seguro.noticias.CategoriaNoticia;
import com.periodismo.seguro.noticias.NoticiaCreada;
import com.periodismo.seguro.noticias.NoticiasApi;
import com.periodismo.seguro.noticias.SolicitudNoticia;
import java.sql.Connection;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

/** HU-16: tabla propia, carga por script y una categoria por noticia. */
@SpringBootTest
@RecordApplicationEvents
@TestPropertySource(properties = {
    "app.imagenes.directorio=target/imagenes-prueba-categorias",
    "spring.datasource.url=jdbc:h2:mem:categoriashu16;DB_CLOSE_DELAY=-1"
})
class CategoriasTest {
    private static final List<String> SLUGS = List.of(
        "politica", "economia", "derechos-humanos", "sociedad", "seguridad-justicia",
        "medio-ambiente", "cultura", "internacional", "otros");

    @Autowired NoticiasApi api;
    @Autowired CategoriaRepository catalogo;
    @Autowired NoticiaRepository noticias;
    @Autowired DataSource dataSource;
    @Autowired JdbcTemplate jdbc;
    @Autowired ApplicationEvents eventos;

    private String crear(String categoria) {
        return api.crear("cuenta-16", "Condor Silente 482",
            new SolicitudNoticia("Titulo", "Subtitulo", "<p>Texto</p>", List.of(), categoria));
    }

    @Test
    void elScriptCargaLas9CategoriasConSlugNombreYOrden() {
        List<CategoriaNoticia> categorias = api.categorias();

        assertEquals(9, categorias.size());
        assertEquals(SLUGS, categorias.stream().map(CategoriaNoticia::slug).toList());
        assertEquals("Política", categorias.get(0).nombre());
        assertEquals("Derechos Humanos", categorias.get(2).nombre());
        assertEquals(9, catalogo.count());
    }

    @Test
    void elScriptEsIdempotenteAlEjecutarseOtraVez() throws Exception {
        try (Connection conexion = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(conexion, new ClassPathResource("data.sql"));
        }

        assertEquals(9, catalogo.count());
        assertEquals(SLUGS, api.categorias().stream().map(CategoriaNoticia::slug).toList());
    }

    @Test
    void cadaNoticiaGuardaSuCategoriaYLaExponePorSuNombre() {
        String id = crear("economia");

        assertEquals("economia", noticias.findById(id).orElseThrow().getCategoriaId());
        assertEquals("Economía", api.obtener(id).orElseThrow().categoriaNombre());

        var resumen = api.listar(0, 50).stream().filter(n -> n.id().equals(id)).findFirst().orElseThrow();
        assertEquals("economia", resumen.categoriaSlug());
        assertEquals("Economía", resumen.categoriaNombre());
    }

    @Test
    void sinCategoriaExplicitaLaNotaQuedaEnOtros() {
        String id = api.crear("cuenta-16", "Condor Silente 482",
            new SolicitudNoticia("Titulo", "Subtitulo", "<p>Texto</p>", List.of()));

        assertEquals(SolicitudNoticia.CATEGORIA_OTROS, noticias.findById(id).orElseThrow().getCategoriaId());
        assertEquals("Otros", api.obtener(id).orElseThrow().categoriaNombre());
    }

    @Test
    void seRechazaCrearUnaNoticiaConCategoriaDesconocida() {
        assertThrows(IllegalArgumentException.class, () -> crear("no-existe"));
        assertThrows(IllegalArgumentException.class, () -> crear("POLITICA"));
    }

    @Test
    void elEventoDeNoticiaCreadaLlevaLaCategoriaReal() {
        String id = crear("derechos-humanos");

        assertEquals(1, eventos.stream(NoticiaCreada.class)
            .filter(e -> e.noticiaId().equals(id) && "derechos-humanos".equals(e.categoria())).count());
    }

    @Test
    void elScriptAsignaOtrosALasNoticiasAntiguasSinCategoria() throws Exception {
        String id = crear("politica");
        jdbc.update("UPDATE noticias SET categoria = NULL WHERE id = ?", id);

        try (Connection conexion = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(conexion, new ClassPathResource("data.sql"));
        }

        assertEquals("otros", jdbc.queryForObject(
            "SELECT categoria FROM noticias WHERE id = ?", String.class, id));
    }
}
