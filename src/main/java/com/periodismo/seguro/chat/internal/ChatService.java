package com.periodismo.seguro.chat.internal;

import com.periodismo.seguro.autenticacion.AutenticacionApi;
import com.periodismo.seguro.chat.ChatApi;
import com.periodismo.seguro.chat.SuscripcionChat;
import com.periodismo.seguro.cuentas.CuentasApi;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
class ChatService implements ChatApi {
    private static final int MAXIMO_CARACTERES = 500;
    private static final String SEUDONIMO_POR_DEFECTO = "Periodista";

    private final AutenticacionApi autenticacion;
    private final CuentasApi cuentas;
    private final SalaChat sala;

    ChatService(AutenticacionApi autenticacion, CuentasApi cuentas, SalaChat sala) {
        this.autenticacion = autenticacion;
        this.cuentas = cuentas;
        this.sala = sala;
    }

    /** Fail-closed: sin sesion valida no hay entrada en la sala. */
    @Override
    public Optional<String> conectar(String sesionId) {
        return autenticacion.validarSesion(sesionId);
    }

    @Override
    public void abrirConexion(String conexionId, String cuentaId, SuscripcionChat suscripcion) {
        String seudonimo = cuentas.seudonimoDe(cuentaId).orElse(SEUDONIMO_POR_DEFECTO);
        sala.agregar(conexionId, cuentaId, seudonimo, suscripcion);
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

    @Override
    public List<String> usuariosConectados() {
        return sala.usuariosConectados();
    }
}
