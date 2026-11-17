package com.periodismo.seguro.entrada;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.periodismo.seguro.cuentas.CuentasApi;
import com.periodismo.seguro.cuentas.Rol;
import com.periodismo.seguro.cuentas.SolicitudRegistro;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/** Reglas de acceso: el periodista publica, el lector reacciona/comenta y usa el chat. */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "app.imagenes.directorio=target/imagenes-prueba-roles",
    "spring.datasource.url=jdbc:h2:mem:rolesHu08;DB_CLOSE_DELAY=-1"
})
class RolesWebTest {
    private static final List<String> RUTAS_PERIODISTA = List.of("/inicio", "/mis-notas", "/noticias/nueva");

    @Autowired MockMvc mvc;
    @Autowired CuentasApi cuentas;

    private MockHttpSession entrar(Rol rol) throws Exception {
        String correo = "r" + UUID.randomUUID() + "@ejemplo.com";
        cuentas.registrar(new SolicitudRegistro(correo, "Ana", "Segura123", rol));
        return (MockHttpSession) mvc.perform(post("/login").with(csrf())
                .param("correo", correo).param("clave", "Segura123"))
            .andReturn().getRequest().getSession();
    }

    @Test
    void elPeriodistaAutenticadoAbrePanelMisNotasYEscrituraPeroNoElChat() throws Exception {
        MockHttpSession sesion = entrar(Rol.PERIODISTA);
        for (String ruta : RUTAS_PERIODISTA) {
            mvc.perform(get(ruta).session(sesion)).andExpect(status().isOk());
        }
        mvc.perform(get("/chat").session(sesion)).andExpect(status().isForbidden());
    }

    @Test
    void elPanelDelPeriodistaNoMuestraElChat() throws Exception {
        mvc.perform(get("/inicio").session(entrar(Rol.PERIODISTA)))
            .andExpect(content().string(containsString("href=\"/mis-notas\"")))
            .andExpect(content().string(containsString("href=\"/noticias/nueva\"")))
            .andExpect(content().string(not(containsString("href=\"/chat\""))))
            .andExpect(content().string(containsString("action=\"/logout\"")));
    }

    @Test
    void sinSesionNiElPanelNiLasNoticiasNiElChatSeAbren() throws Exception {
        for (String ruta : RUTAS_PERIODISTA) {
            mvc.perform(get(ruta)).andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/login"));
        }
        mvc.perform(get("/chat")).andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("http://localhost/login"));
    }

    @Test
    void sinSesionNoSePuedeCrearUnaNoticia() throws Exception {
        mvc.perform(multipart("/noticias/nueva").with(csrf())
                .param("titulo", "Titulo").param("subtitulo", "Subtitulo").param("cuerpo", "<p>Texto</p>")
                .param("categoria", "otros"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("http://localhost/login"));
    }

    @Test
    @WithMockUser(username = "cuenta-lector", roles = "LECTOR")
    void elLectorNoPuedeCrearNoticiasNiAbrirElPanel() throws Exception {
        mvc.perform(get("/noticias/nueva")).andExpect(status().isForbidden());
        mvc.perform(multipart("/noticias/nueva").with(csrf())
                .param("titulo", "Titulo").param("subtitulo", "Subtitulo").param("cuerpo", "<p>Texto</p>")
                .param("categoria", "otros"))
            .andExpect(status().isForbidden());
        for (String ruta : RUTAS_PERIODISTA) {
            mvc.perform(get(ruta)).andExpect(status().isForbidden());
        }
    }

    @Test
    @WithMockUser(username = "cuenta-lector", roles = "LECTOR")
    void elLectorVeElFeedSinEnlacesDePeriodistaYPuedeAbrirElChat() throws Exception {
        mvc.perform(get("/"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("id=\"feed\"")))
            .andExpect(content().string(not(containsString("Escribir noticia"))))
            .andExpect(content().string(not(containsString("Mis notas"))))
            .andExpect(content().string(containsString("href=\"/chat\"")));
        mvc.perform(get("/chat")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "cuenta-admin", roles = "ADMINISTRADOR")
    void elAdministradorNoHeredaElAccesoDePeriodistaNiDeLector() throws Exception {
        for (String ruta : RUTAS_PERIODISTA) {
            mvc.perform(get(ruta)).andExpect(status().isForbidden());
        }
        mvc.perform(get("/chat")).andExpect(status().isForbidden());
    }

    @Test
    void elRegistroPuedeCrearLectorYPeriodistaPeroNuncaAdministrador() throws Exception {
        String lector = "lector" + UUID.randomUUID() + "@ejemplo.com";
        mvc.perform(post("/registro").with(csrf())
                .param("correo", lector).param("nombreReal", "Ana").param("clave", "Segura123")
                .param("confirmacion", "Segura123").param("rol", "LECTOR"))
            .andExpect(status().is3xxRedirection());
        assertEquals(Rol.LECTOR, cuentas.credencialesPorCorreo(lector).orElseThrow().rol());

        String admin = "admin" + UUID.randomUUID() + "@ejemplo.com";
        mvc.perform(post("/registro").with(csrf())
                .param("correo", admin).param("nombreReal", "Ana").param("clave", "Segura123")
                .param("confirmacion", "Segura123").param("rol", "ADMINISTRADOR"))
            .andExpect(status().isOk())
            .andExpect(model().attributeHasFieldErrors("form", "rol"));
    }

    @Test
    void elLectorEntraAlFeedYElPeriodistaAlPanelTrasIniciarSesion() throws Exception {
        String correoLector = "l" + UUID.randomUUID() + "@ejemplo.com";
        cuentas.registrar(new SolicitudRegistro(correoLector, "Ana", "Segura123", Rol.LECTOR));
        mvc.perform(post("/login").with(csrf())
                .param("correo", correoLector).param("clave", "Segura123"))
            .andExpect(redirectedUrl("/"));

        String correoPeriodista = "p" + UUID.randomUUID() + "@ejemplo.com";
        cuentas.registrar(new SolicitudRegistro(correoPeriodista, "Beto", "Segura123", Rol.PERIODISTA));
        mvc.perform(post("/login").with(csrf())
                .param("correo", correoPeriodista).param("clave", "Segura123"))
            .andExpect(redirectedUrl("/inicio"));
    }

    @Test
    void elChatMuestraElEstadoDelWebSocketYElFormularioParaLectores() throws Exception {
        mvc.perform(get("/chat").session(entrar(Rol.LECTOR)))
            .andExpect(content().string(containsString("id=\"log\"")))
            .andExpect(content().string(containsString("id=\"estado\"")))
            .andExpect(content().string(containsString("/js/chat.js")));
    }
}
