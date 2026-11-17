package com.periodismo.seguro.entrada;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.periodismo.seguro.notificaciones.AvisoNoticia;
import com.periodismo.seguro.notificaciones.NotificacionesApi;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

/** HU-21: el adaptador convierte el aviso del dominio en JSON con enlace al detalle. */
class AvisosWebSocketHandlerTest {
    @Test
    void elAdaptadorRegistraLaConexionYEnviaTituloSeudonimoYEnlace() throws Exception {
        NotificacionesApi notificaciones = mock(NotificacionesApi.class);
        WebSocketSession sesion = mock(WebSocketSession.class);
        when(sesion.getId()).thenReturn("c1");
        when(sesion.isOpen()).thenReturn(true);
        AvisosWebSocketHandler handler = new AvisosWebSocketHandler(notificaciones, new ObjectMapper());

        handler.afterConnectionEstablished(sesion);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Consumer<AvisoNoticia>> receptor = ArgumentCaptor.forClass(Consumer.class);
        verify(notificaciones).abrirConexion(eq("c1"), receptor.capture());
        receptor.getValue().accept(new AvisoNoticia("n-1", "Titulo", "Condor Silente 482"));

        ArgumentCaptor<TextMessage> mensaje = ArgumentCaptor.forClass(TextMessage.class);
        verify(sesion).sendMessage(mensaje.capture());
        String json = mensaje.getValue().getPayload();
        assertTrue(json.contains("\"tipo\":\"noticia\""), json);
        assertTrue(json.contains("\"titulo\":\"Titulo\""), json);
        assertTrue(json.contains("\"autor\":\"Condor Silente 482\""), json);
        assertTrue(json.contains("\"url\":\"/noticias/n-1\""), json);
    }
}
