package com.periodismo.seguro.entrada;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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

/** HU-08 y HU-04: reglas de acceso por rol, 403 para el rol incorrecto y registro sin eleccion de rol. */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "app.imagenes.directorio=target/imagenes-prueba-roles",
    "spring.datasource.url=jdbc:h2:mem:rolesHu08;DB_CLOSE_DELAY=-1"
})
class RolesWebTest {
    private static final List<String> RUTAS_PERIODISTA = List.of("/inicio", "/mis-notas", "/noticias/nueva", "/chat");

    @Autowired MockMvc mvc;
    @Autowired CuentasApi cuentas;

    private MockHttpSession periodista() throws Exception {
        String correo = "r" + UUID.randomUUID() + "@ejemplo.com";
        cuentas.registrar(new SolicitudRegistro(correo, "Ana Periodista", "Segura123"));
        return (MockHttpSession) mvc.perform(post("/login").with(csrf())
                .param("correo", correo).param("clave", "Segura123"))
            .andReturn().getRequest().getSession();
    }

    @Test
    void elPeriodistaAutenticadoAbreElPanelMisNotasEscrituraYChat() throws Exception {
        MockHttpSession sesion = periodista();
        for (String ruta : RUTAS_PERIODISTA) {
            mvc.perform(get(ruta).session(sesion)).andExpect(status().isOk());
        }
    }

    @Test
    void elPanelMuestraEnlacesClarosAMisNotasEscribirChatYCerrarSesion() throws Exception {
        mvc.perform(get("/inicio").session(periodista()))
            .andExpect(content().string(containsString("href=\"/mis-notas\"")))
            .andExpect(content().string(containsString("href=\"/noticias/nueva\"")))
            .andExpect(content().string(containsString("href=\"/chat\"")))
            .andExpect(content().string(containsString("action=\"/logout\"")));
    }

    @Test
    void sinSesionNiElPanelNiLasNoticiasNiElChatSeAbren() throws Exception {
        for (String ruta : RUTAS_PERIODISTA) {
            mvc.perform(get(ruta)).andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/login"));
        }
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
    void elLectorNoPuedeCrearNoticiasNiAbrirElPanelNiElChat() throws Exception {
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
    void elLectorSigueViendoElFeedPublicoYNoVeEnlacesDePeriodista() throws Exception {
        mvc.perform(get("/"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("id=\"feed\"")))
            .andExpect(content().string(not(containsString("Escribir noticia"))))
            .andExpect(content().string(not(containsString("Mis notas"))));
    }

    @Test
    @WithMockUser(username = "cuenta-admin", roles = "ADMINISTRADOR")
    void elAdministradorNoHeredaElAccesoDePeriodista() throws Exception {
        for (String ruta : RUTAS_PERIODISTA) {
            mvc.perform(get(ruta)).andExpect(status().isForbidden());
        }
    }

    @Test
    void elRegistroPublicoNoPermiteElegirRolYCreaPeriodistas() throws Exception {
        String correo = "nuevo" + UUID.randomUUID() + "@ejemplo.com";
        mvc.perform(post("/registro").with(csrf())
                .param("correo", correo).param("nombreReal", "Ana").param("clave", "Segura123")
                .param("confirmacion", "Segura123").param("rol", "ADMINISTRADOR"))
            .andExpect(status().is3xxRedirection());

        assertEquals(Rol.PERIODISTA, cuentas.credencialesPorCorreo(correo).orElseThrow().rol());
    }

    @Test
    void elChatMuestraElEstadoDelWebSocketYElFormularioParaPeriodistas() throws Exception {
        mvc.perform(get("/chat").session(periodista()))
            .andExpect(content().string(containsString("id=\"log\"")))
            .andExpect(content().string(containsString("id=\"estado\"")))
            .andExpect(content().string(containsString("/js/chat.js")));
    }
}
