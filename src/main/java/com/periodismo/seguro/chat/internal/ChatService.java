package com.periodismo.seguro.chat.internal;

import com.periodismo.seguro.autenticacion.AutenticacionApi;
import com.periodismo.seguro.chat.ChatApi;
import java.util.Optional;
import java.util.function.Consumer;
import org.springframework.stereotype.Service;

@Service
class ChatService implements ChatApi {
    private static final int MAXIMO_CARACTERES = 500;

    private final AutenticacionApi autenticacion;
    private final SalaChat sala;

    ChatService(AutenticacionApi autenticacion, SalaChat sala) {
        this.autenticacion = autenticacion;
        this.sala = sala;
    }

    /** Fail-closed: sin sesion valida no hay entrada en la sala. */
    @Override
    public Optional<String> conectar(String sesionId) {
        return autenticacion.validarSesion(sesionId);
    }

    @Override
    public void abrirConexion(String conexionId, Consumer<String> receptor) {
        sala.agregar(conexionId, receptor);
    }

    @Override
    public void cerrarConexion(String conexionId) {
        sala.quitar(conexionId);
    }

    @Override
    public void enviar(String texto) {
        if (texto == null) {
            return;
        }
        String limpio = texto.trim();
        if (limpio.isEmpty()) {
            return;
        }
        sala.enviarATodos(limpio.substring(0, Math.min(limpio.length(), MAXIMO_CARACTERES)));
    }
}
