package com.periodismo.seguro.entrada;

import com.periodismo.seguro.autenticacion.AutenticacionApi;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * HU-06: nueva clave desde el enlace de recuperacion. Si el token no existe, ya se uso o caduco
 * se muestra un error claro y se ofrece pedir otro enlace.
 */
@Controller
class RestablecerController {
    private final AutenticacionApi autenticacion;

    RestablecerController(AutenticacionApi autenticacion) { this.autenticacion = autenticacion; }

    @GetMapping("/restablecer")
    String formulario(@RequestParam(required = false) String token, Model model, HttpServletResponse respuesta) {
        sinReferer(respuesta);
        if (!autenticacion.tokenRecuperacionVigente(token)) {
            return invalido(model);
        }
        RestablecerForm form = new RestablecerForm();
        form.setToken(token);
        model.addAttribute("form", form);
        return "restablecer";
    }

    @PostMapping("/restablecer")
    String restablecer(@Valid @ModelAttribute("form") RestablecerForm form, BindingResult errores, Model model,
                       HttpServletResponse respuesta) {
        sinReferer(respuesta);
        if (!autenticacion.tokenRecuperacionVigente(form.getToken())) {
            return invalido(model);
        }
        if (form.getClave() != null && !form.getClave().equals(form.getConfirmacion())) {
            errores.rejectValue("confirmacion", "no.coincide", "Las contraseñas no coinciden");
        }
        if (errores.hasErrors()) {
            return "restablecer";
        }
        try {
            if (!autenticacion.restablecerClave(form.getToken(), form.getClave())) {
                return invalido(model);
            }
        } catch (IllegalArgumentException e) {
            errores.rejectValue("clave", "debil", e.getMessage());
            return "restablecer";
        }
        return "redirect:/login?restablecida";
    }

    private static String invalido(Model model) {
        model.addAttribute("invalido", true);
        return "restablecer";
    }

    /** El token viaja en la URL: se evita que salga en la cabecera Referer al seguir un enlace. */
    private static void sinReferer(HttpServletResponse respuesta) {
        respuesta.setHeader("Referrer-Policy", "no-referrer");
    }
}
