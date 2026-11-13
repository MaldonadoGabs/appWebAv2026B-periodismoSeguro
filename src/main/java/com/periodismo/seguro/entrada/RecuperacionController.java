package com.periodismo.seguro.entrada;

import com.periodismo.seguro.autenticacion.AutenticacionApi;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * HU-05: solicitud de recuperacion de clave. La respuesta es la misma exista o no la cuenta,
 * para no permitir la enumeracion de usuarios.
 */
@Controller
class RecuperacionController {
    private final AutenticacionApi autenticacion;

    RecuperacionController(AutenticacionApi autenticacion) { this.autenticacion = autenticacion; }

    @GetMapping("/recuperar")
    String formulario(Model model) {
        model.addAttribute("form", new RecuperacionForm());
        return "recuperar";
    }

    @PostMapping("/recuperar")
    String solicitar(@Valid @ModelAttribute("form") RecuperacionForm form, BindingResult errores) {
        if (errores.hasErrors()) {
            return "recuperar";
        }
        autenticacion.solicitarRecuperacion(form.getCorreo());
        return "redirect:/recuperar?enviado";
    }
}
