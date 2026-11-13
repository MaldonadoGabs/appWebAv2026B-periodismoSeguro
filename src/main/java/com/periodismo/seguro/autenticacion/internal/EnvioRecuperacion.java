package com.periodismo.seguro.autenticacion.internal;

/** HU-05: entrega del enlace de recuperacion. Hoy se simula; mas adelante sera un correo real. */
interface EnvioRecuperacion {
    void enviar(String enlace);
}
