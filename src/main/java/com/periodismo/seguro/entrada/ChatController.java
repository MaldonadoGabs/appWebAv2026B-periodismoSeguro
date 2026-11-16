package com.periodismo.seguro.entrada;

import com.periodismo.seguro.validacion.MensajeRepo;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * HU-08 / WebSockets: pantalla del chat para periodistas. Reutiliza el WebSocket /ws y chat.js;
 * el acceso lo limita ConfiguracionSeguridad a PERIODISTA.
 */
@Controller
class ChatController {
    private final MensajeRepo repo;

    ChatController(MensajeRepo repo) { this.repo = repo; }

    @GetMapping("/chat")
    String chat(Model m) {
        m.addAttribute("mensajes", repo.findTop20ByOrderByIdDesc());
        return "chat";
    }
}
