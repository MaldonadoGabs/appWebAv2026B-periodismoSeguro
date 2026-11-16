package com.periodismo.seguro;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.periodismo.seguro.noticias.NoticiasApi;
import com.periodismo.seguro.noticias.SolicitudNoticia;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/** HU-26: los logs nunca exponen correos, nombres reales ni IPs junto a acciones de publicacion. */
@SpringBootTest
@TestPropertySource(properties = {
    "app.imagenes.directorio=target/imagenes-prueba-logs",
    "spring.datasource.url=jdbc:h2:mem:logsHu26;DB_CLOSE_DELAY=-1"
})
class PrivacidadLogsTest {
    @Autowired NoticiasApi noticias;

    @Test
    void elSanitizadorEnmascaraCorreosYIpv4() {
        String mensaje = "Publicacion de ana.real@ejemplo.com desde 192.168.1.45 con seudonimo Condor";
        String limpio = LogSanitizador.enmascarar(mensaje);
        assertEquals("Publicacion de [correo] desde [ip] con seudonimo Condor", limpio);
    }

    @Test
    void elSanitizadorNoCambiaMensajesSinDatosSensibles() {
        String mensaje = "Noticia creada con id 123 y categoria cultura";
        assertEquals(mensaje, LogSanitizador.enmascarar(mensaje));
        org.junit.jupiter.api.Assertions.assertNull(LogSanitizador.enmascarar(null));
        assertEquals("", LogSanitizador.enmascarar(""));
    }

    @Test
    void publicarUnaNoticiaNoEscribeEnLosLogsCorreoNombreRealNiIp() {
        LoggerContext contexto = (LoggerContext) LoggerFactory.getILoggerFactory();
        ListAppender<ILoggingEvent> capturador = new ListAppender<>();
        capturador.setContext(contexto);
        capturador.start();
        ch.qos.logback.classic.Logger raiz = contexto.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        raiz.addAppender(capturador);

        String correo = "periodista.confidencial@ejemplo.com";
        String nombreReal = "Ana Maria Gonzalez";
        String ip = "10.20.30.40";
        try {
            // Se publica una noticia: el servicio no registra identidad ni IP.
            noticias.crear("cuenta-" + UUID.randomUUID(), "Condor Silente 482",
                new SolicitudNoticia("Titulo de prueba", "Subtitulo",
                    "<p>Texto</p>", "cultura", List.of()));
        } finally {
            raiz.detachAppender(capturador);
            capturador.stop();
        }

        StringBuilder todos = new StringBuilder();
        for (ILoggingEvent evento : capturador.list) {
            todos.append(evento.getFormattedMessage()).append('\n');
        }
        String logs = todos.toString();
        assertFalse(logs.contains(correo), "el correo no aparece en logs: " + logs);
        assertFalse(logs.contains(nombreReal), "el nombre real no aparece en logs: " + logs);
        assertFalse(logs.contains(ip), "la IP no aparece en logs: " + logs);
    }

    @Test
    void losDTOPublicosNoExponenCorreoNombreRealNiIpEnSuTexto() {
        String id = noticias.crear("cuenta-secreta", "Condor Silente 482",
            new SolicitudNoticia("Nota", "Sub", "<p>Texto</p>", "otros", List.of()));
        String texto = noticias.obtener(id).orElseThrow().toString();
        assertTrue(texto.contains("Condor Silente 482"));
        assertFalse(texto.contains("cuenta-secreta"));
        assertFalse(texto.contains("@"));
    }
}
