package com.periodismo.seguro.entrada;

import com.periodismo.seguro.chat.ChatApi;
import com.periodismo.seguro.chat.ChatWebSocketApi;
import java.io.IOException;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/** Adaptador WebSocket de la sala: traduce sesiones del transporte a conexiones de ChatApi. */
@Component
class ChatWebSocketHandler extends TextWebSocketHandler implements ChatWebSocketApi {
    static final String CUENTA_ID = ChatWebSocketApi.CUENTA_ID;

    private final ChatApi chat;

    ChatWebSocketHandler(ChatApi chat) {
        this.chat = chat;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession sesion) throws Exception {
        if (sesion.getAttributes().get(CUENTA_ID) == null) {
            sesion.close(CloseStatus.POLICY_VIOLATION.withReason("Sin autorizacion"));
            return;
        }
        String conexionId = sesion.getId();
        chat.abrirConexion(conexionId, texto -> {
            try {
                sesion.sendMessage(new TextMessage(texto));
            } catch (IOException e) {
                chat.cerrarConexion(conexionId);
            }
        });
    }

    @Override
    protected void handleTextMessage(@NonNull WebSocketSession sesion, @NonNull TextMessage mensaje) throws Exception {
        chat.enviar(mensaje.getPayload());
    }

    @Override
    public void afterConnectionClosed(@NonNull WebSocketSession sesion, @NonNull CloseStatus estado) {
        chat.cerrarConexion(sesion.getId());
    }

    @Override
    public void handleTransportError(@NonNull WebSocketSession sesion, @NonNull Throwable error) throws Exception {
        chat.cerrarConexion(sesion.getId());
        if (sesion.isOpen()) {
            sesion.close(CloseStatus.SERVER_ERROR);
        }
    }
}
