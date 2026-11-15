package com.periodismo.seguro.entrada;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.periodismo.seguro.noticias.ImagenAdjunta;
import com.periodismo.seguro.noticias.NoticiasApi;
import com.periodismo.seguro.noticias.SolicitudNoticia;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/** HU-10: feed publico en la pagina principal, paginacion, privacidad y recursos propios. */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "app.imagenes.directorio=target/imagenes-prueba-feed",
    // H2 propia: por defecto todas las pruebas comparten mem:periodismo y el estado vacio no es fiable.
    "spring.datasource.url=jdbc:h2:mem:feedhu10;DB_CLOSE_DELAY=-1"
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FeedNoticiasWebTest {
    private static final Path DIRECTORIO =
        Paths.get("target/imagenes-prueba-feed").toAbsolutePath().normalize();

    @Autowired MockMvc mvc;
    @Autowired NoticiasApi noticias;

    private static byte[] png() throws IOException {
        BufferedImage imagen = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        ImageIO.write(imagen, "png", salida);
        return salida.toByteArray();
    }

    private String crear(String titulo, List<ImagenAdjunta> imagenes) {
        return noticias.crear("cuenta-" + UUID.randomUUID(), "Condor Silente 482",
            new SolicitudNoticia(titulo, "Subtitulo de " + titulo,
                "<p>Texto <strong>importante</strong> de " + titulo + "</p>", imagenes));
    }

    private static void borrarImagenesDelDisco() throws IOException {
        if (!Files.isDirectory(DIRECTORIO)) return;
        try (Stream<Path> archivos = Files.list(DIRECTORIO)) {
            for (Path archivo : archivos.filter(Files::isRegularFile).toList()) {
                Files.deleteIfExists(archivo);
            }
        }
    }

    private static void sinRecursosExternos(String html) {
        assertFalse(html.matches("(?s).*(src|href)=\"https?://.*"), "recursos de otros dominios");
    }

    @Test
    @Order(1)
    void elInicioEsPublicoYMuestraElEstadoVacio() throws Exception {
        String html = mvc.perform(get("/"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("id=\"feed\"")))
            .andExpect(content().string(containsString("Todavía no hay noticias publicadas")))
            .andReturn().getResponse().getContentAsString();
        sinRecursosExternos(html);
    }

    @Test
    @Order(2)
    void elFeedMuestraEncabezadoExtractoYSeudonimoSinDatosReales() throws Exception {
        crear("Primera nota del feed", List.of(new ImagenAdjunta("image/png", png())));
        crear("Segunda nota del feed", List.of());

        String html = mvc.perform(get("/"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Primera nota del feed")))
            .andExpect(content().string(containsString("Segunda nota del feed")))
            .andExpect(content().string(containsString("Subtitulo de Primera nota del feed")))
            .andExpect(content().string(containsString("Texto importante de Primera nota del feed")))
            .andExpect(content().string(containsString("Condor Silente 482")))
            .andExpect(content().string(containsString("/noticias/imagenes/")))
            .andExpect(content().string(not(containsString("cuenta-"))))
            .andExpect(content().string(not(containsString("@ejemplo.com"))))
            .andExpect(content().string(not(containsString("Texto <strong>importante"))))
            .andReturn().getResponse().getContentAsString();
        sinRecursosExternos(html);
    }

    @Test
    @Order(3)
    void laImagenDelFeedSeSirveSinSesion() throws Exception {
        String id = crear("Nota con imagen publica", List.of(new ImagenAdjunta("image/png", png())));
        String imagenId = noticias.obtener(id).orElseThrow().imagenesIds().get(0);

        mvc.perform(get("/noticias/imagenes/" + imagenId))
            .andExpect(status().isOk())
            .andExpect(content().contentType("image/png"));
    }

    @Test
    @Order(4)
    void laPaginacionFuncionaYNormalizaParametrosInvalidos() throws Exception {
        for (int i = 0; i < 12; i++) {
            crear("Nota paginada " + i, List.of());
        }

        mvc.perform(get("/").param("pagina", "2").param("tamano", "5"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Página 2 de")));

        for (String[] parametros : new String[][] {
            {"pagina", "abc"}, {"pagina", "-3"}, {"pagina", "99999"},
            {"tamano", "0"}, {"tamano", "999"}, {"tamano", "no-es-numero"}}) {
            mvc.perform(get("/").param(parametros[0], parametros[1]))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"feed\"")));
        }
    }

    @Test
    @Order(5)
    void unaImagenPerdidaNoRompeElFeed() throws Exception {
        borrarImagenesDelDisco();

        String html = mvc.perform(get("/"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Primera nota del feed")))
            .andExpect(content().string(not(containsString("/noticias/imagenes/"))))
            .andReturn().getResponse().getContentAsString();
        sinRecursosExternos(html);
    }
}
