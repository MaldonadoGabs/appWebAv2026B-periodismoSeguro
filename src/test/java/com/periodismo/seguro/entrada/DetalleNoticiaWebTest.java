package com.periodismo.seguro.entrada;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.periodismo.seguro.noticias.ImagenAdjunta;
import com.periodismo.seguro.noticias.NoticiasApi;
import com.periodismo.seguro.noticias.SolicitudNoticia;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/** HU-12: detalle publico de la noticia con todos sus datos visibles y 404 amigable. */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "app.imagenes.directorio=target/imagenes-prueba-detalle",
    "spring.datasource.url=jdbc:h2:mem:detalleHu12;DB_CLOSE_DELAY=-1"
})
class DetalleNoticiaWebTest {
    @Autowired MockMvc mvc;
    @Autowired NoticiasApi noticias;

    private String crear(String titulo, String categoria) {
        return noticias.crear("cuenta-secreta-" + UUID.randomUUID(), "Condor Silente 482",
            new SolicitudNoticia(titulo, "Subtitulo de " + titulo,
                "<p>Texto <strong>importante</strong> de la nota</p>", categoria, List.of()));
    }

    private String crearConImagen(String titulo) throws IOException {
        BufferedImage imagen = new BufferedImage(4, 2, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        ImageIO.write(imagen, "png", salida);
        return noticias.crear("cuenta-secreta-" + UUID.randomUUID(), "Condor Silente 482",
            new SolicitudNoticia(titulo, "Subtitulo", "<p>Texto</p>", "cultura",
                List.of(new ImagenAdjunta("image/png", salida.toByteArray()))));
    }

    @Test
    void elDetalleEsPublicoYSinSesionSeVeCompleto() throws Exception {
        String id = crear("Nota de prueba detalle", "cultura");

        String html = mvc.perform(get("/noticias/" + id))
            .andExpect(status().isOk())
            .andExpect(view().name("detalle-noticia"))
            .andExpect(content().string(containsString("Nota de prueba detalle")))
            .andExpect(content().string(containsString("Subtitulo de Nota de prueba detalle")))
            .andExpect(content().string(containsString("<p>Texto <strong>importante</strong> de la nota</p>")))
            .andExpect(content().string(containsString("Condor Silente 482")))
            .andExpect(content().string(containsString("Cultura")))
            .andExpect(content().string(containsString("UTC")))
            .andReturn().getResponse().getContentAsString();
        assertFalse(html.matches("(?s).*(src|href)=\"https?://.*"), "recursos de otros dominios");
    }

    @Test
    void lasImagenesDelDetalleSeMuestranCompletasYNoComoMiniaturas() throws Exception {
        String id = crearConImagen("Nota con imagen completa");

        String html = mvc.perform(get("/noticias/" + id))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("class=\"imagenes-noticia\"")))
            .andExpect(content().string(not(containsString("class=\"miniaturas\""))))
            .andReturn().getResponse().getContentAsString();
        assertTrue(html.contains("/noticias/imagenes/"), "la imagen se sirve desde la nota");
    }

    @Test
    void elDetalleNuncaMuestraCorreoNombreRealNiIdDeCuenta() throws Exception {
        String id = crear("Nota sin datos privados", "politica");

        mvc.perform(get("/noticias/" + id))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Condor Silente 482")))
            .andExpect(content().string(not(containsString("cuenta-secreta-"))))
            .andExpect(content().string(not(containsString("@"))))
            .andExpect(content().string(not(containsString("Ana"))));
    }

    @Test
    void unaNoticiaInexistenteDevuelve404ConPaginaAmigable() throws Exception {
        mvc.perform(get("/noticias/no-existe-nunca"))
            .andExpect(status().isNotFound())
            .andExpect(content().string(containsString("404")))
            .andExpect(content().string(containsString("Volver al inicio")));
    }

    @Test
    void elFeedEnlazaCadaTarjetaConSuDetalle() throws Exception {
        String id = crear("Nota enlazada", "sociedad");

        String html = mvc.perform(get("/").param("tamano", "50"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        org.junit.jupiter.api.Assertions.assertTrue(
            html.contains("/noticias/" + id), "cada tarjeta enlaza con su detalle");
    }
}
