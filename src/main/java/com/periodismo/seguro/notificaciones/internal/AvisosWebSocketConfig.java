package com.periodismo.seguro.notificaciones.internal;

import com.periodismo.seguro.notificaciones.AvisosWebSocketApi;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * HU-21: endpoint publico de avisos. Es distinto de /ws (chat) para no exigir sesion de periodista:
 * cualquier persona con una pantalla abierta puede enterarse de una nota nueva.
 */
@Configuration
@EnableWebSocket
class AvisosWebSocketConfig implements WebSocketConfigurer {
    private final AvisosWebSocketApi handler;

    AvisosWebSocketConfig(AvisosWebSocketApi handler) { this.handler = handler; }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registro) {
        registro.addHandler(handler, "/avisos-ws");
    }
}
