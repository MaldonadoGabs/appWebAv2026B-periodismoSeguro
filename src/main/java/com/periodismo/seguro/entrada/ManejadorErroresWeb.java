package com.periodismo.seguro.entrada;

import jakarta.servlet.http.HttpServletResponse;
import java.security.Principal;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;

/**
 * HU-12: las paginas de error se renderizan con plantillas propias, sin stack traces
 * ni datos internos (HU-25). Un 404 se ve amigable; el resto usa una pagina generica.
 * Los atributos de sesion se aportan aqui porque un @ControllerAdvice no hereda los
 * @ModelAttribute globales de otros advices al manejar una excepcion.
 */
@ControllerAdvice
class ManejadorErroresWeb {

    @ExceptionHandler(ResponseStatusException.class)
    String alFallar(ResponseStatusException error, HttpServletResponse respuesta, Model modelo,
                    Principal principal, Authentication autenticacion) {
        int codigo = error.getStatusCode().value();
        respuesta.setStatus(codigo);
        modelo.addAttribute("codigo", codigo);
        // Solo booleanos: nunca correo, nombre real ni identificadores en la pagina de error.
        modelo.addAttribute("conSesion", principal != null);
        modelo.addAttribute("esPeriodista", autenticacion != null && autenticacion.getAuthorities().stream()
            .anyMatch(a -> "ROLE_PERIODISTA".equals(a.getAuthority())));
        return codigo == HttpStatus.NOT_FOUND.value() ? "error/404" : "error/error";
    }
}
