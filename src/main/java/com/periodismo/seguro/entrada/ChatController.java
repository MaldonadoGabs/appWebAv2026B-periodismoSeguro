package com.periodismo.seguro.entrada;

import com.periodismo.seguro.cuentas.CuentasApi;
import java.security.Principal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Pantalla de la sala de chat. El acceso lo limita Spring Security a PERIODISTA con sesion iniciada;
 * los mensajes llegan en tiempo real por el WebSocket /ws del modulo chat.
 */
@Controller
class ChatController {
    private final CuentasApi cuentas;

    ChatController(CuentasApi cuentas) { this.cuentas = cuentas; }

    @GetMapping("/chat")
    String chat(Principal principal, Model model) {
        // El cliente necesita solo su propio seudonimo para alinear sus burbujas; nunca su cuenta.
        model.addAttribute("yo", cuentas.seudonimoDe(principal.getName()).orElse("Periodista"));
        return "chat";
    }
}
