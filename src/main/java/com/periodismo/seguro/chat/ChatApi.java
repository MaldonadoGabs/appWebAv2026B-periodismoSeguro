package com.periodismo.seguro.chat;

import java.util.Optional;
import java.util.function.Consumer;

/** Sala de chat en tiempo real: entrada autorizada y difusion de mensajes a los participantes. */
public interface ChatApi {
    /**
     * Valida la sesion HTTP. Devuelve el id de cuenta autorizado a entrar, o vacio si la sesion no es valida.
     * El rol PERIODISTA lo exige ademas Spring Security en el handshake de /ws.
     */
    Optional<String> conectar(String sesionId);

    /** Registra una conexion de la sala junto con su receptor de mensajes. */
    void abrirConexion(String conexionId, Consumer<String> receptor);

    /** Retira una conexion de la sala. */
    void cerrarConexion(String conexionId);

    /** Difunde el texto a todas las conexiones abiertas. */
    void enviar(String texto);
}
