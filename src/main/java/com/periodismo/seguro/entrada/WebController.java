package com.periodismo.seguro.entrada;

import com.periodismo.seguro.noticias.NoticiasApi;
import com.periodismo.seguro.validacion.MensajeRepo;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Capa de entrada: HTTP y paginas Thymeleaf. Solo usa las APIs publicas de los modulos.
 * HU-10: la pagina principal es un feed publico y paginado de noticias.
 */
@Controller
class WebController {
    static final int POR_PAGINA = 10;
    static final int MAXIMO_POR_PAGINA = 50;

    private final MensajeRepo repo;
    private final NoticiasApi noticias;

    WebController(MensajeRepo repo, NoticiasApi noticias) {
        this.repo = repo;
        this.noticias = noticias;
    }

    @GetMapping("/")
    String home(@RequestParam(defaultValue = "1") String pagina,
                @RequestParam(defaultValue = "10") String tamano, Model m) {
        int porPagina = normalizar(tamano, POR_PAGINA, 1, MAXIMO_POR_PAGINA);
        long total = noticias.contar();
        int totalPaginas = (int) Math.max(1, Math.ceil((double) total / porPagina));
        // Parametros invalidos se normalizan: nunca un 400 ni un 500, y nunca una pagina fuera de rango.
        int actual = Math.min(normalizar(pagina, 1, 1, Integer.MAX_VALUE), totalPaginas);

        m.addAttribute("noticias", noticias.listar(actual - 1, porPagina));
        m.addAttribute("pagina", actual);
        m.addAttribute("tamano", porPagina);
        m.addAttribute("totalPaginas", totalPaginas);
        m.addAttribute("totalNoticias", total);
        // Chat de prueba del stack, conservado al final de la pagina.
        m.addAttribute("mensajes", repo.findTop20ByOrderByIdDesc());
        m.addAttribute("total", repo.count());
        return "index";
    }

    private static int normalizar(String valor, int defecto, int minimo, int maximo) {
        try {
            return Math.min(Math.max(Integer.parseInt(valor), minimo), maximo);
        } catch (NumberFormatException e) {
            return defecto;
        }
    }
}
