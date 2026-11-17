package com.periodismo.seguro.interacciones.internal;

import com.periodismo.seguro.interacciones.Comentario;
import com.periodismo.seguro.interacciones.InteraccionesApi;
import com.periodismo.seguro.interacciones.TipoReaccion;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class InteraccionesService implements InteraccionesApi {
    static final int MAXIMO_COMENTARIO = 500;
    private static final DateTimeFormatter HORA =
        DateTimeFormatter.ofPattern("dd/MM HH:mm 'UTC'").withZone(ZoneOffset.UTC);

    private final ReaccionNoticiaRepository reacciones;
    private final ComentarioNoticiaRepository comentarios;
    private final Clock reloj;

    InteraccionesService(ReaccionNoticiaRepository reacciones,
                         ComentarioNoticiaRepository comentarios, Clock reloj) {
        this.reacciones = reacciones;
        this.comentarios = comentarios;
        this.reloj = reloj;
    }

    @Override
    @Transactional
    public void reaccionar(String cuentaId, String noticiaId, TipoReaccion tipo) {
        if (cuentaId == null || noticiaId == null || tipo == null) {
            return;
        }
        String id = clave(cuentaId, noticiaId);
        Optional<ReaccionNoticia> actual = reacciones.findById(id);
        if (actual.isPresent() && actual.get().getTipo() == tipo) {
            reacciones.deleteById(id);
            return;
        }
        reacciones.save(new ReaccionNoticia(id, cuentaId, noticiaId, tipo));
    }

    @Override
    @Transactional
    public void quitarReaccion(String cuentaId, String noticiaId) {
        if (cuentaId == null || noticiaId == null) {
            return;
        }
        String id = clave(cuentaId, noticiaId);
        if (reacciones.existsById(id)) {
            reacciones.deleteById(id);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Map<TipoReaccion, Long> conteo(String noticiaId) {
        Map<TipoReaccion, Long> totales = new EnumMap<>(TipoReaccion.class);
        for (TipoReaccion tipo : TipoReaccion.values()) {
            totales.put(tipo, 0L);
        }
        if (noticiaId == null) {
            return totales;
        }
        for (ReaccionNoticia reaccion : reacciones.findByNoticiaId(noticiaId)) {
            totales.merge(reaccion.getTipo(), 1L, Long::sum);
        }
        return totales;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TipoReaccion> reaccionDe(String cuentaId, String noticiaId) {
        if (cuentaId == null || noticiaId == null) {
            return Optional.empty();
        }
        return reacciones.findById(clave(cuentaId, noticiaId)).map(ReaccionNoticia::getTipo);
    }

    @Override
    @Transactional
    public Comentario comentar(String cuentaId, String autorSeudonimo, String noticiaId, String texto) {
        if (cuentaId == null || noticiaId == null || texto == null || texto.isBlank()) {
            throw new IllegalArgumentException("El comentario no puede estar vacío");
        }
        String limpio = texto.trim();
        if (limpio.length() > MAXIMO_COMENTARIO) {
            throw new IllegalArgumentException("El comentario admite hasta 500 caracteres");
        }
        Instant creadaEn = reloj.instant().truncatedTo(ChronoUnit.MINUTES);
        String id = UUID.randomUUID().toString();
        String seudonimo = autorSeudonimo == null || autorSeudonimo.isBlank() ? "Lector" : autorSeudonimo;
        comentarios.save(new ComentarioNoticia(id, noticiaId, cuentaId, seudonimo, limpio, creadaEn));
        return new Comentario(id, noticiaId, seudonimo, limpio, HORA.format(creadaEn));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Comentario> comentarios(String noticiaId) {
        if (noticiaId == null) {
            return List.of();
        }
        return comentarios.findByNoticiaIdOrderByCreadaEnAscIdAsc(noticiaId).stream()
            .map(c -> new Comentario(c.getId(), c.getNoticiaId(), c.getAutorSeudonimo(), c.getTexto(),
                HORA.format(c.getCreadaEn())))
            .toList();
    }

    private static String clave(String cuentaId, String noticiaId) {
        return cuentaId + "|" + noticiaId;
    }
}
