package com.periodismo.seguro.notificaciones.internal;

import com.periodismo.seguro.noticias.NoticiaCreada;
import com.periodismo.seguro.notificaciones.AvisoNoticia;
import com.periodismo.seguro.notificaciones.NotificacionesApi;
import java.util.function.Consumer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** HU-21: reacciona a NoticiaCreada despues del commit y empuja el aviso a los conectados. */
@Service
class NotificacionesService implements NotificacionesApi {
    private final SalaAvisos sala;

    NotificacionesService(SalaAvisos sala) { this.sala = sala; }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void alCrearseNoticia(NoticiaCreada evento) {
        avisarNoticiaNueva(new AvisoNoticia(evento.noticiaId(), evento.titulo(), evento.autorSeudonimo()));
    }

    @Override
    public void avisarNoticiaNueva(AvisoNoticia aviso) {
        sala.enviarAviso(aviso);
    }

    @Override
    public void abrirConexion(String conexionId, Consumer<AvisoNoticia> receptor) {
        sala.agregar(conexionId, receptor);
    }

    @Override
    public void cerrarConexion(String conexionId) {
        sala.quitar(conexionId);
    }
}
