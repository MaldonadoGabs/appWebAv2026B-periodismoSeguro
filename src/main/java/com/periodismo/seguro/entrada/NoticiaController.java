package com.periodismo.seguro.entrada;

import com.periodismo.seguro.cuentas.CuentasApi;
import com.periodismo.seguro.noticias.ArchivoImagen;
import com.periodismo.seguro.noticias.CategoriaNoticia;
import com.periodismo.seguro.noticias.ImagenAdjunta;
import com.periodismo.seguro.noticias.NoticiaPublica;
import com.periodismo.seguro.noticias.NoticiasApi;
import com.periodismo.seguro.noticias.SolicitudNoticia;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.io.IOException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/**
 * HU-09: interfaz para redactar noticias (encabezado, subtitulo, texto e imagenes).
 * HU-16: el periodista elige una categoria del catalogo antes de guardar.
 * El periodista modifica todo en el editor antes de guardar; la edicion de noticias
 * ya publicadas corresponde a otra historia. La sesion la exige Spring Security.
 */
@Controller
class NoticiaController {
    private static final List<String> TIPOS_PERMITIDOS = List.of("image/png", "image/jpeg");
    private static final int MAXIMO_IMAGENES = 5;
    private static final long MAXIMO_BYTES = 5L * 1024 * 1024;

    private final NoticiasApi noticias;
    private final CuentasApi cuentas;
    private final SanitizadorHtml sanitizador;

    NoticiaController(NoticiasApi noticias, CuentasApi cuentas, SanitizadorHtml sanitizador) {
        this.noticias = noticias;
        this.cuentas = cuentas;
        this.sanitizador = sanitizador;
    }

    @GetMapping("/noticias/nueva")
    String formulario(Model model) {
        model.addAttribute("form", new NoticiaForm());
        model.addAttribute("categorias", noticias.categorias());
        return "noticia-nueva";
    }

    @PostMapping("/noticias/nueva")
    String crear(@Valid @ModelAttribute("form") NoticiaForm form, BindingResult errores,
                 @RequestParam(value = "imagenes", required = false) List<MultipartFile> imagenes,
                 Principal principal, Model model) {
        String cuerpo = sanitizador.limpiar(form.getCuerpo());
        // Se guarda ya limpio tambien en el formulario: si hay errores, la pagina no reinyecta HTML crudo.
        form.setCuerpo(cuerpo);
        List<MultipartFile> adjuntas = imagenes == null
            ? List.of()
            : imagenes.stream().filter(archivo -> !archivo.isEmpty()).toList();
        List<CategoriaNoticia> catalogo = noticias.categorias();

        validarImagenes(adjuntas, errores);
        validarCategoria(form.getCategoriaSlug(), catalogo, errores);
        if (!errores.hasFieldErrors("cuerpo") && cuerpo.isBlank()) {
            errores.rejectValue("cuerpo", "vacio", "Escribe el texto de la noticia");
        }
        if (errores.hasErrors()) {
            model.addAttribute("categorias", catalogo);
            return "noticia-nueva";
        }

        String cuentaId = principal.getName();
        String seudonimo = cuentas.seudonimoDe(cuentaId).orElse("Periodista");
        String id;
        try {
            id = noticias.crear(cuentaId, seudonimo,
                new SolicitudNoticia(form.getTitulo(), form.getSubtitulo(), cuerpo,
                    aAdjuntas(adjuntas), form.getCategoriaSlug()));
        } catch (IllegalArgumentException e) {
            model.addAttribute("categorias", catalogo);
            errores.reject("noticia", e.getMessage());
            return "noticia-nueva";
        }
        return "redirect:/noticias/" + id;
    }

    /** Pagina de confirmacion: muestra lo guardado tal como lo vera el lector, con el seudonimo. */
    @GetMapping("/noticias/{id}")
    String creada(@PathVariable String id, Model model) {
        NoticiaPublica noticia = noticias.obtener(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        model.addAttribute("noticia", noticia);
        return "noticia-creada";
    }

    /** Sirve la imagen desde el almacenamiento del modulo, sin exponer rutas del disco. */
    @GetMapping("/noticias/imagenes/{imagenId}")
    void imagen(@PathVariable String imagenId, HttpServletResponse respuesta) throws IOException {
        ArchivoImagen archivo = noticias.imagen(imagenId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        respuesta.setContentType(archivo.tipoContenido());
        respuesta.setHeader("X-Content-Type-Options", "nosniff");
        respuesta.setHeader("Cache-Control", "no-store");
        respuesta.setContentLength(archivo.contenido().length);
        respuesta.getOutputStream().write(archivo.contenido());
    }

    /** HU-16: solo se acepta un slug del catalogo; cualquier otro valor se rechaza en el formulario. */
    private static void validarCategoria(String slug, List<CategoriaNoticia> catalogo, BindingResult errores) {
        if (errores.hasFieldErrors("categoriaSlug")) {
            return;
        }
        boolean conocida = slug != null && catalogo.stream().anyMatch(c -> c.slug().equals(slug));
        if (!conocida) {
            errores.rejectValue("categoriaSlug", "invalida", "La categoria elegida no es valida");
        }
    }

    private static void validarImagenes(List<MultipartFile> archivos, BindingResult errores) {
        if (archivos.size() > MAXIMO_IMAGENES) {
            errores.reject("imagenes", "Se admiten hasta " + MAXIMO_IMAGENES + " imagenes");
            return;
        }
        for (MultipartFile archivo : archivos) {
            if (archivo.getSize() > MAXIMO_BYTES || !TIPOS_PERMITIDOS.contains(archivo.getContentType())) {
                errores.reject("imagenes", "Solo imagenes PNG o JPEG de hasta 5 MB");
                return;
            }
        }
    }

    private static List<ImagenAdjunta> aAdjuntas(List<MultipartFile> archivos) {
        List<ImagenAdjunta> adjuntas = new ArrayList<>();
        for (MultipartFile archivo : archivos) {
            try {
                adjuntas.add(new ImagenAdjunta(archivo.getContentType(), archivo.getBytes()));
            } catch (IOException e) {
                throw new IllegalArgumentException("No se pudo leer una imagen", e);
            }
        }
        return adjuntas;
    }
}
