package com.periodismo.seguro.chat.internal;

import com.periodismo.seguro.autenticacion.AutenticacionApi;
import com.periodismo.seguro.chat.ChatApi;
import com.periodismo.seguro.chat.MensajeChat;
import com.periodismo.seguro.chat.SuscripcionChat;
import com.periodismo.seguro.cuentas.CuentasApi;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
class ChatService implements ChatApi {
    private static final int MAXIMO_CARACTERES = 500;
    private static final String SEUDONIMO_POR_DEFECTO = "Lector";

    private final AutenticacionApi autenticacion;
    private final CuentasApi cuentas;
    private final SalaChat sala;
    private final Clock reloj;

    ChatService(AutenticacionApi autenticacion, CuentasApi cuentas, SalaChat sala, Clock reloj) {
        this.autenticacion = autenticacion;
        this.cuentas = cuentas;
        this.sala = sala;
        this.reloj = reloj;
    }

    /** Fail-closed: sin sesion valida no hay entrada en la sala. */
    @Override
    public Optional<String> conectar(String sesionId) {
        return autenticacion.validarSesion(sesionId);
    }

    @Override
    public void abrirConexion(String conexionId, String cuentaId, SuscripcionChat suscripcion) {
        sala.agregar(conexionId, cuentaId, seudonimoDe(cuentaId), suscripcion);
    }

    @Override
    public void cerrarConexion(String conexionId) {
        sala.quitar(conexionId);
    }

    @Override
    public void enviar(String cuentaId, String texto) {
        if (cuentaId == null || texto == null) {
            return;
        }
        String limpio = texto.trim();
        if (limpio.isEmpty()) {
            return;
        }
        // Sin segundos: la conversacion muestra la hora, no un instante exacto de actividad.
        sala.enviarMensaje(new MensajeChat(seudonimoDe(cuentaId),
            limpio.substring(0, Math.min(limpio.length(), MAXIMO_CARACTERES)),
            reloj.instant().truncatedTo(ChronoUnit.MINUTES)));
    }

    @Override
    public void avisarEscritura(String cuentaId, boolean activo) {
        if (cuentaId == null) {
            return;
        }
        sala.avisarEscritura(cuentaId, seudonimoDe(cuentaId), activo);
    }

    @Override
    public List<String> usuariosConectados() {
        return sala.usuariosConectados();
    }

    private String seudonimoDe(String cuentaId) {
        return cuentas.seudonimoDe(cuentaId).orElse(SEUDONIMO_POR_DEFECTO);
    }
}
