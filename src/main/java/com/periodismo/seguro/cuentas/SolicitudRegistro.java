package com.periodismo.seguro.cuentas;

/** Datos de registro. El toString oculta todo para no filtrar correo, nombre real ni clave en logs (HU-26). */
public record SolicitudRegistro(String correo, String nombreReal, String clave) {
    @Override
    public String toString() { return "SolicitudRegistro[oculto]"; }
}
