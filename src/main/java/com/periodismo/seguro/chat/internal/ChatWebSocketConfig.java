package com.periodismo.seguro.chat.internal;

import com.periodismo.seguro.chat.ChatWebSocketApi;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/** Registro del endpoint WebSocket de la sala; Spring Security tambien protege /ws en el handshake HTTP. */
@Configuration
@EnableWebSocket
class ChatWebSocketConfig implements WebSocketConfigurer {
    private final ChatWebSocketApi handler;
    private final ChatHandshakeInterceptor interceptor;

    ChatWebSocketConfig(ChatWebSocketApi handler, ChatHandshakeInterceptor interceptor) {
        this.handler = handler;
        this.interceptor = interceptor;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registro) {
        registro.addHandler(handler, "/ws").addInterceptors(interceptor);
    }
}
