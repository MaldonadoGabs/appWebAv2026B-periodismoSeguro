package com.periodismo.seguro.autenticacion;

import java.util.Optional;

/**
 * Epica Registro y autenticacion: HU-03 inicio de sesion, HU-04 cierre, HU-05 y HU-06 recuperacion de clave.
 * Dueno de las sesiones: los demas modulos solo preguntan si una sesion es valida (HU-17, HU-08).
 */
public interface AutenticacionApi {
    /**
     * HU-03. Verifica correo y clave (BCrypt) y devuelve el id de cuenta, o vacio si falla (sin distinguir la causa).
     * La sesion HTTP (cookie) la crea el formulario de /login; {@link #validarSesion} la consulta despues.
     */
    Optional<String> verificarCredenciales(String correo, String clave);

    /** HU-04. */
    void cerrarSesion(String sesionId);

    /** Devuelve el id de cuenta dueno de la sesion, o vacio si no es valida (fail-closed). */
    Optional<String> validarSesion(String sesionId);

    /** HU-05. Respuesta siempre generica, exista o no la cuenta. */
    void solicitarRecuperacion(String seudonimo);

    /** HU-06. */
    boolean restablecerClave(String token, String nuevaClave);
}
