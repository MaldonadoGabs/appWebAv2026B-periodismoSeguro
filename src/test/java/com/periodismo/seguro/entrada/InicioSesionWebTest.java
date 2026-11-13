package com.periodismo.seguro.entrada;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
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
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** HU-03: formulario de inicio de sesion, mensaje generico, redireccion y paginas protegidas. */
@SpringBootTest
@AutoConfigureMockMvc
class InicioSesionWebTest {
    @Autowired MockMvc mvc;
    @Autowired CuentasApi cuentas;

    private static String correoNuevo() { return "l" + UUID.randomUUID() + "@ejemplo.com"; }

    private ResultActions entrar(String correo, String clave) throws Exception {
        return mvc.perform(post("/login").with(csrf()).param("correo", correo).param("clave", clave));
    }

    @Test
    void elFormularioDeLoginSeMuestraConSusCampos() throws Exception {
        mvc.perform(get("/login"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("name=\"correo\"")))
            .andExpect(content().string(containsString("name=\"clave\"")))
            .andExpect(content().string(containsString("name=\"_csrf\"")));
    }

    @Test
    void credencialesValidasRedirigenALaPantallaPrincipal() throws Exception {
        String correo = correoNuevo();
        cuentas.registrar(new SolicitudRegistro(correo, "Ana", "Segura123"));

        entrar(correo, "Segura123").andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/inicio"));
    }

    @Test
    void laPantallaPrincipalMuestraElSeudonimoYNoLosDatosReales() throws Exception {
        String correo = correoNuevo();
        String seudonimo = cuentas.registrar(new SolicitudRegistro(correo, "Xylophon Quimera", "Segura123")).seudonimo();
        MockHttpSession sesion = (MockHttpSession) entrar(correo, "Segura123").andReturn().getRequest().getSession();

        mvc.perform(get("/inicio").session(sesion))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString(seudonimo)))
            .andExpect(content().string(not(containsString(correo))))
            .andExpect(content().string(not(containsString("Xylophon"))));
    }

    @Test
    void claveIncorrectaYCorreoInexistenteMuestranElMismoMensajeGenerico() throws Exception {
        String correo = correoNuevo();
        cuentas.registrar(new SolicitudRegistro(correo, "Ana", "Segura123"));

        entrar(correo, "Incorrecta1").andExpect(redirectedUrl("/login?error"));
        entrar(correoNuevo(), "Segura123").andExpect(redirectedUrl("/login?error"));
        mvc.perform(get("/login").param("error", ""))
            .andExpect(content().string(containsString("Correo o contraseña incorrectos")));
    }

    @Test
    void tras5FallosElLoginAvisaDelBloqueoAunConLaClaveCorrecta() throws Exception {
        String correo = correoNuevo();
        cuentas.registrar(new SolicitudRegistro(correo, "Ana", "Segura123"));
        for (int i = 0; i < 5; i++) {
            entrar(correo, "Incorrecta1").andExpect(redirectedUrl("/login?error"));
        }

        entrar(correo, "Segura123").andExpect(redirectedUrl("/login?bloqueado"));
        mvc.perform(get("/login").param("bloqueado", ""))
            .andExpect(content().string(containsString("Demasiados intentos")));
    }

    @Test
    void sinSesionLasPaginasProtegidasRedirigenAlLogin() throws Exception {
        mvc.perform(get("/inicio")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("http://localhost/login"));
    }

    @Test
    void lasRutasPublicasSiguenAccesiblesSinSesion() throws Exception {
        for (String ruta : new String[] {"/", "/registro", "/login", "/actuator/health", "/css/estilo.css"}) {
            mvc.perform(get(ruta)).andExpect(status().isOk());
        }
    }

    @Test
    void elEnvioSinTokenCsrfSeRechaza() throws Exception {
        mvc.perform(post("/login").param("correo", "a@b.com").param("clave", "x")).andExpect(status().isForbidden());
    }
}
