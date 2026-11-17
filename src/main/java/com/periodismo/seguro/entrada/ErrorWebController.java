package com.periodismo.seguro.entrada;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * HU-12: pagina 404 amigable cuando una noticia no existe.
 * HU-25: sin exponer stack traces ni datos internos en las paginas de error.
 */
@Controller
class ErrorWebController implements ErrorController {

    @GetMapping("/error")
    String error(HttpServletRequest request) {
        Object codigo = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        if (codigo != null && Integer.parseInt(codigo.toString()) == HttpStatus.NOT_FOUND.value()) {
            return "error/404";
        }
        return "error/error";
    }
}
