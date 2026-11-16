package com.periodismo.seguro.entrada;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.periodismo.seguro.chat.ChatApi;
import com.periodismo.seguro.chat.SuscripcionChat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

/** El adaptador registra conexiones en ChatApi y envia mensajes y conectados como JSON. */
class ChatWebSocketHandlerTest {
    private final ChatApi chat = mock(ChatApi.class);
    private final ObjectMapper json = new ObjectMapper();
    private final ChatWebSocketHandler handler = new ChatWebSocketHandler(chat, json);

    @Test
    void unaConexionAutorizadaRecibeMensajesYConectados() throws Exception {
        WebSocketSession sesion = sesion(true);

        handler.afterConnectionEstablished(sesion);

        ArgumentCaptor<SuscripcionChat> suscripcion = ArgumentCaptor.forClass(SuscripcionChat.class);
        verify(chat).abrirConexion(eq("ws-1"), eq("cuenta-1"), suscripcion.capture());
        suscripcion.getValue().mensajes().accept("hola");
        suscripcion.getValue().conectados().accept(List.of("Ana_Rojo"));

        ArgumentCaptor<TextMessage> enviados = ArgumentCaptor.forClass(TextMessage.class);
        verify(sesion, times(2)).sendMessage(enviados.capture());
        Map<?, ?> mensaje = json.readValue(enviados.getAllValues().get(0).getPayload(), Map.class);
        assertEquals("mensaje", mensaje.get("tipo"));
        assertEquals("hola", mensaje.get("texto"));
        Map<?, ?> conectados = json.readValue(enviados.getAllValues().get(1).getPayload(), Map.class);
        assertEquals("conectados", conectados.get("tipo"));
        assertEquals(List.of("Ana_Rojo"), conectados.get("usuarios"));
    }

    @Test
    void unaConexionSinAutorizacionSeCierraYNoSeRegistra() throws Exception {
        WebSocketSession sesion = sesion(false);

        handler.afterConnectionEstablished(sesion);

        verify(sesion).close(any(CloseStatus.class));
        verify(chat, never()).abrirConexion(anyString(), anyString(), any());
    }

    @Test
    void elTextoRecibidoSeEntregaALaSala() throws Exception {
        handler.handleTextMessage(sesion(true), new TextMessage("mensaje"));
        verify(chat).enviar("mensaje");
    }

    @Test
    void alCerrarLaSesionSeRetiraLaConexion() throws Exception {
        WebSocketSession sesion = sesion(true);
        handler.afterConnectionClosed(sesion, CloseStatus.NORMAL);
        verify(chat).cerrarConexion("ws-1");
    }

    private static WebSocketSession sesion(boolean autorizada) {
        WebSocketSession sesion = mock(WebSocketSession.class);
        Map<String, Object> atributos = new HashMap<>();
        if (autorizada) {
            atributos.put(ChatWebSocketHandler.CUENTA_ID, "cuenta-1");
        }
        when(sesion.getAttributes()).thenReturn(atributos);
        when(sesion.getId()).thenReturn("ws-1");
        when(sesion.isOpen()).thenReturn(true);
        return sesion;
    }
}
