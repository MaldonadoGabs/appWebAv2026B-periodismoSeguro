package com.periodismo.seguro.notificaciones.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.periodismo.seguro.noticias.NoticiaCreada;
import com.periodismo.seguro.notificaciones.AvisoNoticia;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** HU-21: el evento de publicacion se convierte en un aviso publico con titulo y seudonimo. */
class NotificacionesServiceTest {
    @Test
    void alCrearseUnaNoticiaSeEntregaElAvisoALasConexionesAbiertas() {
        SalaAvisos sala = new SalaAvisos();
        NotificacionesService servicio = new NotificacionesService(sala);
        List<AvisoNoticia> recibidos = new ArrayList<>();
        sala.agregar("c1", recibidos::add);
        sala.agregar("c2", recibidos::add);

        servicio.alCrearseNoticia(new NoticiaCreada("n-1", "Titulo nuevo", "Condor Silente 482", "politica"));

        assertEquals(List.of(
            new AvisoNoticia("n-1", "Titulo nuevo", "Condor Silente 482"),
            new AvisoNoticia("n-1", "Titulo nuevo", "Condor Silente 482")), recibidos);
    }

    @Test
    void unaConexionCerradaDejaDeRecibirAvisos() {
        SalaAvisos sala = new SalaAvisos();
        NotificacionesService servicio = new NotificacionesService(sala);
        List<AvisoNoticia> recibidos = new ArrayList<>();
        sala.agregar("c1", recibidos::add);
        sala.quitar("c1");

        servicio.avisarNoticiaNueva(new AvisoNoticia("n-2", "Otro titulo", "Seudonimo"));

        assertTrue(recibidos.isEmpty());
    }
}
