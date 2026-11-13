package com.periodismo.seguro.autenticacion.internal;

/**
 * HU-05: entrega del enlace de recuperacion al correo de la cuenta.
 * Implementaciones: consola (por defecto, para desarrollo) y Resend (correo real).
 * Ninguna debe registrar el destinatario en logs (HU-26).
 */
interface EnvioRecuperacion {
    void enviar(String destinatario, String enlace);
}
