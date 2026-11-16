package com.periodismo.seguro.chat.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.periodismo.seguro.chat.ChatApi;
import com.periodismo.seguro.chat.ChatWebSocketApi;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/** Solo entra al WebSocket quien tiene sesion valida y el rol PERIODISTA. */
class ChatHandshakeInterceptorTest {
    private static final String CUENTA = "cuenta-1";
    private static final String SESION = "sesion-http-1";

    private ChatApi chat;
    private ChatHandshakeInterceptor interceptor;
    private Map<String, Object> atributos;
    private ServletServerHttpRequest peticion;

    @BeforeEach
    void preparar() {
        chat = mock(ChatApi.class);
        interceptor = new ChatHandshakeInterceptor(chat);
        atributos = new HashMap<>();
        HttpSession sesionHttp = mock(HttpSession.class);
        HttpServletRequest servlet = mock(HttpServletRequest.class);
        peticion = mock(ServletServerHttpRequest.class);
        when(sesionHttp.getId()).thenReturn(SESION);
        when(servlet.getSession(false)).thenReturn(sesionHttp);
        when(peticion.getServletRequest()).thenReturn(servlet);
        when(chat.conectar(SESION)).thenReturn(Optional.of(CUENTA));
    }

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void elPeriodistaConSesionValidaAbreElSocket() {
        autenticar(CUENTA, "ROLE_PERIODISTA");
        assertTrue(interceptor.beforeHandshake(peticion, null, null, atributos));
        assertEquals(CUENTA, atributos.get(ChatWebSocketApi.CUENTA_ID));
    }

    @Test
    void elLectorNoPuedeAbrirElSocket() {
        autenticar(CUENTA, "ROLE_LECTOR");
        assertFalse(interceptor.beforeHandshake(peticion, null, null, atributos));
        assertTrue(atributos.isEmpty());
    }

    @Test
    void sinAutenticacionNoSeAbreElSocket() {
        assertFalse(interceptor.beforeHandshake(peticion, null, null, atributos));
    }

    @Test
    void unUsuarioAnonimoNoPuedeAbrirElSocket() {
        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken(
            "anon", "anonymous", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));
        assertFalse(interceptor.beforeHandshake(peticion, null, null, atributos));
    }

    @Test
    void unaSesionInvalidaNoAbreElSocket() {
        autenticar(CUENTA, "ROLE_PERIODISTA");
        when(chat.conectar(SESION)).thenReturn(Optional.empty());
        assertFalse(interceptor.beforeHandshake(peticion, null, null, atributos));
    }

    @Test
    void noSePuedeUsarLaSesionDeOtraCuenta() {
        autenticar("otra-cuenta", "ROLE_PERIODISTA");
        assertFalse(interceptor.beforeHandshake(peticion, null, null, atributos));
    }

    private static void autenticar(String cuenta, String rol) {
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
            cuenta, null, List.of(new SimpleGrantedAuthority(rol))));
    }
}
