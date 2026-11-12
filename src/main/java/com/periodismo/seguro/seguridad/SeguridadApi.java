package com.periodismo.seguro.seguridad;

import java.util.Map;

/**
 * Epica Seguridad y privacidad: HU-08 proteccion de rutas y roles, HU-25 cabeceras y sesiones seguras,
 * HU-26 no registrar datos que identifiquen al periodista (ni IP ni identificadores en logs).
 */
public interface SeguridadApi {
    /** HU-08. */
    boolean puedeAcceder(String ruta, String rol);

    /** HU-25. */
    Map<String, String> cabecerasSeguras();
}
