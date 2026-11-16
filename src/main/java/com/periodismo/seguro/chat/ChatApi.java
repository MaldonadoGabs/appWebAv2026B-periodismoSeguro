package com.periodismo.seguro.chat;

import java.util.List;
import java.util.Optional;

/** Sala de chat en tiempo real: entrada autorizada, mensajes y participantes conectados. */
public interface ChatApi {
    /**
     * Valida la sesion HTTP. Devuelve el id de cuenta autorizado a entrar, o vacio si la sesion no es valida.
     * El rol PERIODISTA lo exige ademas Spring Security en el handshake de /ws.
     */
    Optional<String> conectar(String sesionId);

    /** Registra una conexion de la sala con su cuenta y sus callbacks. */
    void abrirConexion(String conexionId, String cuentaId, SuscripcionChat suscripcion);

    /** Retira una conexion de la sala. */
    void cerrarConexion(String conexionId);

    /** Difunde el texto a todas las conexiones abiertas. */
    void enviar(String texto);

    /** Seudonimos de quienes estan conectados ahora, sin duplicados y en orden estable. */
    List<String> usuariosConectados();
}
