package com.periodismo.seguro.entrada;

import com.periodismo.seguro.cuentas.CorreoYaRegistradoException;
import com.periodismo.seguro.cuentas.CuentasApi;
import com.periodismo.seguro.cuentas.ResultadoRegistro;
import com.periodismo.seguro.cuentas.Rol;
import com.periodismo.seguro.cuentas.SolicitudRegistro;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Registro publico: elige entre LECTOR y PERIODISTA; nunca se puede pedir ADMINISTRADOR. */
@Controller
class RegistroController {
    private final CuentasApi cuentas;

    RegistroController(CuentasApi cuentas) { this.cuentas = cuentas; }

    @GetMapping("/registro")
    String formulario(Model model) {
        RegistroForm form = new RegistroForm();
        form.setRol("PERIODISTA");
        model.addAttribute("form", form);
        return "registro";
    }

    @PostMapping("/registro")
    String registrar(@Valid @ModelAttribute("form") RegistroForm form, BindingResult errores, RedirectAttributes flash) {
        if (form.getClave() != null && !form.getClave().equals(form.getConfirmacion())) {
            errores.rejectValue("confirmacion", "no.coincide", "Las contraseñas no coinciden");
        }
        String rol = form.getRol();
        if (!"LECTOR".equalsIgnoreCase(rol) && !"PERIODISTA".equalsIgnoreCase(rol)) {
            errores.rejectValue("rol", "no.valido", "Elige lector o periodista");
        }
        if (errores.hasErrors()) {
            return "registro";
        }
        try {
            ResultadoRegistro resultado = cuentas.registrar(new SolicitudRegistro(
                form.getCorreo(), form.getNombreReal(), form.getClave(), Rol.valueOf(rol.toUpperCase())));
            flash.addFlashAttribute("mensaje",
                "Registro exitoso. Tu seudónimo es " + resultado.seudonimo() + ". Ya puedes iniciar sesión.");
            return "redirect:/login";
        } catch (CorreoYaRegistradoException e) {
            errores.rejectValue("correo", "duplicado", "Este correo ya está registrado");
            return "registro";
        }
    }
}
