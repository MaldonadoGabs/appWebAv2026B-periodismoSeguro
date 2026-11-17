package com.periodismo.seguro.notificaciones.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.periodismo.seguro.noticias.NoticiasApi;
import com.periodismo.seguro.noticias.SolicitudNoticia;
import com.periodismo.seguro.notificaciones.AvisoNoticia;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/** HU-21: de la publicacion real al receptor del WebSocket, despues del commit. */
@SpringBootTest
@TestPropertySource(properties = {
    "app.imagenes.directorio=target/imagenes-prueba-avisos-integracion",
    "spring.datasource.url=jdbc:h2:mem:avisosHu21;DB_CLOSE_DELAY=-1"
})
class NotificacionesIntegracionTest {
    @Autowired NoticiasApi noticias;
    @Autowired SalaAvisos sala;

    @Test
    void publicarUnaNoticiaEntregaElAvisoConTituloYSeudonimo() {
        List<AvisoNoticia> recibidos = new ArrayList<>();
        sala.agregar("lector-1", recibidos::add);

        String id = noticias.crear("cuenta-secreta", "Condor Silente 482",
            new SolicitudNoticia("Titulo publicado", "Subtitulo", "<p>Texto</p>", "politica", List.of()));

        assertEquals(List.of(new AvisoNoticia(id, "Titulo publicado", "Condor Silente 482")), recibidos);
    }
}
