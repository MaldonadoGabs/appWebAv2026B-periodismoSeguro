package com.periodismo.seguro.entrada;

import com.periodismo.seguro.validacion.Mensaje;
import com.periodismo.seguro.validacion.MensajeRepo;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

class ChatHandler extends TextWebSocketHandler {
    private final Set<WebSocketSession> sesiones = new CopyOnWriteArraySet<>();
    private final MensajeRepo repo;

    ChatHandler(MensajeRepo repo) { this.repo = repo; }

    @Override public void afterConnectionEstablished(WebSocketSession s) { sesiones.add(s); }
    @Override public void afterConnectionClosed(WebSocketSession s, CloseStatus st) { sesiones.remove(s); }

    @Override
    protected void handleTextMessage(WebSocketSession s, TextMessage msg) throws Exception {
        String t = msg.getPayload();
        if (t.length() > 500) t = t.substring(0, 500);
        Mensaje guardado = repo.save(new Mensaje(t));
        TextMessage out = new TextMessage(guardado.getTexto());
        for (WebSocketSession x : sesiones) if (x.isOpen()) x.sendMessage(out);
    }
}
