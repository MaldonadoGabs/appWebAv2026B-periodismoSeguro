package com.periodismo.seguro.entrada;

import com.periodismo.seguro.validacion.MensajeRepo;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/** Capa de entrada: HTTP y paginas Thymeleaf. Solo usa las APIs publicas de los modulos. */
@Controller
class WebController {
    private final MensajeRepo repo;

    WebController(MensajeRepo repo) { this.repo = repo; }

    @GetMapping("/")
    String home(Model m) {
        m.addAttribute("mensajes", repo.findTop20ByOrderByIdDesc());
        m.addAttribute("total", repo.count());
        return "index";
    }
}
