package com.periodismo.seguro.entrada;

import com.periodismo.seguro.validacion.MensajeRepo;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
class WsConfig implements WebSocketConfigurer {
    private final MensajeRepo repo;

    WsConfig(MensajeRepo repo) { this.repo = repo; }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry r) {
        r.addHandler(new ChatHandler(repo), "/ws").setAllowedOrigins("*");
    }
}
