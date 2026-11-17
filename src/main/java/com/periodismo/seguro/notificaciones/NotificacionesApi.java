package com.periodismo.seguro.notificaciones;

import java.util.function.Consumer;

/** Epica WebSockets: HU-21 notificacion en tiempo real de noticias nuevas. */
public interface NotificacionesApi {
    /** HU-21: entrega el aviso a todas las conexiones abiertas. */
    void avisarNoticiaNueva(AvisoNoticia aviso);

    /** Registra una conexion de la capa de entrada para recibir avisos. */
    void abrirConexion(String conexionId, Consumer<AvisoNoticia> receptor);

    /** Da de baja una conexion cerrada. */
    void cerrarConexion(String conexionId);
}
