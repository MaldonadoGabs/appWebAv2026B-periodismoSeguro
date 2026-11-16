package com.periodismo.seguro.chat.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.periodismo.seguro.autenticacion.AutenticacionApi;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Un mensaje enviado por un participante se refleja en todas las conexiones abiertas de la sala. */
class ChatSalaTest {
    private final SalaChat sala = new SalaChat();
    private final AutenticacionApi autenticacion = mock(AutenticacionApi.class);
    private final ChatService chat = new ChatService(autenticacion, sala);

    @Test
    void elMensajeLlegaATodasLasConexiones() {
        List<String> emisor = new ArrayList<>();
        List<String> otro = new ArrayList<>();
        chat.abrirConexion("c1", emisor::add);
        chat.abrirConexion("c2", otro::add);

        chat.enviar("  texto compartido  ");

        assertEquals(List.of("texto compartido"), emisor);
        assertEquals(List.of("texto compartido"), otro);
    }

    @Test
    void unMensajeVacioNoSeDifunde() {
        List<String> receptores = new ArrayList<>();
        chat.abrirConexion("c1", receptores::add);

        chat.enviar("   ");

        assertTrue(receptores.isEmpty());
    }

    @Test
    void unaConexionCerradaDejaDeRecibirMensajes() {
        List<String> emisor = new ArrayList<>();
        List<String> saliente = new ArrayList<>();
        chat.abrirConexion("c1", emisor::add);
        chat.abrirConexion("c2", saliente::add);
        chat.cerrarConexion("c2");

        chat.enviar("hola");

        assertEquals(List.of("hola"), emisor);
        assertTrue(saliente.isEmpty());
    }

    @Test
    void unReceptorRotoNoImpideQueLosDemasRecibanElMensaje() {
        List<String> sano = new ArrayList<>();
        chat.abrirConexion("roto", texto -> { throw new IllegalStateException("fallo"); });
        chat.abrirConexion("c2", sano::add);

        chat.enviar("hola");

        assertEquals(List.of("hola"), sano);
    }

    @Test
    void laConexionValidaLaSesionHttpDelModuloDeAutenticacion() {
        when(autenticacion.validarSesion("sesion-1")).thenReturn(Optional.of("cuenta-1"));
        when(autenticacion.validarSesion("sesion-invalida")).thenReturn(Optional.empty());

        assertEquals(Optional.of("cuenta-1"), chat.conectar("sesion-1"));
        assertEquals(Optional.empty(), chat.conectar("sesion-invalida"));
    }
}
