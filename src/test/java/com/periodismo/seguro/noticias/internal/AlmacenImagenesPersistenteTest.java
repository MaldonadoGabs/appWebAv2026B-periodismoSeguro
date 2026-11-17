package com.periodismo.seguro.noticias.internal;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.periodismo.seguro.noticias.ImagenAdjunta;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/** La copia en BD permite servir la imagen aunque el archivo local haya desaparecido. */
@SpringBootTest
@TestPropertySource(properties = {
    "app.imagenes.directorio=target/imagenes-prueba-persistente",
    "spring.datasource.url=jdbc:h2:mem:imagenesPersistente;DB_CLOSE_DELAY=-1"
})
class AlmacenImagenesPersistenteTest {
    private static final Path DIRECTORIO =
        Paths.get("target/imagenes-prueba-persistente").toAbsolutePath().normalize();

    @Autowired AlmacenImagenes almacen;
    @Autowired ImagenAlmacenadaRepository repositorio;

    @Test
    void laImagenSigueDisponibleCuandoSePierdeElArchivoEnDisco() throws IOException {
        BufferedImage imagen = new BufferedImage(3, 2, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        ImageIO.write(imagen, "png", salida);

        String nombre = almacen.guardar(new ImagenAdjunta("image/png", salida.toByteArray()));
        Path archivo = DIRECTORIO.resolve(nombre);
        assertTrue(Files.isRegularFile(archivo));
        assertTrue(repositorio.existsById(nombre));

        Files.deleteIfExists(archivo);
        assertFalse(Files.isRegularFile(archivo));
        assertTrue(almacen.existe(nombre));
        byte[] leidos = almacen.leer(nombre).orElseThrow();
        assertTrue(leidos.length > 0);

        almacen.eliminar(nombre);
        assertFalse(almacen.existe(nombre));
        assertFalse(repositorio.existsById(nombre));
        assertArrayEquals(new byte[0], almacen.leer(nombre).orElse(new byte[0]));
    }
}
