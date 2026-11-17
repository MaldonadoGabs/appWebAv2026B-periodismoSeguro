package com.periodismo.seguro.chat.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.periodismo.seguro.autenticacion.AutenticacionApi;
import com.periodismo.seguro.chat.EstadoEscritura;
import com.periodismo.seguro.chat.MensajeChat;
import com.periodismo.seguro.chat.SuscripcionChat;
import com.periodismo.seguro.cuentas.CuentasApi;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Conversacion de la sala: autor, hora, ingresos y escritura, siempre con seudonimos. */
class ChatSalaTest {
    private static final Instant INSTANTE = Instant.parse("2026-11-17T14:30:37Z");
    private static final Instant HORA = Instant.parse("2026-11-17T14:30:00Z");

    private final SalaChat sala = new SalaChat();
    private final AutenticacionApi autenticacion = mock(AutenticacionApi.class);
    private final CuentasApi cuentas = mock(CuentasApi.class);
    private final ChatService chat = new ChatService(
        autenticacion, cuentas, sala, Clock.fixed(INSTANTE, ZoneOffset.UTC));

    @BeforeEach
    void preparar() {
        when(cuentas.seudonimoDe("cuenta-1")).thenReturn(Optional.of("Ana_Rojo"));
        when(cuentas.seudonimoDe("cuenta-2")).thenReturn(Optional.of("Zorro_25"));
    }

    @Test
    void elMensajeLlegaConAutorYHoraRedondeadaAlMinuto() {
        Registro uno = registro();
        Registro otro = registro();
        abrir("c1", "cuenta-1", uno);
        abrir("c2", "cuenta-2", otro);

        chat.enviar("cuenta-1", "  texto compartido  ");

        MensajeChat esperado = new MensajeChat("Ana_Rojo", "texto compartido", HORA);
        assertEquals(List.of(esperado), uno.mensajes);
        assertEquals(List.of(esperado), otro.mensajes);
        assertEquals(0, uno.mensajes.get(0).enviadoEn().getEpochSecond() % 60);
    }

    @Test
    void unMensajeVacioNoSeDifunde() {
        Registro registro = registro();
        abrir("c1", "cuenta-1", registro);

        chat.enviar("cuenta-1", "   ");

        assertTrue(registro.mensajes.isEmpty());
    }

    @Test
    void unaConexionCerradaDejaDeRecibirMensajes() {
        Registro emisor = registro();
        Registro saliente = registro();
        abrir("c1", "cuenta-1", emisor);
        abrir("c2", "cuenta-2", saliente);
        chat.cerrarConexion("c2");

        chat.enviar("cuenta-1", "hola");

        assertEquals(1, emisor.mensajes.size());
        assertTrue(saliente.mensajes.isEmpty());
    }

    @Test
    void unReceptorRotoNoImpideQueLosDemasRecibanElMensaje() {
        Registro sano = registro();
        chat.abrirConexion("roto", "cuenta-1", new SuscripcionChat(
            mensaje -> { throw new IllegalStateException("fallo"); }, conectados -> { },
            ingreso -> { }, estado -> { }));
        abrir("c2", "cuenta-2", sano);

        chat.enviar("cuenta-2", "hola");

        assertEquals(1, sano.mensajes.size());
    }

    @Test
    void laListaMuestraSeudonimosUnicosYOrdenados() {
        Registro registro = registro();
        abrir("c1", "cuenta-2", registro);
        abrir("c2", "cuenta-1", registro);
        abrir("c3", "cuenta-1", registro);

        assertEquals(List.of("Ana_Rojo", "Zorro_25"), chat.usuariosConectados());
        assertEquals(List.of("Ana_Rojo", "Zorro_25"), registro.conectados.getLast());
    }

    @Test
    void laListaSeActualizaAlConectarYAlDesconectar() {
        Registro registro = registro();
        abrir("c1", "cuenta-1", registro);
        assertEquals(List.of("Ana_Rojo"), registro.conectados.getLast());

        abrir("c2", "cuenta-2", registro);
        assertEquals(List.of("Ana_Rojo", "Zorro_25"), registro.conectados.getLast());

        chat.cerrarConexion("c1");
        assertEquals(List.of("Zorro_25"), registro.conectados.getLast());
        assertEquals(List.of("Zorro_25"), chat.usuariosConectados());
    }

    @Test
    void alEntrarAlguienSeAvisaConSuSeudonimo() {
        Registro primero = registro();
        Registro segundo = registro();
        abrir("c1", "cuenta-1", primero);
        abrir("c2", "cuenta-2", segundo);

        assertEquals(List.of("Ana_Rojo", "Zorro_25"), primero.ingresos);
        assertEquals(List.of("Zorro_25"), segundo.ingresos);
    }

    @Test
    void elAvisoDeEscrituraNoSeEnviaAQuienEscribe() {
        Registro emisor = registro();
        Registro otro = registro();
        abrir("c1", "cuenta-1", emisor);
        abrir("c2", "cuenta-2", otro);

        chat.avisarEscritura("cuenta-1", true);

        assertTrue(emisor.escribiendo.isEmpty());
        assertEquals(List.of(new EstadoEscritura("Ana_Rojo", true)), otro.escribiendo);
    }

    @Test
    void sinConexionesLaListaEstaVacia() {
        assertTrue(chat.usuariosConectados().isEmpty());
    }

    @Test
    void laConexionValidaLaSesionHttpDelModuloDeAutenticacion() {
        when(autenticacion.validarSesion("sesion-1")).thenReturn(Optional.of("cuenta-1"));
        when(autenticacion.validarSesion("sesion-invalida")).thenReturn(Optional.empty());

        assertEquals(Optional.of("cuenta-1"), chat.conectar("sesion-1"));
        assertEquals(Optional.empty(), chat.conectar("sesion-invalida"));
    }

    private void abrir(String conexionId, String cuentaId, Registro registro) {
        chat.abrirConexion(conexionId, cuentaId, new SuscripcionChat(
            registro.mensajes::add, registro.conectados::add, registro.ingresos::add, registro.escribiendo::add));
    }

    private static Registro registro() {
        return new Registro();
    }

    private static final class Registro {
        private final List<MensajeChat> mensajes = new ArrayList<>();
        private final List<List<String>> conectados = new ArrayList<>();
        private final List<String> ingresos = new ArrayList<>();
        private final List<EstadoEscritura> escribiendo = new ArrayList<>();
    }
}
