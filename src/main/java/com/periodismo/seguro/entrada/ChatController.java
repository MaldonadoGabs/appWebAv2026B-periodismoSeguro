package com.periodismo.seguro.entrada;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Pantalla de la sala de chat. El acceso lo limita Spring Security a PERIODISTA con sesion iniciada;
 * los mensajes llegan en tiempo real por el WebSocket /ws del modulo chat.
 */
@Controller
class ChatController {
    @GetMapping("/chat")
    String chat() {
        return "chat";
    }
}
