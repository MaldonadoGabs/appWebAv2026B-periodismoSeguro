package com.periodismo.seguro.autenticacion.internal;

import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Proveedor de correo real (app.correo.proveedor=resend): envia el enlace por la API de Resend.
 * Si el envio falla se registra el tipo de error, nunca el destinatario, y el usuario recibe la misma respuesta
 * de siempre (no se revela si la cuenta existe).
 */
@Component
@ConditionalOnProperty(name = "app.correo.proveedor", havingValue = "resend")
class EnvioRecuperacionResend implements EnvioRecuperacion {
    private static final Logger log = LoggerFactory.getLogger(EnvioRecuperacionResend.class);

    private final RestClient http;
    private final String remitente;

    EnvioRecuperacionResend(@Qualifier("clienteResend") RestClient http, @Value("${app.correo.remitente}") String remitente) {
        this.http = http;
        this.remitente = remitente;
    }

    @Override
    public void enviar(String destinatario, String enlace) {
        String seguro = enlace.replace("\"", "%22");
        Map<String, Object> cuerpo = Map.of(
            "from", remitente,
            "to", List.of(destinatario),
            "subject", "Recupera tu contraseña de Periodismo Seguro",
            "text", "Pediste restablecer tu contraseña.\n\n" + enlace
                + "\n\nEl enlace sirve una sola vez y caduca en 30 minutos. Si no fuiste tú, ignora este mensaje.",
            "html", "<p>Pediste restablecer tu contraseña de Periodismo Seguro.</p>"
                + "<p><a href=\"" + seguro + "\">Restablecer mi contraseña</a></p>"
                + "<p>El enlace sirve una sola vez y caduca en 30 minutos. Si no fuiste tú, ignora este mensaje.</p>");
        try {
            http.post().uri("/emails").contentType(MediaType.APPLICATION_JSON).body(cuerpo).retrieve().toBodilessEntity();
        } catch (RestClientException e) {
            log.error("No se pudo enviar el correo de recuperacion: {}", e.getClass().getSimpleName());
        }
    }
}
