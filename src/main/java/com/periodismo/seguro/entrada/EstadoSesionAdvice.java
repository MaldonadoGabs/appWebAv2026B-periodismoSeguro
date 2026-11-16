package com.periodismo.seguro.entrada;

import java.security.Principal;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * HU-04: indica a la cabecera comun si hay sesion iniciada, sin exponer datos del usuario.
 * HU-08: y si la sesion tiene rol PERIODISTA, para mostrar solo los enlaces de periodista.
 */
@ControllerAdvice
class EstadoSesionAdvice {
    @ModelAttribute("conSesion")
    boolean conSesion(Principal principal) { return principal != null; }

    @ModelAttribute("esPeriodista")
    boolean esPeriodista(Authentication autenticacion) {
        return autenticacion != null && autenticacion.getAuthorities().stream()
            .anyMatch(a -> "ROLE_PERIODISTA".equals(a.getAuthority()));
    }
}
