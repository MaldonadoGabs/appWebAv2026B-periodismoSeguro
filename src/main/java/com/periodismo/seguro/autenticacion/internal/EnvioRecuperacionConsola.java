package com.periodismo.seguro.autenticacion.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * HU-05: simula el correo imprimiendo el enlace en consola. Nunca se imprime el correo,
 * el nombre real ni el id de cuenta (HU-26).
 */
@Component
class EnvioRecuperacionConsola implements EnvioRecuperacion {
    private static final Logger log = LoggerFactory.getLogger(EnvioRecuperacionConsola.class);

    @Override
    public void enviar(String enlace) {
        log.info("Correo de recuperacion simulado. Enlace: {}", enlace);
    }
}
