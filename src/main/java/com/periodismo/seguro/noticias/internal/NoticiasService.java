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

/** HU-09: crear noticias e imagenes. HU-10: feed paginado. HU-01: categoria obligatoria leida de la tabla. El resto del CRUD sigue pendiente. */
@Service
class NoticiasService implements NoticiasApi {
    static final int MAXIMO_IMAGENES = 5;
    static final int TAMANO_MAXIMO_PAGINA = 50;
    /** La hora ya viene truncada a 1 h (HU-26); se muestra en UTC para no inventar la zona del lector. */
    private static final DateTimeFormatter FECHA =
        DateTimeFormatter.ofPattern("dd/MM/yyyy '·' HH:mm 'UTC'").withZone(ZoneOffset.UTC);

    private final NoticiaRepository noticias;
    private final ImagenNoticiaRepository imagenesNoticia;
    private final CatalogoCategorias catalogo;
    private final AlmacenImagenes almacen;
    private final ApplicationEventPublisher eventos;
    private final Clock reloj;

    NoticiasService(NoticiaRepository noticias, ImagenNoticiaRepository imagenesNoticia,
                    CatalogoCategorias catalogo, AlmacenImagenes almacen,
                    ApplicationEventPublisher eventos, Clock reloj) {
        this.noticias = noticias;
        this.imagenesNoticia = imagenesNoticia;
        this.catalogo = catalogo;
        this.almacen = almacen;
        this.eventos = eventos;
        this.reloj = reloj;
    }

    @Override
    @Transactional
    public String crear(String autorCuentaId, String autorSeudonimo, SolicitudNoticia solicitud) {
        validar(solicitud);
        String id = UUID.randomUUID().toString();
        String categoria = solicitud.categoria().trim();
        // HU-26: solo se conserva la hora, nunca la marca de tiempo exacta.
        Instant creadaEn = reloj.instant().truncatedTo(ChronoUnit.HOURS);
        noticias.save(new Noticia(id, autorCuentaId, autorSeudonimo,
            solicitud.titulo().trim(), solicitud.subtitulo().trim(), solicitud.cuerpo(),
            categoria, creadaEn));

        int orden = 0;
        for (ImagenAdjunta imagen : solicitud.imagenes()) {
            String nombreGuardado = almacen.guardar(imagen);
            imagenesNoticia.save(new ImagenNoticia(UUID.randomUUID().toString(), id, nombreGuardado,
                imagen.tipoContenido(), orden++));
        }
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
            catalogo.nombreDe(n.getCategoria())));
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
            catalogo.nombreDe(n.getCategoria()));
    }

    @Override public List<String> buscar(String texto, String categoria) { return List.of(); }
    @Override public boolean editar(String autorCuentaId, String id, String cuerpo) { return false; }
    @Override public boolean eliminar(String autorCuentaId, String id, boolean confirmado) { return false; }

    @Override
    public List<CategoriaNoticia> categorias() { return catalogo.todas(); }

    private void validar(SolicitudNoticia solicitud) {
        if (solicitud == null || solicitud.titulo() == null || solicitud.titulo().isBlank()
            || solicitud.subtitulo() == null || solicitud.subtitulo().isBlank()
            || solicitud.cuerpo() == null || solicitud.cuerpo().isBlank()) {
            throw new IllegalArgumentException("Faltan el encabezado, el subtitulo o el texto");
        }
        if (solicitud.categoria() == null || solicitud.categoria().isBlank()
            || !catalogo.existe(solicitud.categoria().trim())) {
            throw new IllegalArgumentException("La categoria no existe");
        }
        if (solicitud.imagenes().size() > MAXIMO_IMAGENES) {
            throw new IllegalArgumentException("Se admiten hasta " + MAXIMO_IMAGENES + " imagenes");
        }
    }
}
