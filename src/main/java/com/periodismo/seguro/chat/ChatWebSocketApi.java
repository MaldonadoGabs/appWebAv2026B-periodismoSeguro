package com.periodismo.seguro.chat;

import org.springframework.web.socket.WebSocketHandler;

/** Puerto de entrada del chat: lo implementa el adaptador WebSocket de la capa de entrada. */
public interface ChatWebSocketApi extends WebSocketHandler {
    /** Atributo del handshake que identifica a la cuenta autorizada en la sesion WebSocket. */
    String CUENTA_ID = "cuentaId";
}
