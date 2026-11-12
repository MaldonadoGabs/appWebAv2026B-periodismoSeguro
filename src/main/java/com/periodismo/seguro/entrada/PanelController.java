package com.periodismo.seguro.entrada;

import com.periodismo.seguro.cuentas.CuentasApi;
import java.security.Principal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/** HU-03: pantalla principal protegida. El principal es el id de cuenta; solo se muestra el seudonimo. */
@Controller
class PanelController {
    private final CuentasApi cuentas;

    PanelController(CuentasApi cuentas) { this.cuentas = cuentas; }

    @GetMapping("/inicio")
    String inicio(Principal principal, Model model) {
        model.addAttribute("seudonimo", cuentas.seudonimoDe(principal.getName()).orElse("Periodista"));
        return "panel";
    }
}
