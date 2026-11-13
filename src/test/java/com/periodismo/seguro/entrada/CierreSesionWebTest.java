package com.periodismo.seguro.entrada;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.periodismo.seguro.cuentas.CuentasApi;
import com.periodismo.seguro.cuentas.SolicitudRegistro;
import jakarta.servlet.http.Cookie;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/** HU-04: boton de cierre de sesion, invalidacion de la sesion y proteccion posterior. */
@SpringBootTest
@AutoConfigureMockMvc
class CierreSesionWebTest {
    @Autowired MockMvc mvc;
    @Autowired CuentasApi cuentas;

    private MockHttpSession iniciarSesion() throws Exception {
        String correo = "c" + UUID.randomUUID() + "@ejemplo.com";
        cuentas.registrar(new SolicitudRegistro(correo, "Ana", "Segura123"));
        return (MockHttpSession) mvc.perform(post("/login").with(csrf()).param("correo", correo).param("clave", "Segura123"))
            .andReturn().getRequest().getSession();
    }

    @Test
    void conSesionLaCabeceraMuestraElBotonDeCerrarSesionEnCualquierPantalla() throws Exception {
        MockHttpSession sesion = iniciarSesion();
        for (String ruta : new String[] {"/", "/inicio"}) {
            mvc.perform(get(ruta).session(sesion))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Cerrar sesión")))
                .andExpect(content().string(containsString("action=\"/logout\"")));
        }
    }

    @Test
    void sinSesionNoSeMuestraElBotonYSiLosEnlacesDeEntrada() throws Exception {
        mvc.perform(get("/"))
            .andExpect(content().string(not(containsString("Cerrar sesión"))))
            .andExpect(content().string(containsString("Registrarme")))
            .andExpect(content().string(containsString("Entrar")));
    }

    @Test
    void cerrarSesionInvalidaLaSesionYRedirigeAlLoginConAviso() throws Exception {
        MockHttpSession sesion = iniciarSesion();

        MvcResult r = mvc.perform(post("/logout").with(csrf()).session(sesion))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login?salio"))
            .andReturn();

        assertTrue(sesion.isInvalid());
        mvc.perform(get("/login").param("salio", ""))
            .andExpect(content().string(containsString("Cerraste sesión correctamente")));
    }

    @Test
    void cerrarSesionEliminaLaCookieDeSesion() throws Exception {
        MockHttpSession sesion = iniciarSesion();

        MvcResult r = mvc.perform(post("/logout").with(csrf()).session(sesion)).andReturn();

        Cookie cookie = r.getResponse().getCookie("JSESSIONID");
        assertTrue(cookie != null && cookie.getMaxAge() == 0, "la cookie JSESSIONID debe expirar");
    }

    @Test
    void trasCerrarSesionLasPaginasProtegidasRedirigenAlLogin() throws Exception {
        MockHttpSession sesion = iniciarSesion();
        mvc.perform(get("/inicio").session(sesion)).andExpect(status().isOk());

        mvc.perform(post("/logout").with(csrf()).session(sesion));

        mvc.perform(get("/inicio").session(sesion))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("http://localhost/login"));
    }

    @Test
    void cerrarSesionExigeTokenCsrf() throws Exception {
        MockHttpSession sesion = iniciarSesion();
        mvc.perform(post("/logout").session(sesion)).andExpect(status().isForbidden());
    }
}
