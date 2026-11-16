package com.periodismo.seguro.chat.internal;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;

/** Estado en memoria de la sala unica: conexiones abiertas y difusion de mensajes. */
@Component
class SalaChat {
    private final Map<String, Consumer<String>> participantes = new ConcurrentHashMap<>();

    void agregar(String conexionId, Consumer<String> receptor) {
        participantes.put(conexionId, receptor);
    }

    void quitar(String conexionId) {
        participantes.remove(conexionId);
    }

    void enviarATodos(String texto) {
        for (var participante : participantes.entrySet()) {
            try {
                participante.getValue().accept(texto);
            } catch (RuntimeException e) {
                // Un receptor roto no debe impedir que el resto reciba el mensaje.
                participantes.remove(participante.getKey());
            }
        }
    }
}
