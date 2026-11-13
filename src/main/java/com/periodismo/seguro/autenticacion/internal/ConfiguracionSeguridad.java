package com.periodismo.seguro.autenticacion.internal;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.stereotype.Component;

/** HU-03: reglas de acceso, formulario de inicio de sesion y bloqueo por intentos fallidos. */
@Configuration
class ConfiguracionSeguridad {
    @Bean
    Clock reloj() { return Clock.systemUTC(); }

    @Bean
    SessionRegistry sessionRegistry() { return new SessionRegistryImpl(); }

    @Bean
    HttpSessionEventPublisher httpSessionEventPublisher() { return new HttpSessionEventPublisher(); }

    @Bean
    SecurityFilterChain filtros(HttpSecurity http, SessionRegistry registro, ControlIntentos intentos) throws Exception {
        AuthenticationSuccessHandler exito = (req, res, auth) -> {
            intentos.registrarExito(req.getParameter("correo"));
            res.sendRedirect(req.getContextPath() + "/inicio");
        };
        AuthenticationFailureHandler fallo = (req, res, e) -> res.sendRedirect(
            req.getContextPath() + (e instanceof LockedException ? "/login?bloqueado" : "/login?error"));

        http
            .authorizeHttpRequests(a -> a
                .requestMatchers("/", "/registro", "/login", "/actuator/health", "/css/**", "/js/**", "/fonts/**", "/ws")
                .permitAll()
                .anyRequest().authenticated())
            .formLogin(f -> f
                .loginPage("/login").loginProcessingUrl("/login")
                .usernameParameter("correo").passwordParameter("clave")
                .successHandler(exito).failureHandler(fallo))
            .sessionManagement(s -> s.maximumSessions(-1).sessionRegistry(registro))
            .csrf(c -> c.ignoringRequestMatchers("/ws"))
            .httpBasic(AbstractHttpConfigurer::disable);
        return http.build();
    }

    /** Cuenta un fallo por cada intento con credenciales invalidas, exista o no el correo. */
    @Component
    static class EscuchaFallos {
        private final ControlIntentos intentos;

        EscuchaFallos(ControlIntentos intentos) { this.intentos = intentos; }

        @EventListener
        void alFallar(AuthenticationFailureBadCredentialsEvent evento) {
            intentos.registrarFallo(evento.getAuthentication().getName());
        }
    }
}
