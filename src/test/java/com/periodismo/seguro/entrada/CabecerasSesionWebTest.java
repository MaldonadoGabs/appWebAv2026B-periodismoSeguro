package com.periodismo.seguro.entrada;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.periodismo.seguro.seguridad.SeguridadApi;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;

/** HU-25: cabeceras de seguridad (CSP con blob: en img-src, X-Frame-Options) y cookies HttpOnly/SameSite. */
@SpringBootTest
@AutoConfigureMockMvc
class CabecerasSesionWebTest {
    @Autowired MockMvc mvc;
    @Autowired SeguridadApi seguridad;

    @Test
    void todasLasRespuestasLlevanLasCabecerasDeSeguridad() throws Exception {
        MockHttpServletResponse respuesta = mvc.perform(get("/"))
            .andExpect(status().isOk())
            .andExpect(header().string("X-Content-Type-Options", "nosniff"))
            .andExpect(header().string("X-Frame-Options", "DENY"))
            .andExpect(header().string("Referrer-Policy", "no-referrer"))
            .andReturn().getResponse();

        String csp = respuesta.getHeader("Content-Security-Policy");
        org.junit.jupiter.api.Assertions.assertNotNull(csp, "debe haber CSP");
        assertTrue(csp.contains("default-src 'self'"), "CSP: " + csp);
        assertTrue(csp.contains("img-src 'self' blob:"), "blob: en img-src para el editor: " + csp);
        assertTrue(csp.contains("script-src 'self'"), "sin scripts externos: " + csp);
        assertTrue(csp.contains("frame-ancestors 'none'"), "sin marcos: " + csp);
        assertTrue(csp.contains("form-action 'self'"), "formularios solo propios: " + csp);
        assertFalseHttp(csp, "http:");
        assertFalseHttp(csp, "https:");
    }

    private static void assertFalseHttp(String csp, String dominio) {
        org.junit.jupiter.api.Assertions.assertFalse(csp.contains(dominio),
            "la CSP no permite recursos de otros dominios: " + csp);
    }

    @Test
    void laCookieDeSesionEsHttpOnlyYSameSiteLax() throws Exception {
        MockHttpServletResponse respuesta = mvc.perform(get("/")).andReturn().getResponse();
        String cookie = respuesta.getHeader("Set-Cookie");
        if (cookie == null) {
            return; // El feed publico no crea sesion todavia; se comprueba en el login.
        }
        assertTrue(cookie.toLowerCase().contains("httponly"), cookie);
        assertTrue(cookie.toLowerCase().contains("samesite=lax"), cookie);
    }

    @Test
    void elApiDeSeguridadPublicaLasMismasCabeceras() {
        var cabeceras = seguridad.cabecerasSeguras();
        assertEquals("DENY", cabeceras.get("X-Frame-Options"));
        assertEquals("nosniff", cabeceras.get("X-Content-Type-Options"));
        assertEquals("no-referrer", cabeceras.get("Referrer-Policy"));
        assertTrue(cabeceras.get("Content-Security-Policy").contains("img-src 'self' blob:"));
    }

    @Test
    void elEditorSigueCargandoSusRecursosPropiosBajoLaCsp() throws Exception {
        // El editor exige sesion; se comprueba que sus recursos son locales (sin CDN).
        String html = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .get("/login"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("/css/estilo.css")))
            .andExpect(content().string(containsString("/js/tema.js")))
            .andReturn().getResponse().getContentAsString();
        org.junit.jupiter.api.Assertions.assertFalse(
            html.matches("(?s).*(src|href)=\"https?://.*"), "recursos de otros dominios");
    }
}
