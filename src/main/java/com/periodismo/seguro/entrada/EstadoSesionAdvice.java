package com.periodismo.seguro.entrada;

import java.security.Principal;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** HU-04: indica a la cabecera comun si hay sesion iniciada, sin exponer datos del usuario. */
@ControllerAdvice
class EstadoSesionAdvice {
    @ModelAttribute("conSesion")
    boolean conSesion(Principal principal) { return principal != null; }
}
