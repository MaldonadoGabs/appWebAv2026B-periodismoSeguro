package com.periodismo.seguro.autenticacion.internal;

import com.periodismo.seguro.autenticacion.AutenticacionApi;
import java.util.Optional;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
class AutenticacionService implements AutenticacionApi {
    private final AuthenticationConfiguration configuracion;
    private final SessionRegistry sesiones;
    private final ControlIntentos intentos;

    AutenticacionService(AuthenticationConfiguration configuracion, SessionRegistry sesiones, ControlIntentos intentos) {
        this.configuracion = configuracion;
        this.sesiones = sesiones;
        this.intentos = intentos;
    }

    /** HU-03: verifica correo y clave con BCrypt y el mismo control de intentos del formulario. */
    @Override
    public Optional<String> verificarCredenciales(String correo, String clave) {
        try {
            var manager = configuracion.getAuthenticationManager();
            var auth = manager.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(correo, clave));
            intentos.registrarExito(correo);
            return Optional.of(auth.getName());
        } catch (AuthenticationException e) {
            return Optional.empty();
        } catch (Exception e) {
            throw new IllegalStateException("Autenticacion no disponible", e);
        }
    }

    @Override
    public void cerrarSesion(String sesionId) {}

    /** Fail-closed: solo es valida una sesion registrada y no expirada. */
    @Override
    public Optional<String> validarSesion(String sesionId) {
        if (sesionId == null) {
            return Optional.empty();
        }
        SessionInformation info = sesiones.getSessionInformation(sesionId);
        if (info == null || info.isExpired() || !(info.getPrincipal() instanceof UserDetails usuario)) {
            return Optional.empty();
        }
        return Optional.of(usuario.getUsername());
    }

    @Override
    public void solicitarRecuperacion(String seudonimo) {}

    @Override
    public boolean restablecerClave(String token, String nuevaClave) { return false; }
}
