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
 * HU-01: incluye la categoria, que se valida contra el catalogo.
 * El periodista modifica todo en el editor antes de guardar; la edicion de noticias
 * ya publicadas corresponde a otra historia. La sesion y el rol PERIODISTA los exige Spring Security.
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
        model.addAttribute("categorias", noticias.categorias());
        String cuerpo = sanitizador.limpiar(form.getCuerpo());
        // Se guarda ya limpio tambien en el formulario: si hay errores, la pagina no reinyecta HTML crudo.
        form.setCuerpo(cuerpo);
        List<MultipartFile> adjuntas = imagenes == null
            ? List.of()
            : imagenes.stream().filter(archivo -> !archivo.isEmpty()).toList();

        validarImagenes(adjuntas, errores);
        if (!errores.hasFieldErrors("cuerpo") && cuerpo.isBlank()) {
            errores.rejectValue("cuerpo", "vacio", "Escribe el texto de la noticia");
        }
        if (!errores.hasFieldErrors("categoria") && !esCategoriaValida(form.getCategoria())) {
            errores.rejectValue("categoria", "no.valida", "Elige una categoría de la lista");
        }
        if (errores.hasErrors()) {
            return "noticia-nueva";
        }

        String cuentaId = principal.getName();
        String seudonimo = cuentas.seudonimoDe(cuentaId).orElse("Periodista");
        String id;
        try {
            id = noticias.crear(cuentaId, seudonimo,
                new SolicitudNoticia(form.getTitulo(), form.getSubtitulo(), cuerpo, form.getCategoria(),
                    aAdjuntas(adjuntas)));
        } catch (IllegalArgumentException e) {
            errores.reject("imagenes", e.getMessage());
            return "noticia-nueva";
        }
        return "redirect:/noticias/" + id + "/confirmacion";
    }

    /**
     * HU-12: detalle publico de una noticia. Lo ve cualquier lector, sin sesion.
     * Muestra titulo, contenido, categoria, seudonimo y fechas de publicacion y edicion.
     * Si la noticia no existe, responde 404 con una pagina amigable.
     */
    @GetMapping("/noticias/{id}")
    String detalle(@PathVariable String id, Model model) {
        NoticiaPublica noticia = noticias.obtener(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        model.addAttribute("noticia", noticia);
        return "detalle-noticia";
    }

    /**
     * HU-09: pagina de confirmacion tras guardar. Solo el autor la ve; para los demas 404.
     * Se movio a /noticias/{id}/confirmacion para no chocar con el detalle publico de HU-12.
     */
    @GetMapping("/noticias/{id}/confirmacion")
    String creada(@PathVariable String id, Principal principal, Model model) {
        if (!noticias.esAutor(principal.getName(), id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        NoticiaPublica noticia = noticias.obtener(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        model.addAttribute("noticia", noticia);
        return "noticia-creada";
    }

    /**
     * HU-13: formulario de edicion. Carga los datos actuales de la noticia.
     * Solo el autor puede editarla; si no es el autor o la noticia no existe, 404.
     */
    @GetMapping("/noticias/{id}/editar")
    String editarForm(@PathVariable String id, Principal principal, Model model) {
        if (!noticias.esAutor(principal.getName(), id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        NoticiaPublica noticia = noticias.obtener(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        NoticiaForm form = new NoticiaForm();
        form.setTitulo(noticia.titulo());
        form.setSubtitulo(noticia.subtitulo());
        form.setCuerpo(noticia.cuerpo());
        // La categoria llega como nombre visible; hay que buscar el id del catalogo.
        String categoriaId = "";
        if (noticia.categoria() != null) {
            categoriaId = noticias.categorias().stream()
                .filter(c -> c.nombre().equals(noticia.categoria()))
                .map(CategoriaNoticia::id)
                .findFirst().orElse("");
        }
        form.setCategoria(categoriaId);
        model.addAttribute("form", form);
        model.addAttribute("noticiaId", id);
        model.addAttribute("categorias", noticias.categorias());
        return "noticia-editar";
    }

    /**
     * HU-13: guardar la edicion. Aplica las mismas validaciones que al crear.
     * Solo el autor puede editarla; registra la fecha de modificacion.
     */
    @PostMapping("/noticias/{id}/editar")
    String editarGuardar(@PathVariable String id,
                         @Valid @ModelAttribute("form") NoticiaForm form, BindingResult errores,
                         Principal principal, Model model) {
        model.addAttribute("noticiaId", id);
        model.addAttribute("categorias", noticias.categorias());
        String cuerpo = sanitizador.limpiar(form.getCuerpo());
        form.setCuerpo(cuerpo);

        if (!errores.hasFieldErrors("cuerpo") && cuerpo.isBlank()) {
            errores.rejectValue("cuerpo", "vacio", "Escribe el texto de la noticia");
        }
        if (!errores.hasFieldErrors("categoria") && !esCategoriaValida(form.getCategoria())) {
            errores.rejectValue("categoria", "no.valida", "Elige una categoría de la lista");
        }
        if (errores.hasErrors()) {
            return "noticia-editar";
        }
        boolean actualizado;
        try {
            actualizado = noticias.editar(principal.getName(), id,
                new SolicitudNoticia(form.getTitulo(), form.getSubtitulo(), cuerpo, form.getCategoria(),
                    List.of()));
        } catch (IllegalArgumentException e) {
            errores.reject("imagenes", e.getMessage());
            return "noticia-editar";
        }
        // HU-13: si la noticia no existe o no es del autor, no se revela su existencia (404).
        if (!actualizado) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return "redirect:/noticias/" + id + "/confirmacion";
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

    private boolean esCategoriaValida(String id) {
        return id != null && noticias.categorias().stream().anyMatch(c -> c.id().equals(id));
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
