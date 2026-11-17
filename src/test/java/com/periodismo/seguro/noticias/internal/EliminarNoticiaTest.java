package com.periodismo.seguro.noticias.internal;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.periodismo.seguro.noticias.ImagenAdjunta;
import com.periodismo.seguro.noticias.NoticiasApi;
import com.periodismo.seguro.noticias.SolicitudNoticia;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/** HU-14: borrado con confirmacion, solo autor o administrador, limpiando imagenes y referencias. */
@SpringBootTest
@TestPropertySource(properties = {
    "app.imagenes.directorio=target/imagenes-prueba-eliminar",
    "spring.datasource.url=jdbc:h2:mem:eliminarHu14;DB_CLOSE_DELAY=-1"
})
class EliminarNoticiaTest {
    private static final Path DIRECTORIO =
        Paths.get("target/imagenes-prueba-eliminar").toAbsolutePath().normalize();

    @Autowired NoticiasApi api;
    @Autowired NoticiaRepository noticias;
    @Autowired ImagenNoticiaRepository imagenesNoticia;
    @Autowired AlmacenImagenes almacen;

    private static byte[] png() throws IOException {
        BufferedImage imagen = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        ImageIO.write(imagen, "png", salida);
        return salida.toByteArray();
    }

    private String publicar(String autor) throws IOException {
        return api.crear(autor, "Condor Silente 482", new SolicitudNoticia(
            "Nota a eliminar", "Subtitulo", "<p>Texto</p>", "politica",
            List.of(new ImagenAdjunta("image/png", png()))));
    }

    @Test
    void sinConfirmacionNoSeElimina() throws IOException {
        String autor = "autor-" + UUID.randomUUID();
        String id = publicar(autor);

        assertFalse(api.eliminar(autor, id, false, false));
        assertTrue(api.obtener(id).isPresent());
    }

    @Test
    void elAutorConfirmaYSeBorranNotaImagenesYReferencias() throws IOException {
        String autor = "autor-" + UUID.randomUUID();
        String id = publicar(autor);
        String nombre = imagenesNoticia.findByNoticiaIdOrderByOrdenAsc(id).get(0).getNombreGuardado();
        assertTrue(almacen.existe(nombre));

        assertTrue(api.eliminar(autor, id, true, false));

        assertTrue(api.obtener(id).isEmpty());
        assertTrue(noticias.findById(id).isEmpty());
        assertTrue(imagenesNoticia.findByNoticiaIdOrderByOrdenAsc(id).isEmpty());
        assertFalse(almacen.existe(nombre));
        assertFalse(Files.isRegularFile(DIRECTORIO.resolve(nombre)));
    }

    @Test
    void otroPeriodistaNoPuedeEliminarLaNotaAjena() throws IOException {
        String id = publicar("autor-" + UUID.randomUUID());

        assertFalse(api.eliminar("otro-" + UUID.randomUUID(), id, true, false));
        assertTrue(api.obtener(id).isPresent());
    }

    @Test
    void unAdministradorPuedeEliminarUnaNotaAjena() throws IOException {
        String id = publicar("autor-" + UUID.randomUUID());

        assertTrue(api.eliminar("cuenta-admin", id, true, true));
        assertTrue(api.obtener(id).isEmpty());
    }

    @Test
    void unaNoticiaInexistenteOCuentaNulaNoSeElimina() {
        assertFalse(api.eliminar("autor", "no-existe", true, false));
        assertFalse(api.eliminar(null, "no-existe", true, true));
    }
}
