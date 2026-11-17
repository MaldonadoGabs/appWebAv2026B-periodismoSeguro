package com.periodismo.seguro.interacciones.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.periodismo.seguro.interacciones.Comentario;
import com.periodismo.seguro.interacciones.InteraccionesApi;
import com.periodismo.seguro.interacciones.TipoReaccion;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/** Reacciones y comentarios de lectores sin exponer su cuenta real. */
@SpringBootTest
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:interaccionesHu;DB_CLOSE_DELAY=-1")
class InteraccionesServiceTest {
    @Autowired InteraccionesApi api;

    @Test
    void unaReaccionSePuedeSustituirYQuitar() {
        api.reaccionar("lector-1", "noticia-1", TipoReaccion.ME_GUSTA);
        assertEquals(Optional.of(TipoReaccion.ME_GUSTA), api.reaccionDe("lector-1", "noticia-1"));
        assertEquals(1L, api.conteo("noticia-1").get(TipoReaccion.ME_GUSTA));

        api.reaccionar("lector-1", "noticia-1", TipoReaccion.IMPACTA);
        assertEquals(0L, api.conteo("noticia-1").get(TipoReaccion.ME_GUSTA));
        assertEquals(1L, api.conteo("noticia-1").get(TipoReaccion.IMPACTA));

        api.reaccionar("lector-1", "noticia-1", TipoReaccion.IMPACTA);
        assertTrue(api.reaccionDe("lector-1", "noticia-1").isEmpty());
        assertEquals(0L, api.conteo("noticia-1").get(TipoReaccion.IMPACTA));
    }

    @Test
    void variosLectoresSumaCadaTipoDeReaccion() {
        api.reaccionar("lector-1", "noticia-2", TipoReaccion.ME_GUSTA);
        api.reaccionar("lector-2", "noticia-2", TipoReaccion.ME_GUSTA);
        api.reaccionar("lector-3", "noticia-2", TipoReaccion.IMPORTANTE);

        assertEquals(2L, api.conteo("noticia-2").get(TipoReaccion.ME_GUSTA));
        assertEquals(1L, api.conteo("noticia-2").get(TipoReaccion.IMPORTANTE));
    }

    @Test
    void unComentarioGuardaElSeudonimoYNuncaLaCuenta() {
        Comentario comentario = api.comentar("lector-secreto", "Condor Silente 482",
            "noticia-3", "  Gracias por informar  ");

        List<Comentario> lista = api.comentarios("noticia-3");
        assertEquals(1, lista.size());
        assertEquals("Condor Silente 482", comentario.autorSeudonimo());
        assertEquals("Gracias por informar", comentario.texto());
        assertTrue(lista.get(0).hora().matches("\\d{2}/\\d{2} \\d{2}:\\d{2} UTC"));
        assertEquals("noticia-3", lista.get(0).noticiaId());
    }

    @Test
    void unComentarioVacioSeRechaza() {
        assertThrows(IllegalArgumentException.class,
            () -> api.comentar("lector-1", "Seudo", "noticia-4", "   "));
        assertTrue(api.comentarios("noticia-4").isEmpty());
    }
}
