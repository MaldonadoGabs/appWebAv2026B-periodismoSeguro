package com.periodismo.seguro.entrada;

import com.periodismo.seguro.cuentas.CorreoYaRegistradoException;
import com.periodismo.seguro.cuentas.CuentasApi;
import com.periodismo.seguro.cuentas.ResultadoRegistro;
import com.periodismo.seguro.cuentas.SolicitudRegistro;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** HU-01: registro de periodistas. Solo usa la API publica del modulo cuentas. */
@Controller
class RegistroController {
    private final CuentasApi cuentas;

    RegistroController(CuentasApi cuentas) { this.cuentas = cuentas; }

    @GetMapping("/registro")
    String formulario(Model model) {
        model.addAttribute("form", new RegistroForm());
        return "registro";
    }

    @PostMapping("/registro")
    String registrar(@Valid @ModelAttribute("form") RegistroForm form, BindingResult errores, RedirectAttributes flash) {
        if (form.getClave() != null && !form.getClave().equals(form.getConfirmacion())) {
            errores.rejectValue("confirmacion", "no.coincide", "Las contraseñas no coinciden");
        }
        if (errores.hasErrors()) {
            return "registro";
        }
        try {
            ResultadoRegistro resultado = cuentas.registrar(
                new SolicitudRegistro(form.getCorreo(), form.getNombreReal(), form.getClave()));
            flash.addFlashAttribute("mensaje",
                "Registro exitoso. Tu seudónimo es " + resultado.seudonimo() + ". Ya puedes iniciar sesión.");
            return "redirect:/login";
        } catch (CorreoYaRegistradoException e) {
            errores.rejectValue("correo", "duplicado", "Este correo ya está registrado");
            return "registro";
        }
    }
}
