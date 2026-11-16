package com.periodismo.seguro.noticias.internal;

import com.periodismo.seguro.noticias.ArchivoImagen;
import com.periodismo.seguro.noticias.CategoriaNoticia;
import com.periodismo.seguro.noticias.ImagenAdjunta;
import com.periodismo.seguro.noticias.NoticiaCreada;
import com.periodismo.seguro.noticias.NoticiaPublica;
import com.periodismo.seguro.noticias.NoticiasApi;
import com.periodismo.seguro.noticias.ResumenNoticia;
import com.periodismo.seguro.noticias.SolicitudNoticia;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** HU-09: crear noticias e imagenes. HU-10: feed paginado. HU-01: categoria y "mis notas". El resto del CRUD sigue pendiente. */
@Service
class NoticiasService implements NoticiasApi {
    static final int MAXIMO_IMAGENES = 5;
    static final int TAMANO_MAXIMO_PAGINA = 50;
    /** Solo para noticias creadas sin categoria (llamadas anteriores a HU-01): el evento no inventa una clasificacion. */
    private static final String SIN_CATEGORIA = "sin-categoria";
    /** La hora ya viene truncada a 1 h (HU-26); se muestra en UTC para no inventar la zona del lector. */
    private static final DateTimeFormatter FECHA =
        DateTimeFormatter.ofPattern("dd/MM/yyyy '·' HH:mm 'UTC'").withZone(ZoneOffset.UTC);

    private final NoticiaRepository noticias;
    private final ImagenNoticiaRepository imagenesNoticia;
    private final AlmacenImagenes almacen;
    private final ApplicationEventPublisher eventos;
    private final Clock reloj;

    NoticiasService(NoticiaRepository noticias, ImagenNoticiaRepository imagenesNoticia, AlmacenImagenes almacen,
                    ApplicationEventPublisher eventos, Clock reloj) {
        this.noticias = noticias;
        this.imagenesNoticia = imagenesNoticia;
        this.almacen = almacen;
        this.eventos = eventos;
        this.reloj = reloj;
    }

    @Override
    @Transactional
    public String crear(String autorCuentaId, String autorSeudonimo, SolicitudNoticia solicitud) {
        validar(solicitud);
        String id = UUID.randomUUID().toString();
        // HU-26: solo se conserva la hora, nunca la marca de tiempo exacta.
        Instant creadaEn = reloj.instant().truncatedTo(ChronoUnit.HOURS);
        noticias.save(new Noticia(id, autorCuentaId, autorSeudonimo,
            solicitud.titulo().trim(), solicitud.subtitulo().trim(), solicitud.cuerpo(),
            solicitud.categoria(), creadaEn));

        int orden = 0;
        for (ImagenAdjunta imagen : solicitud.imagenes()) {
            String nombreGuardado = almacen.guardar(imagen);
            imagenesNoticia.save(new ImagenNoticia(UUID.randomUUID().toString(), id, nombreGuardado,
                imagen.tipoContenido(), orden++));
        }
        String categoria = solicitud.categoria() == null ? SIN_CATEGORIA : solicitud.categoria();
        eventos.publishEvent(new NoticiaCreada(id, categoria));
        return id;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ArchivoImagen> imagen(String imagenId) {
        if (imagenId == null) {
            return Optional.empty();
        }
        return imagenesNoticia.findById(imagenId)
            .flatMap(i -> almacen.leer(i.getNombreGuardado())
                .map(bytes -> new ArchivoImagen(i.getTipoContenido(), bytes)));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<NoticiaPublica> obtener(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return noticias.findById(id).map(n -> new NoticiaPublica(
            n.getId(), n.getTitulo(), n.getSubtitulo(), n.getCuerpo(), n.getAutorSeudonimo(),
            imagenesNoticia.findByNoticiaIdOrderByOrdenAsc(id).stream().map(ImagenNoticia::getId).toList(),
            CatalogoCategorias.nombreDe(n.getCategoria()),
            FECHA.format(n.getCreadaEn()),
            n.getModificadaEn() == null ? null : FECHA.format(n.getModificadaEn())));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean esAutor(String autorCuentaId, String id) {
        if (autorCuentaId == null || id == null) {
            return false;
        }
        return noticias.findById(id).map(n -> autorCuentaId.equals(n.getAutorCuentaId())).orElse(false);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResumenNoticia> listar(int pagina, int tamano) {
        return noticias.findAllByOrderByCreadaEnDescIdAsc(paginar(pagina, tamano))
            .stream().map(this::resumen).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long contar() { return noticias.count(); }

    @Override
    @Transactional(readOnly = true)
    public List<ResumenNoticia> listarDeAutor(String autorCuentaId, int pagina, int tamano) {
        if (autorCuentaId == null) {
            return List.of();
        }
        return noticias.findAllByAutorCuentaIdOrderByCreadaEnDescIdAsc(autorCuentaId, paginar(pagina, tamano))
            .stream().map(this::resumen).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long contarDeAutor(String autorCuentaId) {
        return autorCuentaId == null ? 0 : noticias.countByAutorCuentaId(autorCuentaId);
    }

    private static PageRequest paginar(int pagina, int tamano) {
        int paginaSegura = Math.max(0, pagina);
        int tamanoSeguro = Math.min(Math.max(1, tamano), TAMANO_MAXIMO_PAGINA);
        return PageRequest.of(paginaSegura, tamanoSeguro);
    }

    private ResumenNoticia resumen(Noticia n) {
        // Si el disco efimero perdio el archivo, la tarjeta se muestra sin imagen y no se rompe.
        String imagenId = imagenesNoticia.findFirstByNoticiaIdOrderByOrdenAsc(n.getId())
            .filter(i -> almacen.existe(i.getNombreGuardado()))
            .map(ImagenNoticia::getId)
            .orElse(null);
        return new ResumenNoticia(n.getId(), n.getTitulo(), n.getSubtitulo(), Extracto.de(n.getCuerpo()),
            n.getAutorSeudonimo(), FECHA.format(n.getCreadaEn()), imagenId,
            CatalogoCategorias.nombreDe(n.getCategoria()));
    }

    /** HU-11: busqueda por texto (encabezado o cuerpo) y filtro por categoria, paginada. */
    @Override
    @Transactional(readOnly = true)
    public List<ResumenNoticia> buscar(String texto, String categoria, int pagina, int tamano) {
        String textoBusqueda = normalizarTexto(texto);
        String categoriaBusqueda = normalizarCategoria(categoria);
        return noticias.buscar(textoBusqueda, categoriaBusqueda, paginar(pagina, tamano))
            .stream().map(this::resumen).toList();
    }

    /** HU-11: total de resultados de una busqueda. */
    @Override
    @Transactional(readOnly = true)
    public long contarBusqueda(String texto, String categoria) {
        String textoBusqueda = normalizarTexto(texto);
        String categoriaBusqueda = normalizarCategoria(categoria);
        return noticias.contarBusqueda(textoBusqueda, categoriaBusqueda);
    }

    /** HU-13: el autor actualiza su noticia. Registra la fecha de modificacion. */
    @Override
    @Transactional
    public boolean editar(String autorCuentaId, String id, SolicitudNoticia solicitud) {
        if (autorCuentaId == null || id == null || solicitud == null) {
            return false;
        }
        Noticia noticia = noticias.findById(id).orElse(null);
        if (noticia == null || !autorCuentaId.equals(noticia.getAutorCuentaId())) {
            return false;
        }
        validar(solicitud);
        noticia.setTitulo(solicitud.titulo().trim());
        noticia.setSubtitulo(solicitud.subtitulo().trim());
        noticia.setCuerpo(solicitud.cuerpo());
        noticia.setCategoria(solicitud.categoria());
        // HU-26: hora redondeada a 1 h.
        noticia.setModificadaEn(reloj.instant().truncatedTo(ChronoUnit.HOURS));
        noticias.save(noticia);
        return true;
    }

    @Override public boolean eliminar(String autorCuentaId, String id, boolean confirmado) { return false; }

    @Override
    public List<CategoriaNoticia> categorias() { return CatalogoCategorias.TODAS; }

    private static void validar(SolicitudNoticia solicitud) {
        if (solicitud == null || solicitud.titulo() == null || solicitud.titulo().isBlank()
            || solicitud.subtitulo() == null || solicitud.subtitulo().isBlank()
            || solicitud.cuerpo() == null || solicitud.cuerpo().isBlank()) {
            throw new IllegalArgumentException("Faltan el encabezado, el subtitulo o el texto");
        }
        if (solicitud.categoria() != null && CatalogoCategorias.buscar(solicitud.categoria()).isEmpty()) {
            throw new IllegalArgumentException("La categoria no existe");
        }
        if (solicitud.imagenes().size() > MAXIMO_IMAGENES) {
            throw new IllegalArgumentException("Se admiten hasta " + MAXIMO_IMAGENES + " imagenes");
        }
    }

    /** HU-11: texto vacio o en blanco significa "sin filtro de texto". */
    private static String normalizarTexto(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }

    /** HU-11: categoria vacia o que no existe en el catalogo significa "sin filtro de categoria". */
    private static String normalizarCategoria(String categoria) {
        if (categoria == null || categoria.isBlank()) {
            return null;
        }
        String slug = categoria.trim();
        return CatalogoCategorias.buscar(slug).isPresent() ? slug : null;
    }
}
