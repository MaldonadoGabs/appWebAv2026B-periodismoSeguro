package com.periodismo.seguro.noticias.internal;

import com.periodismo.seguro.noticias.ImagenAdjunta;
import jakarta.annotation.PostConstruct;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * HU-09: imagenes en disco local, con ruta configurable (APP_IMAGENES_DIR).
 * Antes de guardar se re-codifica la imagen: eso descarta EXIF, GPS y demas metadatos que
 * podrian revelar donde o con que equipo trabajo el periodista. El nombre del archivo es un UUID.
 *
 * Limitacion conocida: el filesystem de Azure Container Apps es efimero; las imagenes se pierden
 * al reiniciar o desplegar. El puerto AlmacenImagenes permite migrar sin cambiar el dominio.
 */
@Service
class AlmacenImagenesDisco implements AlmacenImagenes {
    static final long MAXIMO_BYTES = 5L * 1024 * 1024;
    private static final Map<String, String> FORMATOS = Map.of(
        "image/png", "png",
        "image/jpeg", "jpg");
    private static final Pattern NOMBRE_GENERADO =
        Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(png|jpg)");

    private final Path directorio;

    AlmacenImagenesDisco(@Value("${app.imagenes.directorio}") String ruta) {
        this.directorio = Paths.get(ruta).toAbsolutePath().normalize();
    }

    @PostConstruct
    void prepararDirectorio() throws IOException {
        Files.createDirectories(directorio);
    }

    @Override
    public String guardar(ImagenAdjunta imagen) {
        String extension = FORMATOS.get(imagen.tipoContenido());
        if (extension == null) {
            throw new IllegalArgumentException("Solo se admiten imagenes PNG o JPEG");
        }
        byte[] original = imagen.contenido();
        if (original == null || original.length == 0 || original.length > MAXIMO_BYTES) {
            throw new IllegalArgumentException("La imagen esta vacia o supera los 5 MB");
        }
        byte[] sinMetadatos = recodificar(original, extension);
        String nombre = UUID.randomUUID() + "." + extension;
        try {
            Files.write(directorio.resolve(nombre), sinMetadatos);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo guardar la imagen", e);
        }
        return nombre;
    }

    @Override
    public Optional<byte[]> leer(String nombreGuardado) {
        if (nombreGuardado == null || !NOMBRE_GENERADO.matcher(nombreGuardado).matches()) {
            return Optional.empty();
        }
        Path archivo = directorio.resolve(nombreGuardado).normalize();
        if (!archivo.startsWith(directorio) || !Files.isRegularFile(archivo)) {
            return Optional.empty();
        }
        try {
            return Optional.of(Files.readAllBytes(archivo));
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    /** Lee y vuelve a escribir el bitmap: el archivo resultante no conserva EXIF ni otros metadatos. */
    private static byte[] recodificar(byte[] contenido, String extension) {
        try {
            BufferedImage imagen = ImageIO.read(new ByteArrayInputStream(contenido));
            if (imagen == null) {
                throw new IllegalArgumentException("El archivo no es una imagen valida");
            }
            ByteArrayOutputStream salida = new ByteArrayOutputStream();
            String formato = "jpg".equals(extension) ? "jpg" : "png";
            if (!ImageIO.write(imagen, formato, salida)) {
                throw new IllegalArgumentException("No se pudo procesar la imagen");
            }
            return salida.toByteArray();
        } catch (IOException e) {
            throw new IllegalArgumentException("El archivo no es una imagen valida", e);
        }
    }
}
