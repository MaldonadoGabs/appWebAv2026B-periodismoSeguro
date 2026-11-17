package com.periodismo.seguro.entrada;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.periodismo.seguro.notificaciones.AvisoNoticia;
import com.periodismo.seguro.notificaciones.AvisosWebSocketApi;
import com.periodismo.seguro.notificaciones.NotificacionesApi;
import java.io.IOException;
import java.util.Map;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/** HU-21: adaptador WebSocket de avisos. Serializa JSON y deja el dominio sin detalles del transporte. */
@Component
class AvisosWebSocketHandler extends TextWebSocketHandler implements AvisosWebSocketApi {
    private final NotificacionesApi notificaciones;
    private final ObjectMapper json;

    AvisosWebSocketHandler(NotificacionesApi notificaciones, ObjectMapper json) {
        this.notificaciones = notificaciones;
        this.json = json;
    }

    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession sesion) {
        notificaciones.abrirConexion(sesion.getId(), aviso -> enviarJson(sesion, aviso));
    }

    private void enviarJson(WebSocketSession sesion, AvisoNoticia aviso) {
        if (!sesion.isOpen()) {
            return;
        }
        try {
            Map<String, Object> evento = Map.of(
                "tipo", "noticia",
                "noticiaId", aviso.noticiaId(),
                "titulo", aviso.titulo(),
                "autor", aviso.autorSeudonimo(),
                "url", "/noticias/" + aviso.noticiaId());
            sesion.sendMessage(new TextMessage(json.writeValueAsString(evento)));
        } catch (IOException e) {
            notificaciones.cerrarConexion(sesion.getId());
        }
    }

    @Override
    public void afterConnectionClosed(@NonNull WebSocketSession sesion, @NonNull CloseStatus estado) {
        notificaciones.cerrarConexion(sesion.getId());
    }

    @Override
    public void handleTransportError(@NonNull WebSocketSession sesion, @NonNull Throwable error) throws Exception {
        notificaciones.cerrarConexion(sesion.getId());
        if (sesion.isOpen()) {
            sesion.close(CloseStatus.SERVER_ERROR);
        }
    }
}
