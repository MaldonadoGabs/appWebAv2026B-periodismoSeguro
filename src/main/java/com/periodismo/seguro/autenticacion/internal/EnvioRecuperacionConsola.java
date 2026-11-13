package com.periodismo.seguro.autenticacion.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Proveedor por defecto (app.correo.proveedor=consola): simula el correo imprimiendo el enlace en consola.
 * Nunca se imprime el correo, el nombre real ni el id de cuenta (HU-26). No usar en produccion con datos reales.
 */
@Component
@ConditionalOnProperty(name = "app.correo.proveedor", havingValue = "consola", matchIfMissing = true)
class EnvioRecuperacionConsola implements EnvioRecuperacion {
    private static final Logger log = LoggerFactory.getLogger(EnvioRecuperacionConsola.class);

    @Override
    public void enviar(String destinatario, String enlace) {
        log.info("Correo de recuperacion simulado. Enlace: {}", enlace);
    }
}
