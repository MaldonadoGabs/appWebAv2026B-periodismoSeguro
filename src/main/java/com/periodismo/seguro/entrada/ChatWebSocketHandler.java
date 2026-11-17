package com.periodismo.seguro.entrada;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.periodismo.seguro.chat.ChatApi;
import com.periodismo.seguro.chat.ChatWebSocketApi;
import com.periodismo.seguro.chat.SuscripcionChat;
import java.io.IOException;
import java.util.Map;
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
    private final ObjectMapper json;

    ChatWebSocketHandler(ChatApi chat, ObjectMapper json) {
        this.chat = chat;
        this.json = json;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession sesion) throws Exception {
        if (!(sesion.getAttributes().get(CUENTA_ID) instanceof String cuentaId)) {
            sesion.close(CloseStatus.POLICY_VIOLATION.withReason("Sin autorizacion"));
            return;
        }
        String conexionId = sesion.getId();
        SuscripcionChat suscripcion = new SuscripcionChat(
            mensaje -> enviarJson(sesion, conexionId, Map.of(
                "tipo", "mensaje",
                "autor", mensaje.autorSeudonimo(),
                "texto", mensaje.texto(),
                "hora", mensaje.enviadoEn().toString())),
            usuarios -> enviarJson(sesion, conexionId, Map.of("tipo", "conectados", "usuarios", usuarios)),
            usuario -> enviarJson(sesion, conexionId, Map.of("tipo", "ingreso", "usuario", usuario)),
            estado -> enviarJson(sesion, conexionId,
                Map.of("tipo", "escribiendo", "usuario", estado.usuario(), "activo", estado.activo())));
        chat.abrirConexion(conexionId, cuentaId, suscripcion);
    }

    private void enviarJson(WebSocketSession sesion, String conexionId, Object evento) {
        if (!sesion.isOpen()) {
            return;
        }
        try {
            sesion.sendMessage(new TextMessage(json.writeValueAsString(evento)));
        } catch (IOException e) {
            chat.cerrarConexion(conexionId);
        }
    }

    @Override
    protected void handleTextMessage(@NonNull WebSocketSession sesion, @NonNull TextMessage mensaje) throws Exception {
        if (!(sesion.getAttributes().get(CUENTA_ID) instanceof String cuentaId)) {
            return;
        }
        String texto = mensaje.getPayload();
        try {
            JsonNode entrada = json.readTree(texto);
            if (entrada.isObject() && "escribiendo".equals(entrada.path("tipo").asText())) {
                chat.avisarEscritura(cuentaId, entrada.path("activo").asBoolean(false));
                return;
            }
        } catch (JsonProcessingException e) {
            // Texto plano de clientes antiguos: se trata como mensaje normal.
        }
        chat.enviar(cuentaId, texto);
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
