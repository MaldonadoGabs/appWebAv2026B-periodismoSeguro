package com.periodismo.seguro.entrada;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.periodismo.seguro.cuentas.CuentasApi;
import com.periodismo.seguro.cuentas.SolicitudRegistro;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;

/** HU-05: formulario de recuperacion, respuesta identica exista o no la cuenta, validacion y CSRF. */
@SpringBootTest
@AutoConfigureMockMvc
class RecuperacionWebTest {
    @Autowired MockMvc mvc;
    @Autowired CuentasApi cuentas;

    private static String correoNuevo() { return "w" + UUID.randomUUID() + "@ejemplo.com"; }

    private MockHttpServletResponse solicitar(String correo) throws Exception {
        return mvc.perform(post("/recuperar").with(csrf()).param("correo", correo)).andReturn().getResponse();
    }

    @Test
    void elFormularioEsPublicoYPideElCorreo() throws Exception {
        mvc.perform(get("/recuperar"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("name=\"correo\"")))
            .andExpect(content().string(containsString("name=\"_csrf\"")))
            .andExpect(content().string(containsString("Recuperar")));
    }

    @Test
    void laRespuestaEsLaMismaExistaONoLaCuenta() throws Exception {
        String correo = correoNuevo();
        cuentas.registrar(new SolicitudRegistro(correo, "Ana", "Segura123"));

        MockHttpServletResponse existente = solicitar(correo);
        MockHttpServletResponse inexistente = solicitar(correoNuevo());

        assertEquals(302, existente.getStatus());
        assertEquals(existente.getStatus(), inexistente.getStatus());
        assertEquals("/recuperar?enviado", existente.getRedirectedUrl());
        assertEquals(existente.getRedirectedUrl(), inexistente.getRedirectedUrl());
    }

    @Test
    void laConfirmacionEsGenericaYNoMuestraElCorreo() throws Exception {
        mvc.perform(get("/recuperar").param("enviado", ""))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Si el correo corresponde a una cuenta")))
            .andExpect(content().string(containsString("30 minutos")))
            .andExpect(content().string(not(containsString("@ejemplo.com"))));
    }

    @Test
    void unCorreoInvalidoMuestraElErrorJuntoAlCampo() throws Exception {
        mvc.perform(post("/recuperar").with(csrf()).param("correo", "no-es-correo"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("msg-error")))
            .andExpect(content().string(containsString("El correo no tiene un formato válido")));
        mvc.perform(post("/recuperar").with(csrf()).param("correo", ""))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("El correo es obligatorio")));
    }

    @Test
    void sinTokenCsrfSeRechaza() throws Exception {
        mvc.perform(post("/recuperar").param("correo", correoNuevo())).andExpect(status().isForbidden());
    }

    @Test
    void elLoginEnlazaALaRecuperacion() throws Exception {
        mvc.perform(get("/login"))
            .andExpect(content().string(containsString("href=\"/recuperar\"")))
            .andExpect(content().string(containsString("Olvidé mi contraseña")));
    }

    @Test
    void unaCuentaExistenteRedirigeSinRevelarDatos() throws Exception {
        String correo = correoNuevo();
        cuentas.registrar(new SolicitudRegistro(correo, "Ana", "Segura123"));

        mvc.perform(post("/recuperar").with(csrf()).param("correo", correo))
            .andExpect(redirectedUrl("/recuperar?enviado"));
    }
}
