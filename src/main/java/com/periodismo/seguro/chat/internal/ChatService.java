package com.periodismo.seguro.chat.internal;

import com.periodismo.seguro.autenticacion.AutenticacionApi;
import com.periodismo.seguro.chat.ChatApi;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
class ChatService implements ChatApi {
    private final AutenticacionApi autenticacion;

    ChatService(AutenticacionApi autenticacion) { this.autenticacion = autenticacion; }

    /** HU-17. Fail-closed: sin sesion valida no hay conexion. */
    @Override
    public boolean conectar(String sesionId) { return autenticacion.validarSesion(sesionId).isPresent(); }

    @Override
    public void enviar(String sesionId, String texto) {
        throw new UnsupportedOperationException("HU-18 pendiente");
    }

    @Override public List<String> historial(int limite) { return List.of(); }
    @Override public List<String> usuariosConectados() { return List.of(); }
    @Override public int mensajesSinLeer(String cuentaId) { return 0; }
}
