package com.periodismo.seguro.autenticacion.internal;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/** Envio real del correo de recuperacion con Resend, probado contra un servidor simulado (no se manda nada real). */
@SpringBootTest
@ExtendWith(OutputCaptureExtension.class)
class EnvioRecuperacionResendTest {
    private static final String ENLACE = "https://app.ejemplo.org/restablecer?token=abc123";

    @Autowired EnvioRecuperacion porDefecto;

    private static EnvioRecuperacionResend crear(MockRestServiceServer[] servidor) {
        RestClient.Builder constructor = RestClient.builder()
            .baseUrl("https://api.resend.com")
            .defaultHeader("Authorization", "Bearer re_prueba");
        servidor[0] = MockRestServiceServer.bindTo(constructor).build();
        return new EnvioRecuperacionResend(constructor.build(), "Periodismo Seguro <aviso@ejemplo.org>");
    }

    @Test
    void sinConfiguracionElProveedorPorDefectoEsLaConsola() {
        assertInstanceOf(EnvioRecuperacionConsola.class, porDefecto);
    }

    @Test
    void enviaElEnlaceAlDestinatarioConLaClaveDeApi() {
        MockRestServiceServer[] servidor = new MockRestServiceServer[1];
        EnvioRecuperacionResend envio = crear(servidor);
        servidor[0].expect(requestTo("https://api.resend.com/emails"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header("Authorization", "Bearer re_prueba"))
            .andExpect(jsonPath("$.from").value("Periodismo Seguro <aviso@ejemplo.org>"))
            .andExpect(jsonPath("$.to[0]").value("ana@ejemplo.com"))
            .andExpect(jsonPath("$.subject").value(containsString("contraseña")))
            .andExpect(jsonPath("$.text").value(containsString(ENLACE)))
            .andExpect(jsonPath("$.html").value(containsString(ENLACE)))
            .andRespond(withSuccess("{\"id\":\"1\"}", MediaType.APPLICATION_JSON));

        envio.enviar("ana@ejemplo.com", ENLACE);

        servidor[0].verify();
    }

    @Test
    void siResendFallaNoSeLanzaErrorNiSeRegistraElCorreo(CapturedOutput salida) {
        MockRestServiceServer[] servidor = new MockRestServiceServer[1];
        EnvioRecuperacionResend envio = crear(servidor);
        servidor[0].expect(requestTo("https://api.resend.com/emails")).andRespond(withServerError());

        assertDoesNotThrow(() -> envio.enviar("secreto@ejemplo.com", ENLACE));

        assertFalse(salida.getAll().contains("secreto@ejemplo.com"), "el correo no aparece en los logs");
        assertFalse(salida.getAll().contains("abc123"), "el token no aparece en los logs");
    }

    @Test
    void sinApiKeyElProveedorResendNoArranca() {
        assertThrows(IllegalStateException.class, () -> new ConfiguracionCorreo().clienteResend(" "));
    }
}
