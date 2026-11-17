package com.periodismo.seguro.entrada;

import com.periodismo.seguro.cuentas.CuentasApi;
import com.periodismo.seguro.interacciones.InteraccionesApi;
import com.periodismo.seguro.interacciones.TipoReaccion;
import java.security.Principal;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

/** HU: el lector reacciona y comenta; los periodistas no tienen acceso a estos endpoints. */
@Controller
class InteraccionesController {
    private final InteraccionesApi interacciones;
    private final CuentasApi cuentas;

    InteraccionesController(InteraccionesApi interacciones, CuentasApi cuentas) {
        this.interacciones = interacciones;
        this.cuentas = cuentas;
    }

    @PostMapping("/noticias/{id}/reaccionar")
    String reaccionar(@PathVariable String id, @RequestParam String tipo, Principal principal) {
        TipoReaccion reaccion = parsear(tipo);
        interacciones.reaccionar(principal.getName(), id, reaccion);
        return "redirect:/noticias/" + id;
    }

    @PostMapping("/noticias/{id}/quitar-reaccion")
    String quitar(@PathVariable String id, Principal principal) {
        interacciones.quitarReaccion(principal.getName(), id);
        return "redirect:/noticias/" + id;
    }

    @PostMapping("/noticias/{id}/comentar")
    String comentar(@PathVariable String id, @RequestParam String texto, Principal principal) {
        String seudonimo = cuentas.seudonimoDe(principal.getName()).orElse("Lector");
        try {
            interacciones.comentar(principal.getName(), seudonimo, id, texto);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
        return "redirect:/noticias/" + id;
    }

    private static TipoReaccion parsear(String tipo) {
        try {
            return TipoReaccion.valueOf(tipo);
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reacción no válida");
        }
    }
}
