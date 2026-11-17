package com.periodismo.seguro.cuentas;

/** Datos de registro. El toString oculta todo para no filtrar correo, nombre real ni clave en logs (HU-26). */
public record SolicitudRegistro(String correo, String nombreReal, String clave, Rol rol) {
    /** Las llamadas internas sin rol explícito siguen creando periodistas. */
    public SolicitudRegistro(String correo, String nombreReal, String clave) {
        this(correo, nombreReal, clave, Rol.PERIODISTA);
    }

    @Override
    public String toString() { return "SolicitudRegistro[oculto]"; }
}
