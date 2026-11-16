package com.periodismo.seguro;

import ch.qos.logback.classic.pattern.MessageConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import java.util.regex.Pattern;

/**
 * HU-26: convertidor de mensaje de log que enmascara correos electronicos y direcciones IP.
 * Vive en la raiz de la aplicacion (infraestructura transversal, fuera de los modulos) para que
 * ningun modulo dependa de Logback: las reglas de arquitectura se mantienen intactas.
 * Se registra como convertidor de Logback para que cualquier %msg se limpie antes de
 * llegar a la consola o al archivo.
 *
 * Uso en logback-spring.xml:
 *   <conversionRule name="msgS" converterClass="com.periodismo.seguro.LogSanitizador"/>
 *   <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msgS%n</pattern>
 */
public class LogSanitizador extends MessageConverter {

    private static final Pattern CORREO = Pattern.compile(
        "[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}");
    private static final Pattern IP = Pattern.compile(
        "\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b");

    @Override
    public String convert(ILoggingEvent evento) {
        return enmascarar(evento.getFormattedMessage());
    }

    /** Reemplaza correos por [correo] e IPs por [ip]. Visibilidad de paquete para las pruebas. */
    static String enmascarar(String texto) {
        if (texto == null || texto.isEmpty()) {
            return texto;
        }
        String resultado = CORREO.matcher(texto).replaceAll("[correo]");
        return IP.matcher(resultado).replaceAll("[ip]");
    }
}
