package com.periodismo.seguro.notificaciones.internal;

import com.periodismo.seguro.noticias.NoticiaCreada;
import com.periodismo.seguro.notificaciones.NotificacionesApi;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

/** Reacciona al evento NoticiaCreada; no llama a Noticias. Pendiente HU-21: empujar el aviso por WebSocket. */
@Service
class NotificacionesService implements NotificacionesApi {
    @EventListener
    public void alCrearseNoticia(NoticiaCreada evento) {
        avisarNoticiaNueva(evento.noticiaId());
    }

    @Override
    public void avisarNoticiaNueva(String noticiaId) {}
}
