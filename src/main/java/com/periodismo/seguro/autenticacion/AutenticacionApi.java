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

    /**
     * HU-05. Si el correo tiene cuenta, genera un enlace de recuperacion de un solo uso (caduca en 30 minutos)
     * y lo envia. No devuelve nada para que la respuesta sea siempre la misma, exista o no la cuenta.
     */
    void solicitarRecuperacion(String correo);

    /** HU-06: true si el token de recuperacion existe, no se ha usado y no ha caducado. */
    boolean tokenRecuperacionVigente(String token);

    /**
     * HU-06: si el token es vigente lo invalida, guarda la nueva clave con BCrypt (via el modulo cuentas)
     * y cierra las sesiones abiertas de esa cuenta. Devuelve false si el token no existe, ya se uso o caduco.
     *
     * @throws IllegalArgumentException si la clave no cumple las reglas del registro (el token no se gasta)
     */
    boolean restablecerClave(String token, String nuevaClave);
}
