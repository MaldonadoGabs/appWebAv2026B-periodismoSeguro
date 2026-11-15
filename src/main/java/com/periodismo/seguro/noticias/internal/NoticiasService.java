package com.periodismo.seguro.noticias.internal;

import com.periodismo.seguro.noticias.ArchivoImagen;
import com.periodismo.seguro.noticias.ImagenAdjunta;
import com.periodismo.seguro.noticias.NoticiaCreada;
import com.periodismo.seguro.noticias.NoticiaPublica;
import com.periodismo.seguro.noticias.NoticiasApi;
import com.periodismo.seguro.noticias.SolicitudNoticia;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** HU-09: crear noticias e imagenes. El resto del CRUD sigue pendiente de sus historias. */
@Service
class NoticiasService implements NoticiasApi {
    static final int MAXIMO_IMAGENES = 5;
    /** Hasta que HU-16 anada la categoria real, el evento no inventa una clasificacion. */
    private static final String CATEGORIA_PENDIENTE = "sin-categoria";

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
            solicitud.titulo().trim(), solicitud.subtitulo().trim(), solicitud.cuerpo(), creadaEn));

        int orden = 0;
        for (ImagenAdjunta imagen : solicitud.imagenes()) {
            String nombreGuardado = almacen.guardar(imagen);
            imagenesNoticia.save(new ImagenNoticia(UUID.randomUUID().toString(), id, nombreGuardado,
                imagen.tipoContenido(), orden++));
        }
        eventos.publishEvent(new NoticiaCreada(id, CATEGORIA_PENDIENTE));
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
            imagenesNoticia.findByNoticiaIdOrderByOrdenAsc(id).stream().map(ImagenNoticia::getId).toList()));
    }

    @Override public List<String> listar(int pagina, int tamano) { return List.of(); }
    @Override public List<String> buscar(String texto, String categoria) { return List.of(); }
    @Override public boolean editar(String autorCuentaId, String id, String cuerpo) { return false; }
    @Override public boolean eliminar(String autorCuentaId, String id, boolean confirmado) { return false; }
    @Override public List<String> categorias() { return List.of(); }

    private static void validar(SolicitudNoticia solicitud) {
        if (solicitud == null || solicitud.titulo() == null || solicitud.titulo().isBlank()
            || solicitud.subtitulo() == null || solicitud.subtitulo().isBlank()
            || solicitud.cuerpo() == null || solicitud.cuerpo().isBlank()) {
            throw new IllegalArgumentException("Faltan el encabezado, el subtitulo o el texto");
        }
        if (solicitud.imagenes().size() > MAXIMO_IMAGENES) {
            throw new IllegalArgumentException("Se admiten hasta " + MAXIMO_IMAGENES + " imagenes");
        }
    }
}
