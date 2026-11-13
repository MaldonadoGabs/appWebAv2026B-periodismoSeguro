package com.periodismo.seguro.entrada;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.periodismo.seguro.autenticacion.AutenticacionApi;
import com.periodismo.seguro.cuentas.CuentasApi;
import com.periodismo.seguro.cuentas.SolicitudRegistro;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** HU-06: pagina de nueva clave desde el enlace, errores claros, reglas del registro, un solo uso y CSRF. */
@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class RestablecerWebTest {
    private static final Pattern ENLACE = Pattern.compile("/restablecer\\?token=([A-Za-z0-9_-]+)");
    private static final String INVALIDO = "El enlace de recuperación no es válido";

    @Autowired MockMvc mvc;
    @Autowired CuentasApi cuentas;
    @Autowired AutenticacionApi auth;

    private static String correoNuevo() { return "x" + UUID.randomUUID() + "@ejemplo.com"; }

    private String tokenPara(String correo, CapturedOutput salida) {
        cuentas.registrar(new SolicitudRegistro(correo, "Ana", "Segura123"));
        auth.solicitarRecuperacion(correo);
        Matcher m = ENLACE.matcher(salida.getAll());
        String ultimo = null;
        while (m.find()) {
            ultimo = m.group(1);
        }
        return ultimo;
    }

    private ResultActions enviar(String token, String clave, String confirmacion) throws Exception {
        return mvc.perform(post("/restablecer").with(csrf())
            .param("token", token).param("clave", clave).param("confirmacion", confirmacion));
    }

    @Test
    void conUnTokenVigenteMuestraElFormularioSinSesion(CapturedOutput salida) throws Exception {
        String token = tokenPara(correoNuevo(), salida);

        mvc.perform(get("/restablecer").param("token", token))
            .andExpect(status().isOk())
            .andExpect(header().string("Referrer-Policy", "no-referrer"))
            .andExpect(content().string(containsString("name=\"clave\"")))
            .andExpect(content().string(containsString("name=\"confirmacion\"")))
            .andExpect(content().string(containsString("name=\"_csrf\"")))
            .andExpect(content().string(containsString("Nueva")))
            .andExpect(content().string(not(containsString(INVALIDO))));
    }

    @Test
    void unTokenInexistenteOAusenteMuestraUnErrorClaro() throws Exception {
        mvc.perform(get("/restablecer").param("token", "no-existe"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("nota error")))
            .andExpect(content().string(containsString(INVALIDO)))
            .andExpect(content().string(containsString("href=\"/recuperar\"")))
            .andExpect(content().string(not(containsString("name=\"clave\""))));
        mvc.perform(get("/restablecer"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString(INVALIDO)));
    }

    @Test
    void unaClaveDebilMarcaErrorJuntoAlCampoYNoGastaElToken(CapturedOutput salida) throws Exception {
        String token = tokenPara(correoNuevo(), salida);

        enviar(token, "debil", "debil")
            .andExpect(status().isOk())
            .andExpect(model().attributeHasFieldErrors("form", "clave"))
            .andExpect(content().string(containsString("msg-error")))
            .andExpect(content().string(containsString("Mínimo 8 caracteres, con mayúscula, minúscula y número")));
        enviar(token, "sinmayuscula1", "sinmayuscula1").andExpect(model().attributeHasFieldErrors("form", "clave"));
        mvc.perform(get("/restablecer").param("token", token))
            .andExpect(content().string(not(containsString(INVALIDO))));
    }

    @Test
    void laConfirmacionDistintaMarcaError(CapturedOutput salida) throws Exception {
        String token = tokenPara(correoNuevo(), salida);

        enviar(token, "Distinta456", "Distinta457")
            .andExpect(status().isOk())
            .andExpect(model().attributeHasFieldErrors("form", "confirmacion"))
            .andExpect(content().string(containsString("Las contraseñas no coinciden")));
    }

    @Test
    void alGuardarRedirigeAlLoginYLaNuevaClaveSirve(CapturedOutput salida) throws Exception {
        String correo = correoNuevo();
        String token = tokenPara(correo, salida);

        enviar(token, "Distinta456", "Distinta456").andExpect(redirectedUrl("/login?restablecida"));

        mvc.perform(get("/login").param("restablecida", ""))
            .andExpect(content().string(containsString("Tu contraseña se cambió")));
        mvc.perform(post("/login").with(csrf()).param("correo", correo).param("clave", "Distinta456"))
            .andExpect(redirectedUrl("/inicio"));
        mvc.perform(post("/login").with(csrf()).param("correo", correo).param("clave", "Segura123"))
            .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    void elEnlaceNoSePuedeUsarDosVeces(CapturedOutput salida) throws Exception {
        String token = tokenPara(correoNuevo(), salida);
        enviar(token, "Distinta456", "Distinta456").andExpect(redirectedUrl("/login?restablecida"));

        enviar(token, "OtraMas789", "OtraMas789")
            .andExpect(status().isOk())
            .andExpect(content().string(containsString(INVALIDO)));
        mvc.perform(get("/restablecer").param("token", token))
            .andExpect(content().string(containsString(INVALIDO)));
    }

    @Test
    void sinTokenCsrfSeRechaza(CapturedOutput salida) throws Exception {
        String token = tokenPara(correoNuevo(), salida);

        mvc.perform(post("/restablecer").param("token", token)
                .param("clave", "Distinta456").param("confirmacion", "Distinta456"))
            .andExpect(status().isForbidden());
        mvc.perform(get("/restablecer").param("token", token))
            .andExpect(content().string(not(containsString(INVALIDO))));
    }
}
