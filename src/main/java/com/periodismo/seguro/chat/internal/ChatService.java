package com.periodismo.seguro.chat.internal;

import com.periodismo.seguro.autenticacion.AutenticacionApi;
import com.periodismo.seguro.chat.ChatApi;
import com.periodismo.seguro.chat.MensajeChat;
import com.periodismo.seguro.chat.SuscripcionChat;
import com.periodismo.seguro.cuentas.CuentasApi;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
class ChatService implements ChatApi {
    private static final int MAXIMO_CARACTERES = 500;
    private static final String SEUDONIMO_POR_DEFECTO = "Lector";
    private static final int HISTORIAL_AL_ENTRAR = 50;
    private static final int HISTORIAL_MAXIMO = 200;

    private final AutenticacionApi autenticacion;
    private final CuentasApi cuentas;
    private final SalaChat sala;
    private final Clock reloj;
    private final MensajeChatRepository mensajes;

    ChatService(AutenticacionApi autenticacion, CuentasApi cuentas, SalaChat sala, Clock reloj,
                MensajeChatRepository mensajes) {
        this.autenticacion = autenticacion;
        this.cuentas = cuentas;
        this.sala = sala;
        this.reloj = reloj;
        this.mensajes = mensajes;
    }

    /** Fail-closed: sin sesion valida no hay entrada en la sala. */
    @Override
    public Optional<String> conectar(String sesionId) {
        return autenticacion.validarSesion(sesionId);
    }

    @Override
    public void abrirConexion(String conexionId, String cuentaId, SuscripcionChat suscripcion) {
        // HU-19: quien entra recibe primero la conversacion guardada y despues el aviso de que se unio.
        for (MensajeChat guardado : historial(HISTORIAL_AL_ENTRAR)) {
            try {
                suscripcion.mensajes().accept(guardado);
            } catch (RuntimeException e) {
                break; // la conexion ya no responde: la sala la retirara
            }
        }
        sala.agregar(conexionId, cuentaId, seudonimoDe(cuentaId), suscripcion);
    }

    @Override
    public List<MensajeChat> historial(int limite) {
        int tope = Math.max(1, Math.min(limite, HISTORIAL_MAXIMO));
        List<MensajeChat> recientes = new ArrayList<>();
        for (MensajeGuardado m : mensajes.findAllByOrderByIdDesc(PageRequest.of(0, tope))) {
            recientes.add(new MensajeChat(m.getAutorSeudonimo(), m.getTexto(), m.getEnviadoEn()));
        }
        Collections.reverse(recientes);
        return recientes;
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
        MensajeChat mensaje = new MensajeChat(seudonimoDe(cuentaId),
            limpio.substring(0, Math.min(limpio.length(), MAXIMO_CARACTERES)),
            reloj.instant().truncatedTo(ChronoUnit.MINUTES));
        // HU-19: se guarda antes de difundir; solo seudonimo, texto y hora (nunca la cuenta).
        mensajes.save(new MensajeGuardado(mensaje.autorSeudonimo(), mensaje.texto(), mensaje.enviadoEn()));
        sala.enviarMensaje(mensaje);
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
