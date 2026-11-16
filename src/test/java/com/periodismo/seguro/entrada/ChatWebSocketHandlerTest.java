package com.periodismo.seguro.entrada;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.periodismo.seguro.chat.ChatApi;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

/** El adaptador registra conexiones en ChatApi y entrega los mensajes a la sesion WebSocket. */
class ChatWebSocketHandlerTest {
    private final ChatApi chat = mock(ChatApi.class);
    private final ChatWebSocketHandler handler = new ChatWebSocketHandler(chat);

    @Test
    void unaConexionAutorizadaSeRegistraConSuReceptor() throws Exception {
        WebSocketSession sesion = sesion(true);

        handler.afterConnectionEstablished(sesion);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Consumer<String>> receptor = ArgumentCaptor.forClass(Consumer.class);
        verify(chat).abrirConexion(eq("ws-1"), receptor.capture());
        receptor.getValue().accept("hola");
        ArgumentCaptor<TextMessage> mensaje = ArgumentCaptor.forClass(TextMessage.class);
        verify(sesion).sendMessage(mensaje.capture());
        assertEquals("hola", mensaje.getValue().getPayload());
    }

    @Test
    void unaConexionSinAutorizacionSeCierraYNoSeRegistra() throws Exception {
        WebSocketSession sesion = sesion(false);

        handler.afterConnectionEstablished(sesion);

        verify(sesion).close(any(CloseStatus.class));
        verify(chat, never()).abrirConexion(any(), any());
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
