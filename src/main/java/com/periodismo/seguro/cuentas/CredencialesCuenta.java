package com.periodismo.seguro.cuentas;

/**
 * Lo que el modulo autenticacion necesita para verificar un inicio de sesion: id de cuenta y hash de la clave.
 * No incluye correo ni nombre real.
 */
public record CredencialesCuenta(String cuentaId, String claveHash) {
    @Override
    public String toString() { return "CredencialesCuenta[cuentaId=" + cuentaId + "]"; }
}
