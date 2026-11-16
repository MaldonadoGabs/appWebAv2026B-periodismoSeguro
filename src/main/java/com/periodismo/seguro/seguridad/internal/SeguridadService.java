package com.periodismo.seguro.seguridad.internal;

import com.periodismo.seguro.seguridad.SeguridadApi;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/** HU-25: politica de seguridad sin estado y sin dependencias: en microservicios pasa a ser libreria compartida o gateway. */
@Service
class SeguridadService implements SeguridadApi {
    private static final List<String> PUBLICAS = List.of("/", "/login", "/registro");

    /** Fail-closed: solo las rutas publicas se abren sin rol; el resto se define en HU-08. */
    @Override
    public boolean puedeAcceder(String ruta, String rol) { return PUBLICAS.contains(ruta); }

    /**
     * HU-25: cabeceras que se aplican en cada respuesta. La CSP permite blob: en img-src
     * para que el editor pueda mostrar miniaturas locales de las imagenes seleccionadas
     * antes de guardarlas. Las cabeceras reales las configura Spring Security en ConfiguracionSeguridad.
     */
    @Override
    public Map<String, String> cabecerasSeguras() {
        return Map.of(
            "X-Content-Type-Options", "nosniff",
            "X-Frame-Options", "DENY",
            "Referrer-Policy", "no-referrer",
            "Content-Security-Policy",
            "default-src 'self'; img-src 'self' blob:; script-src 'self'; "
            + "style-src 'self'; font-src 'self'; connect-src 'self'; "
            + "frame-ancestors 'none'; base-uri 'self'; form-action 'self'",
            "Strict-Transport-Security", "max-age=31536000; includeSubDomains");
    }
}
