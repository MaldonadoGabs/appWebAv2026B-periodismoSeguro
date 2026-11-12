package com.periodismo.seguro.notificaciones;

/** Epica WebSockets: HU-21 notificacion en tiempo real de noticias nuevas. */
public interface NotificacionesApi {
    void avisarNoticiaNueva(String noticiaId);
}
