package com.periodismo.seguro.entrada;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Pagina de inicio de sesion. El formulario real llega con HU-03; por ahora muestra el mensaje del registro. */
@Controller
class LoginController {
    @GetMapping("/login")
    String login() { return "login"; }
}
