package com.periodismo.seguro.entrada;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.periodismo.seguro.cuentas.CuentasApi;
import com.periodismo.seguro.cuentas.SolicitudRegistro;
import com.periodismo.seguro.noticias.NoticiasApi;
import com.periodismo.seguro.noticias.NoticiaPublica;
import com.periodismo.seguro.noticias.SolicitudNoticia;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/** HU-13: interfaz de edicion, carga de datos actuales, validaciones y fecha de modificacion. */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "app.imagenes.directorio=target/imagenes-prueba-editar-web",
    "spring.datasource.url=jdbc:h2:mem:editarWebHu13;DB_CLOSE_DELAY=-1"
})
class EditarNoticiaWebTest {
    @Autowired MockMvc mvc;
    @Autowired CuentasApi cuentas;
    @Autowired NoticiasApi noticias;

    private record Sesion(MockHttpSession http, String cuentaId) {}

    private Sesion entrar() throws Exception {
        String correo = "e" + UUID.randomUUID() + "@ejemplo.com";
        cuentas.registrar(new SolicitudRegistro(correo, "Nombre Real Secreto", "Segura123"));
        String cuentaId = cuentas.credencialesPorCorreo(correo).orElseThrow().cuentaId();
        MockHttpSession http = (MockHttpSession) mvc.perform(post("/login").with(csrf())
                .param("correo", correo).param("clave", "Segura123"))
            .andReturn().getRequest().getSession();
        return new Sesion(http, cuentaId);
    }

    private String publicar(Sesion sesion, String titulo) {
        return noticias.crear(sesion.cuentaId(), "Condor Silente 482",
            new SolicitudNoticia(titulo, "Subtitulo original", "<p>Texto original</p>", "cultura", List.of()));
    }

    @Test
    void sinSesionLaEdicionRedirigeAlLogin() throws Exception {
        String id = publicar(entrar(), "Nota");
        mvc.perform(get("/noticias/" + id + "/editar"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("http://localhost/login"));
    }

    @Test
    void elFormularioCargaLosDatosActualesDeLaNoticia() throws Exception {
        Sesion sesion = entrar();
        String id = publicar(sesion, "Titulo original");

        String html = mvc.perform(get("/noticias/" + id + "/editar").session(sesion.http))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("id=\"editor\"")))
            .andExpect(content().string(containsString("Titulo original")))
            .andExpect(content().string(containsString("Subtitulo original")))
            .andExpect(content().string(containsString("<p>Texto original</p>")))
            .andExpect(content().string(containsString("id=\"cuerpo\"")))
            .andExpect(content().string(not(containsString("Nombre Real Secreto"))))
            .andReturn().getResponse().getContentAsString();
        assertFalse(html.matches("(?s).*(src|href)=\"https?://.*"), "recursos de otros dominios");
    }

    @Test
    void otroPeriodistaNoPuedeAbrirNiEditarLaNoticiaAjena() throws Exception {
        Sesion autor = entrar();
        Sesion otro = entrar();
        String id = publicar(autor, "Nota del autor");

        mvc.perform(get("/noticias/" + id + "/editar").session(otro.http))
            .andExpect(status().isNotFound());
        mvc.perform(post("/noticias/" + id + "/editar").session(otro.http).with(csrf())
                .param("titulo", "Hackeada").param("subtitulo", "Sub").param("cuerpo", "<p>Inyectado</p>")
                .param("categoria", "politica"))
            .andExpect(status().isNotFound());
        assertFalse(noticias.obtener(id).orElseThrow().titulo().equals("Hackeada"));
    }

    @Test
    void guardarCambiosActualizaLaNoticiaYRegistraLaFecha() throws Exception {
        Sesion sesion = entrar();
        String id = publicar(sesion, "Titulo original");

        mvc.perform(post("/noticias/" + id + "/editar").session(sesion.http).with(csrf())
                .param("titulo", "Titulo modificado")
                .param("subtitulo", "Subtitulo modificado")
                .param("cuerpo", "<p>Texto modificado</p>")
                .param("categoria", "economia"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/noticias/" + id + "/confirmacion"));

        NoticiaPublica actualizada = noticias.obtener(id).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals("Titulo modificado", actualizada.titulo());
        org.junit.jupiter.api.Assertions.assertEquals("Economía", actualizada.categoria());
        org.junit.jupiter.api.Assertions.assertNotNull(actualizada.modificadaEn());

        mvc.perform(get("/noticias/" + id))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Editada:")))
            .andExpect(content().string(containsString("Titulo modificado")));
    }

    @Test
    void elEnvioSinTokenCsrfSeRechaza() throws Exception {
        Sesion sesion = entrar();
        String id = publicar(sesion, "Nota");
        mvc.perform(post("/noticias/" + id + "/editar").session(sesion.http)
                .param("titulo", "Cambio").param("subtitulo", "Sub").param("cuerpo", "<p>Texto</p>")
                .param("categoria", "otros"))
            .andExpect(status().isForbidden());
    }

    @Test
    void lasValidacionesMuestranElErrorJuntoAlCampoYNoGuardan() throws Exception {
        Sesion sesion = entrar();
        String id = publicar(sesion, "Titulo original");

        mvc.perform(post("/noticias/" + id + "/editar").session(sesion.http).with(csrf())
                .param("titulo", "")
                .param("subtitulo", "Subtitulo")
                .param("cuerpo", "<p>Texto</p>")
                .param("categoria", "otros"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Escribe el encabezado")));

        mvc.perform(post("/noticias/" + id + "/editar").session(sesion.http).with(csrf())
                .param("titulo", "Titulo")
                .param("subtitulo", "Subtitulo")
                .param("cuerpo", "<p>Texto</p>")
                .param("categoria", "categoria-inventada"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Elige una categoría")));

        org.junit.jupiter.api.Assertions.assertEquals("Titulo original", noticias.obtener(id).orElseThrow().titulo());
        org.junit.jupiter.api.Assertions.assertNull(noticias.obtener(id).orElseThrow().modificadaEn());
    }

    @Test
    void elHtmlPeligrosoSeEliminaTambienAlEditar() throws Exception {
        Sesion sesion = entrar();
        String id = publicar(sesion, "Titulo");

        mvc.perform(post("/noticias/" + id + "/editar").session(sesion.http).with(csrf())
                .param("titulo", "Titulo").param("subtitulo", "Sub")
                .param("cuerpo", "<p>Hola</p><script>alert(1)</script>"
                    + "<img src=\"https://otrodominio.com/x.png\" onerror=\"alert(2)\">")
                .param("categoria", "otros"))
            .andExpect(status().is3xxRedirection());

        NoticiaPublica guardada = noticias.obtener(id).orElseThrow();
        assertFalse(guardada.cuerpo().contains("script"));
        assertFalse(guardada.cuerpo().contains("onerror"));
        assertFalse(guardada.cuerpo().contains("otrodominio"));
    }
}
