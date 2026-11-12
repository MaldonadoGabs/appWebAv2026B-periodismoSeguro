package com.periodismo.seguro.cuentas.internal;

import java.security.SecureRandom;
import org.springframework.stereotype.Component;

/** Genera seudonimos con el formato "Cóndor Silente 482". La unicidad la garantiza el servicio. */
@Component
class GeneradorSeudonimo {
    private static final String[] ANIMALES = {
        "Cóndor", "Jaguar", "Colibrí", "Lobo", "Halcón", "Búho", "Zorro", "Pantera", "Tucán", "Delfín"
    };
    private static final String[] ADJETIVOS = {
        "Silente", "Sereno", "Veloz", "Astuto", "Valiente", "Nocturno", "Errante", "Sigiloso", "Firme", "Lúcido"
    };
    private final SecureRandom azar = new SecureRandom();

    String nuevo() {
        return ANIMALES[azar.nextInt(ANIMALES.length)] + " "
            + ADJETIVOS[azar.nextInt(ADJETIVOS.length)] + " "
            + (100 + azar.nextInt(900));
    }
}
