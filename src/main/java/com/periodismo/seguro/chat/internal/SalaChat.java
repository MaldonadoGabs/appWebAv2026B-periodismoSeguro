package com.periodismo.seguro.chat.internal;

import com.periodismo.seguro.chat.SuscripcionChat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/** Estado en memoria de la sala unica: conexiones abiertas, mensajes y participantes visibles. */
@Component
class SalaChat {
    private final Map<String, Participante> participantes = new ConcurrentHashMap<>();

    void agregar(String conexionId, String cuentaId, String seudonimo, SuscripcionChat suscripcion) {
        participantes.put(conexionId, new Participante(cuentaId, seudonimo, suscripcion));
        notificarConectados();
    }

    void quitar(String conexionId) {
        participantes.remove(conexionId);
        notificarConectados();
    }

    void enviarATodos(String texto) {
        for (var entrada : participantes.entrySet()) {
            try {
                entrada.getValue().suscripcion().mensajes().accept(texto);
            } catch (RuntimeException e) {
                // Un receptor roto no debe impedir que el resto reciba el mensaje.
                participantes.remove(entrada.getKey());
            }
        }
    }

    /** Solo seudonimos: nunca el id de cuenta, el correo ni el nombre real. */
    List<String> usuariosConectados() {
        return participantes.values().stream()
            .map(Participante::seudonimo)
            .distinct()
            .sorted(String.CASE_INSENSITIVE_ORDER)
            .toList();
    }

    private void notificarConectados() {
        List<String> conectados = usuariosConectados();
        for (var entrada : participantes.entrySet()) {
            try {
                entrada.getValue().suscripcion().conectados().accept(conectados);
            } catch (RuntimeException e) {
                participantes.remove(entrada.getKey());
            }
        }
    }

    private record Participante(String cuentaId, String seudonimo, SuscripcionChat suscripcion) {
    }
}
