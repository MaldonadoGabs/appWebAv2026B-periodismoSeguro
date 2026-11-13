package com.periodismo.seguro.autenticacion.internal;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
class ConfiguracionCorreo {
    /** Cliente HTTP de Resend con tiempos maximos. Falla al arrancar si falta la API key (RESEND_API_KEY). */
    @Bean("clienteResend")
    @ConditionalOnProperty(name = "app.correo.proveedor", havingValue = "resend")
    RestClient clienteResend(@Value("${app.correo.resend.api-key:}") String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Falta RESEND_API_KEY para usar el proveedor de correo resend");
        }
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(5000);
        fabrica.setReadTimeout(5000);
        return RestClient.builder()
            .baseUrl("https://api.resend.com")
            .requestFactory(fabrica)
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
            .build();
    }
}
