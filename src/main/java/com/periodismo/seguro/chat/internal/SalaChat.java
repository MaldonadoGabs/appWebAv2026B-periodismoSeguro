package com.periodismo.seguro.chat.internal;

import com.periodismo.seguro.chat.EstadoEscritura;
import com.periodismo.seguro.chat.MensajeChat;
import com.periodismo.seguro.chat.SuscripcionChat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/** Estado en memoria de la sala unica: conexiones, conversacion y participantes visibles. */
@Component
class SalaChat {
    private final Map<String, Participante> participantes = new ConcurrentHashMap<>();

    void agregar(String conexionId, String cuentaId, String seudonimo, SuscripcionChat suscripcion) {
        participantes.put(conexionId, new Participante(cuentaId, seudonimo, suscripcion));
        notificarConectados();
        notificarIngreso(seudonimo);
    }

    void quitar(String conexionId) {
        participantes.remove(conexionId);
        notificarConectados();
    }

    void enviarMensaje(MensajeChat mensaje) {
        for (var entrada : participantes.entrySet()) {
            try {
                entrada.getValue().suscripcion().mensajes().accept(mensaje);
            } catch (RuntimeException e) {
                // Un receptor roto no debe impedir que el resto reciba el mensaje.
                participantes.remove(entrada.getKey());
            }
        }
    }

    /** El que escribe no recibe su propio aviso; el resto ve el seudonimo, nunca su cuenta. */
    void avisarEscritura(String cuentaIdOrigen, String seudonimo, boolean activo) {
        EstadoEscritura estado = new EstadoEscritura(seudonimo, activo);
        for (var entrada : participantes.entrySet()) {
            Participante participante = entrada.getValue();
            if (participante.cuentaId().equals(cuentaIdOrigen)) {
                continue;
            }
            try {
                participante.suscripcion().escribiendo().accept(estado);
            } catch (RuntimeException e) {
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

    private void notificarIngreso(String seudonimo) {
        for (var entrada : participantes.entrySet()) {
            try {
                entrada.getValue().suscripcion().ingresos().accept(seudonimo);
            } catch (RuntimeException e) {
                participantes.remove(entrada.getKey());
            }
        }
    }

    private record Participante(String cuentaId, String seudonimo, SuscripcionChat suscripcion) {
    }
}
