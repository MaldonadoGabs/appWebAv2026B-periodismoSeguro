package com.periodismo.seguro.notificaciones;

import org.springframework.web.socket.WebSocketHandler;

/** Puerto de entrada de los avisos: lo implementa el adaptador WebSocket de la capa de entrada. */
public interface AvisosWebSocketApi extends WebSocketHandler {}
