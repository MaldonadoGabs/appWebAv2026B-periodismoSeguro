package com.periodismo.seguro.chat.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.periodismo.seguro.chat.ChatApi;
import com.periodismo.seguro.chat.EstadoEscritura;
import com.periodismo.seguro.chat.MensajeChat;
import com.periodismo.seguro.chat.SuscripcionChat;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/** HU-19: la conversacion se guarda en la base de datos y quien entra recibe el historial. */
@SpringBootTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:historialChatHu19;DB_CLOSE_DELAY=-1"
})
class HistorialChatTest {
    @Autowired ChatApi chat;
    @Autowired MensajeChatRepository repo;

    private static String marca() { return "msg-" + UUID.randomUUID(); }

    @Test
    void cadaMensajeEnviadoSeGuardaEnLaBaseDeDatos() {
        String texto = marca();
        long antes = repo.count();

        chat.enviar("cuenta-historial-1", texto);

        assertEquals(antes + 1, repo.count());
        assertTrue(repo.findAll().stream().anyMatch(m -> m.getTexto().equals(texto)));
    }

    @Test
    void elHistorialVieneEnOrdenCronologicoYRespetaElLimite() {
        String a = marca(), b = marca(), c = marca();
        chat.enviar("cuenta-historial-2", a);
        chat.enviar("cuenta-historial-2", b);
        chat.enviar("cuenta-historial-2", c);

        List<MensajeChat> ultimos = chat.historial(2);

        assertEquals(2, ultimos.size());
        assertEquals(b, ultimos.get(0).texto(), "el mas antiguo de los dos va primero");
        assertEquals(c, ultimos.get(1).texto());
    }

    @Test
    void quienEntraRecibeElHistorialAntesDelAvisoDeIngreso() {
        String uno = marca(), dos = marca();
        chat.enviar("cuenta-historial-3", uno);
        chat.enviar("cuenta-historial-3", dos);
        List<String> orden = new ArrayList<>();

        chat.abrirConexion("conexion-" + UUID.randomUUID(), "cuenta-nueva", new SuscripcionChat(
            m -> orden.add("mensaje:" + m.texto()),
            usuarios -> { },
            usuario -> orden.add("ingreso"),
            (EstadoEscritura e) -> { }));

        int posUno = orden.indexOf("mensaje:" + uno);
        int posDos = orden.indexOf("mensaje:" + dos);
        int posIngreso = orden.indexOf("ingreso");
        assertTrue(posUno >= 0 && posDos > posUno, "recibe el historial en orden");
        assertTrue(posIngreso > posDos, "el aviso de ingreso llega despues del historial");
    }

    @Test
    void unMensajeVacioNoSeGuardaYUnoLargoSeRecortaA500() {
        long antes = repo.count();
        chat.enviar("cuenta-historial-4", "   ");
        assertEquals(antes, repo.count());

        chat.enviar("cuenta-historial-4", "x".repeat(900));
        List<MensajeChat> ultimos = chat.historial(1);
        assertEquals(500, ultimos.get(0).texto().length());
    }

    @Test
    void elHistorialNoGuardaCuentaNiDatosPersonales() {
        chat.enviar("cuenta-secreta-historial", marca());

        for (Field campo : MensajeGuardado.class.getDeclaredFields()) {
            String nombre = campo.getName().toLowerCase();
            assertFalse(nombre.contains("cuenta") || nombre.contains("correo") || nombre.contains("nombrereal"),
                "la tabla del historial no debe guardar " + campo.getName());
        }
        assertTrue(repo.findAll().stream().noneMatch(m -> m.getAutorSeudonimo().contains("cuenta-secreta")));
    }

    @Test
    void lasHorasSeGuardanConPrecisionDeMinuto() {
        chat.enviar("cuenta-historial-5", marca());

        MensajeChat ultimo = chat.historial(1).get(0);
        assertEquals(0, ultimo.enviadoEn().getEpochSecond() % 60, "sin segundos");
        assertEquals(0, ultimo.enviadoEn().getNano());
    }
}
