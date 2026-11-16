package com.periodismo.seguro.chat.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.periodismo.seguro.autenticacion.AutenticacionApi;
import com.periodismo.seguro.chat.SuscripcionChat;
import com.periodismo.seguro.cuentas.CuentasApi;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Mensajes y lista de conectados de la sala, siempre con seudonimos. */
class ChatSalaTest {
    private final SalaChat sala = new SalaChat();
    private final AutenticacionApi autenticacion = mock(AutenticacionApi.class);
    private final CuentasApi cuentas = mock(CuentasApi.class);
    private final ChatService chat = new ChatService(autenticacion, cuentas, sala);

    @BeforeEach
    void preparar() {
        when(cuentas.seudonimoDe("cuenta-1")).thenReturn(Optional.of("Ana_Rojo"));
        when(cuentas.seudonimoDe("cuenta-2")).thenReturn(Optional.of("Zorro_25"));
    }

    @Test
    void elMensajeLlegaATodasLasConexiones() {
        Registro uno = registro();
        Registro otro = registro();
        abrir("c1", "cuenta-1", uno);
        abrir("c2", "cuenta-2", otro);

        chat.enviar("  texto compartido  ");

        assertEquals(List.of("texto compartido"), uno.mensajes);
        assertEquals(List.of("texto compartido"), otro.mensajes);
    }

    @Test
    void unMensajeVacioNoSeDifunde() {
        Registro registro = registro();
        abrir("c1", "cuenta-1", registro);

        chat.enviar("   ");

        assertTrue(registro.mensajes.isEmpty());
    }

    @Test
    void unaConexionCerradaDejaDeRecibirMensajes() {
        Registro emisor = registro();
        Registro saliente = registro();
        abrir("c1", "cuenta-1", emisor);
        abrir("c2", "cuenta-2", saliente);
        chat.cerrarConexion("c2");

        chat.enviar("hola");

        assertEquals(List.of("hola"), emisor.mensajes);
        assertTrue(saliente.mensajes.isEmpty());
    }

    @Test
    void unReceptorRotoNoImpideQueLosDemasRecibanElMensaje() {
        Registro sano = registro();
        chat.abrirConexion("roto", "cuenta-1", new SuscripcionChat(
            texto -> { throw new IllegalStateException("fallo"); }, usuarios -> { }));
        abrir("c2", "cuenta-2", sano);

        chat.enviar("hola");

        assertEquals(List.of("hola"), sano.mensajes);
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
        chat.abrirConexion(conexionId, cuentaId,
            new SuscripcionChat(registro.mensajes::add, registro.conectados::add));
    }

    private static Registro registro() {
        return new Registro();
    }

    private static final class Registro {
        private final List<String> mensajes = new ArrayList<>();
        private final List<List<String>> conectados = new ArrayList<>();
    }
}
