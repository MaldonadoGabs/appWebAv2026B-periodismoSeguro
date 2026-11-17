package com.periodismo.seguro.entrada;

import com.periodismo.seguro.noticias.NoticiaPublica;
import com.periodismo.seguro.noticias.NoticiasApi;
import java.security.Principal;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * HU-14: retirar una nota propia previa confirmacion. Un administrador tambien puede hacerlo.
 * La confirmacion es una pagina (no un JavaScript) para que el paso sea explicito y auditable.
 * Si otra cuenta intenta borrarla se responde 404, igual que en la edicion, para no revelar
 * que la noticia existe.
 */
@Controller
class EliminarNoticiaController {
    static final String MENSAJE = "La noticia se eliminó correctamente.";

    private final NoticiasApi noticias;

    EliminarNoticiaController(NoticiasApi noticias) { this.noticias = noticias; }

    @GetMapping("/noticias/{id}/eliminar")
    String confirmar(@PathVariable String id, Principal principal, Authentication autenticacion, Model model) {
        if (!puedeEliminar(principal, autenticacion, id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        NoticiaPublica noticia = noticias.obtener(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        model.addAttribute("noticia", noticia);
        return "noticia-eliminar";
    }

    @PostMapping("/noticias/{id}/eliminar")
    String eliminar(@PathVariable String id, Principal principal, Authentication autenticacion,
                    RedirectAttributes redireccion) {
        boolean administrador = esAdministrador(autenticacion);
        if (!noticias.eliminar(principal.getName(), id, true, administrador)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        redireccion.addFlashAttribute("mensaje", MENSAJE);
        // El periodista vuelve a "mis notas"; el administrador no tiene esa lista, vuelve al feed.
        return "redirect:" + (administrador ? "/" : "/mis-notas");
    }

    private boolean puedeEliminar(Principal principal, Authentication autenticacion, String id) {
        return principal != null && (esAdministrador(autenticacion) || noticias.esAutor(principal.getName(), id));
    }

    private static boolean esAdministrador(Authentication autenticacion) {
        return autenticacion != null && autenticacion.getAuthorities().stream()
            .anyMatch(a -> "ROLE_ADMINISTRADOR".equals(a.getAuthority()));
    }
}
