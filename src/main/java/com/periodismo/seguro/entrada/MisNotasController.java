package com.periodismo.seguro.entrada;

import com.periodismo.seguro.noticias.NoticiasApi;
import java.security.Principal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * HU-01 / HU-08: "Mis notas". La lista sale siempre del id de la sesion (principal), nunca de un parametro,
 * asi un periodista no puede pedir las notas de otro. Solo usa la API publica de noticias.
 */
@Controller
class MisNotasController {
    static final int POR_PAGINA = 10;

    private final NoticiasApi noticias;

    MisNotasController(NoticiasApi noticias) { this.noticias = noticias; }

    @GetMapping("/mis-notas")
    String misNotas(@RequestParam(defaultValue = "1") String pagina, Principal principal, Model m) {
        String cuentaId = principal.getName();
        long total = noticias.contarDeAutor(cuentaId);
        int totalPaginas = (int) Math.max(1, Math.ceil((double) total / POR_PAGINA));
        // Mismo criterio que el feed: un valor invalido se normaliza y una pagina fuera de rango se ajusta.
        int actual = Math.min(normalizar(pagina), totalPaginas);

        m.addAttribute("notas", noticias.listarDeAutor(cuentaId, actual - 1, POR_PAGINA));
        m.addAttribute("pagina", actual);
        m.addAttribute("totalPaginas", totalPaginas);
        m.addAttribute("total", total);
        return "mis-notas";
    }

    private static int normalizar(String valor) {
        try {
            return Math.max(Integer.parseInt(valor), 1);
        } catch (NumberFormatException e) {
            return 1;
        }
    }
}
