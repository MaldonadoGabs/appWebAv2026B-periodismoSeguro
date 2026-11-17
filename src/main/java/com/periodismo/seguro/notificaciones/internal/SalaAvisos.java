package com.periodismo.seguro.notificaciones.internal;

import com.periodismo.seguro.notificaciones.AvisoNoticia;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;

/**
 * HU-21: receptores de avisos en memoria. No guarda identidad, correo ni IP: solo un id de conexion
 * y el callback que la capa de entrada usa para escribir en el socket.
 */
@Component
class SalaAvisos {
    private final Map<String, Consumer<AvisoNoticia>> receptores = new ConcurrentHashMap<>();

    void agregar(String conexionId, Consumer<AvisoNoticia> receptor) {
        if (conexionId != null && receptor != null) {
            receptores.put(conexionId, receptor);
        }
    }

    void quitar(String conexionId) {
        if (conexionId != null) {
            receptores.remove(conexionId);
        }
    }

    void enviarAviso(AvisoNoticia aviso) {
        if (aviso == null) {
            return;
        }
        for (var entrada : receptores.entrySet()) {
            try {
                entrada.getValue().accept(aviso);
            } catch (RuntimeException e) {
                // Un receptor roto no impide que los demas reciban el aviso.
                receptores.remove(entrada.getKey());
            }
        }
    }
}
