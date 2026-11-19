package com.periodismo.seguro.entrada;

import com.periodismo.seguro.noticias.CategoriaNoticia;
import com.periodismo.seguro.noticias.NoticiasApi;
import com.periodismo.seguro.noticias.ResumenNoticia;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Capa de entrada: HTTP y paginas Thymeleaf. Solo usa las APIs publicas de los modulos.
 * HU-10: la pagina principal es un feed publico y paginado de noticias.
 * HU-11: el feed soporta busqueda por texto y filtro por categoria, preservando los parametros.
 */
@Controller
class WebController {
    static final int POR_PAGINA = 10;
    static final int MAXIMO_POR_PAGINA = 50;

    private final NoticiasApi noticias;

    WebController(NoticiasApi noticias) {
        this.noticias = noticias;
    }

    @GetMapping("/")
    String home(@RequestParam(defaultValue = "1") String pagina,
                @RequestParam(defaultValue = "10") String tamano,
                @RequestParam(name = "q", required = false) String texto,
                @RequestParam(name = "categoria", required = false) String categoria,
                Model m) {
        int porPagina = normalizar(tamano, POR_PAGINA, 1, MAXIMO_POR_PAGINA);

        // HU-11: si hay texto o categoria validos, se usa la busqueda; si no, el feed normal.
        boolean hayBusqueda = tieneValor(texto) || tieneValor(categoria);
        long total;
        List<ResumenNoticia> lista;
        if (hayBusqueda) {
            total = noticias.contarBusqueda(texto, categoria);
            lista = noticias.buscar(texto, categoria, 0, Math.toIntExact(total));
        } else {
            total = noticias.contar();
            // No se pagina aqui: se hace abajo con el calculo de paginas.
            lista = List.of();
        }

        int totalPaginas = (int) Math.max(1, Math.ceil((double) total / porPagina));
        int actual = Math.min(normalizar(pagina, 1, 1, Integer.MAX_VALUE), totalPaginas);

        if (hayBusqueda) {
            lista = noticias.buscar(texto, categoria, actual - 1, porPagina);
        } else {
            lista = noticias.listar(actual - 1, porPagina);
        }

        m.addAttribute("noticias", lista);
        m.addAttribute("pagina", actual);
        m.addAttribute("tamano", porPagina);
        m.addAttribute("totalPaginas", totalPaginas);
        m.addAttribute("totalNoticias", total);
        // HU-11: preservar parametros de busqueda en la vista y la paginacion.
        m.addAttribute("q", texto == null ? "" : texto);
        m.addAttribute("categoriaSeleccionada", categoria == null ? "" : categoria);
        m.addAttribute("categorias", noticias.categorias());
        m.addAttribute("hayBusqueda", hayBusqueda);
        return "index";
    }

    private static int normalizar(String valor, int defecto, int minimo, int maximo) {
        try {
            return Math.min(Math.max(Integer.parseInt(valor), minimo), maximo);
        } catch (NumberFormatException e) {
            return defecto;
        }
    }

    private static boolean tieneValor(String s) {
        return s != null && !s.isBlank();
    }
}
