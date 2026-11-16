package com.periodismo.seguro.entrada;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.periodismo.seguro.cuentas.CuentasApi;
import com.periodismo.seguro.cuentas.SolicitudRegistro;
import com.periodismo.seguro.noticias.ImagenAdjunta;
import com.periodismo.seguro.noticias.NoticiasApi;
import com.periodismo.seguro.noticias.SolicitudNoticia;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/** HU-09: interfaz de creacion de noticias, proteccion de la ruta, CSRF, imagenes y HTML saneado. */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "app.imagenes.directorio=target/imagenes-prueba")
class CrearNoticiaWebTest {
    @Autowired MockMvc mvc;
    @Autowired CuentasApi cuentas;
    @Autowired NoticiasApi noticias;

    private String correo;

    private static byte[] png() throws IOException {
        BufferedImage imagen = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        ImageIO.write(imagen, "png", salida);
        return salida.toByteArray();
    }

    private static MockMultipartFile imagen(String nombre, byte[] contenido) {
        return new MockMultipartFile("imagenes", nombre, "image/png", contenido);
    }

    private MockHttpSession entrar() throws Exception {
        correo = "n" + UUID.randomUUID() + "@ejemplo.com";
        cuentas.registrar(new SolicitudRegistro(correo, "Ana Periodista", "Segura123"));
        return (MockHttpSession) mvc.perform(post("/login").with(csrf())
            .param("correo", correo).param("clave", "Segura123"))
            .andReturn().getRequest().getSession();
    }

    private String seudonimo() {
        String cuentaId = cuentas.credencialesPorCorreo(correo).orElseThrow().cuentaId();
        return cuentas.seudonimoDe(cuentaId).orElseThrow();
    }

    @Test
    void sinSesionElFormularioRedirigeAlLogin() throws Exception {
        mvc.perform(get("/noticias/nueva"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("http://localhost/login"));
    }

    @Test
    void elFormularioMuestraElEditorYNoCargaRecursosExternos() throws Exception {
        String html = mvc.perform(get("/noticias/nueva").session(entrar()))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("id=\"editor\"")))
            .andExpect(content().string(containsString("contenteditable=\"true\"")))
            .andExpect(content().string(containsString("name=\"titulo\"")))
            .andExpect(content().string(containsString("name=\"subtitulo\"")))
            .andExpect(content().string(containsString("name=\"cuerpo\"")))
            .andExpect(content().string(containsString("name=\"imagenes\"")))
            .andExpect(content().string(containsString("/js/editor-noticia.js")))
            .andExpect(content().string(containsString("/css/estilo.css")))
            .andReturn().getResponse().getContentAsString();

        assertFalse(html.matches("(?s).*(src|href)=\"https?://.*"), "recursos de otros dominios");
    }

    @Test
    void elEnvioSinTokenCsrfSeRechaza() throws Exception {
        mvc.perform(multipart("/noticias/nueva").session(entrar())
                .param("titulo", "Titulo").param("subtitulo", "Subtitulo").param("cuerpo", "<p>Texto</p>"))
            .andExpect(status().isForbidden());
    }

    @Test
    void crearNoticiaGuardaYMuestraEncabezadoSubtituloTextoEImagenes() throws Exception {
        MockHttpSession sesion = entrar();
        MvcResult resultado = mvc.perform(multipart("/noticias/nueva")
                .file(imagen("foto.png", png()))
                .param("titulo", "Encabezado real")
                .param("subtitulo", "Subtitulo real")
                .param("cuerpo", "<p>Texto del cuerpo</p>")
                .param("categoria", "otros")
                .session(sesion).with(csrf()))
            .andExpect(status().is3xxRedirection())
            .andReturn();

        String destino = resultado.getResponse().getRedirectedUrl();
        assertNotNull(destino);
        assertTrue(destino.startsWith("/noticias/"));

        String html = mvc.perform(get(destino).session(sesion))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Encabezado real")))
            .andExpect(content().string(containsString("Subtitulo real")))
            .andExpect(content().string(containsString("<p>Texto del cuerpo</p>")))
            .andExpect(content().string(containsString("/noticias/imagenes/")))
            .andExpect(content().string(containsString(seudonimo())))
            .andExpect(content().string(not(containsString(correo))))
            .andExpect(content().string(not(containsString("Ana Periodista"))))
            .andReturn().getResponse().getContentAsString();

        assertFalse(html.matches("(?s).*(src|href)=\"https?://.*"), "recursos de otros dominios");
    }

    @Test
    void seAceptanVariasImagenesEnUnaSolaNota() throws Exception {
        byte[] bytes = png();
        MvcResult resultado = mvc.perform(multipart("/noticias/nueva")
                .file(imagen("una.png", bytes)).file(imagen("otra.png", bytes))
                .param("titulo", "Titulo").param("subtitulo", "Subtitulo").param("cuerpo", "<p>Texto</p>")
                .param("categoria", "otros")
                .session(entrar()).with(csrf()))
            .andExpect(status().is3xxRedirection())
            .andReturn();

        String id = resultado.getResponse().getRedirectedUrl().replace("/noticias/", "");
        assertTrue(noticias.obtener(id).orElseThrow().imagenesIds().size() == 2);
    }

    @Test
    void elHtmlPeligrosoSeEliminaAntesDeGuardarse() throws Exception {
        String peligroso = "<p>Hola</p><script>alert(1)</script>"
            + "<img src=\"https://otrodominio.com/x.png\" onerror=\"alert(2)\">"
            + "<a href=\"javascript:alert(3)\">clic</a>";

        MockHttpSession sesion = entrar();
        MvcResult resultado = mvc.perform(multipart("/noticias/nueva")
                .param("titulo", "Titulo").param("subtitulo", "Subtitulo").param("cuerpo", peligroso)
                .param("categoria", "otros")
                .session(sesion).with(csrf()))
            .andExpect(status().is3xxRedirection())
            .andReturn();
        String destino = resultado.getResponse().getRedirectedUrl();

        String html = mvc.perform(get(destino).session(sesion))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        assertTrue(html.contains("<p>Hola</p>"));
        assertFalse(html.contains("alert(1)"));
        assertFalse(html.contains("onerror"));
        assertFalse(html.contains("otrodominio"));
        assertFalse(html.contains("javascript:"));

        String id = destino.replace("/noticias/", "");
        assertFalse(noticias.obtener(id).orElseThrow().cuerpo().contains("script"));
    }

    @Test
    void sinEncabezadoVuelveAlFormularioConElErrorJuntoAlCampo() throws Exception {
        mvc.perform(multipart("/noticias/nueva")
                .param("titulo", "").param("subtitulo", "Subtitulo").param("cuerpo", "<p>Texto</p>")
                .session(entrar()).with(csrf()))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Escribe el encabezado")));
    }

    @Test
    void unaImagenInvalidaMuestraErrorYNoGuardaLaNoticia() throws Exception {
        mvc.perform(multipart("/noticias/nueva")
                .file(new MockMultipartFile("imagenes", "falsa.png", "image/png",
                    "esto no es una imagen".getBytes(StandardCharsets.UTF_8)))
                .param("titulo", "Titulo").param("subtitulo", "Subtitulo").param("cuerpo", "<p>Texto</p>")
                .param("categoria", "otros")
                .session(entrar()).with(csrf()))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("imagen valida")));
    }

    @Test
    void laImagenSeSirveConSuTipoSinCacheYUnaNoticiaInexistenteDevuelve404() throws Exception {
        String id = noticias.crear("cuenta-web", "Condor Silente 482",
            new SolicitudNoticia("Titulo", "Subtitulo", "<p>Texto</p>",
                List.of(new ImagenAdjunta("image/png", png()))));
        String imagenId = noticias.obtener(id).orElseThrow().imagenesIds().get(0);
        MockHttpSession sesion = entrar();

        mvc.perform(get("/noticias/imagenes/" + imagenId).session(sesion))
            .andExpect(status().isOk())
            .andExpect(content().contentType("image/png"))
            .andExpect(header().string("Cache-Control", "no-store"))
            .andExpect(header().string("X-Content-Type-Options", "nosniff"));

        mvc.perform(get("/noticias/no-existe").session(sesion)).andExpect(status().isNotFound());
    }
}
