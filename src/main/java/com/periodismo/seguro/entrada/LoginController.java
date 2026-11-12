package com.periodismo.seguro.entrada;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Pagina de inicio de sesion (HU-03). El envio lo procesa Spring Security en POST /login. */
@Controller
class LoginController {
    @GetMapping("/login")
    String login() { return "login"; }
}
