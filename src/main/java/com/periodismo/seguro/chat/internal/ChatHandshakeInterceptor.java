package com.periodismo.seguro.chat.internal;

import com.periodismo.seguro.chat.ChatApi;
import com.periodismo.seguro.chat.ChatWebSocketApi;
import jakarta.servlet.http.HttpSession;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

/** Puerta de /ws: exige sesion HTTP valida y rol LECTOR antes de abrir el socket. */
@Component
class ChatHandshakeInterceptor implements HandshakeInterceptor {
    private static final String ROL_LECTOR = "ROLE_LECTOR";

    private final ChatApi chat;

    ChatHandshakeInterceptor(ChatApi chat) {
        this.chat = chat;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacion == null || !autenticacion.isAuthenticated()
            || autenticacion.getAuthorities().stream().noneMatch(a -> ROL_LECTOR.equals(a.getAuthority()))) {
            return false;
        }
        if (!(request instanceof ServletServerHttpRequest servlet)) {
            return false;
        }
        HttpSession sesionHttp = servlet.getServletRequest().getSession(false);
        if (sesionHttp == null) {
            return false;
        }
        Optional<String> cuentaId = chat.conectar(sesionHttp.getId());
        if (cuentaId.isEmpty() || !cuentaId.get().equals(autenticacion.getName())) {
            return false;
        }
        attributes.put(ChatWebSocketApi.CUENTA_ID, cuentaId.get());
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // Sin accion: la autorizacion se decide antes de abrir la conexion.
    }
}
