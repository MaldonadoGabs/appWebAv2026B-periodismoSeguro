package com.periodismo.seguro.cuentas;

import java.util.Optional;

/** Epica Registro y autenticacion: HU-01 registro de periodistas, HU-02 identidad real separada del seudonimo. */
public interface CuentasApi {
    /**
     * HU-01: crea la cuenta con la clave en hash BCrypt y un seudonimo unico generado.
     *
     * @throws CorreoYaRegistradoException si el correo ya tiene cuenta
     */
    ResultadoRegistro registrar(SolicitudRegistro solicitud);

    Optional<String> seudonimoDe(String cuentaId);

    /** HU-03: credenciales (id de cuenta y hash de la clave) del correo dado, o vacio si no existe. */
    Optional<CredencialesCuenta> credencialesPorCorreo(String correo);

    /**
     * HU-06: reemplaza la clave de la cuenta por un nuevo hash BCrypt (con sal).
     *
     * @return false si la cuenta no existe
     * @throws IllegalArgumentException si la clave no cumple {@link ReglasClave}
     */
    boolean cambiarClave(String cuentaId, String nuevaClave);
}
