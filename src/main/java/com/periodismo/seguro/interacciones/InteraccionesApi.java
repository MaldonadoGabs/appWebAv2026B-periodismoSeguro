package com.periodismo.seguro.interacciones;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Contrato publico de reacciones y comentarios de lectores sobre noticias. */
public interface InteraccionesApi {
    /** Alterna la reaccion: repetir el mismo tipo la quita, otro tipo la sustituye. */
    void reaccionar(String cuentaId, String noticiaId, TipoReaccion tipo);

    void quitarReaccion(String cuentaId, String noticiaId);

    Map<TipoReaccion, Long> conteo(String noticiaId);

    Optional<TipoReaccion> reaccionDe(String cuentaId, String noticiaId);

    Comentario comentar(String cuentaId, String autorSeudonimo, String noticiaId, String texto);

    List<Comentario> comentarios(String noticiaId);
}
