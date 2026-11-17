package com.periodismo.seguro.entrada;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.periodismo.seguro.noticias.NoticiasApi;
import com.periodismo.seguro.noticias.SolicitudNoticia;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/** HU-11: interfaz de busqueda y filtro del feed, con parametros preservados y estado vacio claro. */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "app.imagenes.directorio=target/imagenes-prueba-busqueda-web",
    "spring.datasource.url=jdbc:h2:mem:busquedaWebHu11;DB_CLOSE_DELAY=-1"
})
class BuscarNoticiasWebTest {
    @Autowired MockMvc mvc;
    @Autowired NoticiasApi noticias;

    private void crear(String titulo, String cuerpo, String categoria) {
        noticias.crear("autor-" + UUID.randomUUID(), "Condor Silente 482",
            new SolicitudNoticia(titulo, "Subtitulo de " + titulo, cuerpo, categoria, List.of()));
    }

    @Test
    void elFeedMuestraElFormularioDeBusquedaYElFiltroDeCategorias() throws Exception {
        String html = mvc.perform(get("/"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("id=\"feed\"")))
            .andExpect(content().string(containsString("name=\"q\"")))
            .andExpect(content().string(containsString("name=\"categoria\"")))
            .andExpect(content().string(containsString("Buscar")))
            .andReturn().getResponse().getContentAsString();
        assertFalse(html.matches("(?s).*(src|href)=\"https?://.*"), "recursos de otros dominios");
    }

    @Test
    void buscarPorTextoMuestraSoloLasCoincidencias() throws Exception {
        crear("Cambio climatico global", "<p>Texto</p>", "medio-ambiente");
        crear("Elecciones generales", "<p>Texto</p>", "politica");

        String html = mvc.perform(get("/").param("q", "climatico").param("tamano", "50"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Cambio climatico global")))
            .andExpect(content().string(not(containsString("Elecciones generales"))))
            .andReturn().getResponse().getContentAsString();
        assertFalse(html.matches("(?s).*(src|href)=\"https?://.*"));
    }

    @Test
    void filtrarPorCategoriaMuestraSoloEsaCategoria() throws Exception {
        crear("Nota de cultura", "<p>Texto</p>", "cultura");
        crear("Nota de economia", "<p>Texto</p>", "economia");

        mvc.perform(get("/").param("categoria", "cultura").param("tamano", "50"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Nota de cultura")))
            .andExpect(content().string(not(containsString("Nota de economia"))));
    }

    @Test
    void sinResultadosMuestraUnMensajeExplicito() throws Exception {
        crear("Nota existente", "<p>Texto</p>", "otros");

        mvc.perform(get("/").param("q", "esto-no-existe-nunca"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("No se encontraron noticias")))
            .andExpect(content().string(not(containsString("class=\"tarjeta\""))));
    }

    @Test
    void laPaginacionPreservaTextoYCategoria() throws Exception {
        for (int i = 0; i < 12; i++) {
            crear("Nota buscada " + i, "<p>palabra clave</p>", "sociedad");
        }

        String html = mvc.perform(get("/")
                .param("q", "buscada").param("categoria", "sociedad").param("tamano", "5"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Página 1 de")))
            .andReturn().getResponse().getContentAsString();

        // Los enlaces de paginacion conservan q y categoria.
        org.junit.jupiter.api.Assertions.assertTrue(
            html.contains("q=buscada") && html.contains("categoria=sociedad"),
            "los parametros de busqueda se preservan en la paginacion: " + html);

        mvc.perform(get("/").param("q", "buscada").param("categoria", "sociedad")
                .param("tamano", "5").param("pagina", "2"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Página 2 de")));
    }

    @Test
    void losResultadosDeBusquedaSiguenMostrandoSoloElSeudonimo() throws Exception {
        crear("Nota privacidad", "<p>Texto</p>", "otros");

        String html = mvc.perform(get("/").param("q", "privacidad"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Condor Silente 482")))
            .andExpect(content().string(not(containsString("autor-"))))
            .andExpect(content().string(not(containsString("@"))))
            .andReturn().getResponse().getContentAsString();
        assertFalse(html.matches("(?s).*(src|href)=\"https?://.*"));
    }
}
