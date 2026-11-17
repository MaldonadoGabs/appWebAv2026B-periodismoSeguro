package com.periodismo.seguro.noticias.internal;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;

/** Re-codifica las imagenes para descartar EXIF/GPS y valida los formatos permitidos. */
final class RecodificadorImagenes {
    static final long MAXIMO_BYTES = 5L * 1024 * 1024;
    static final Pattern NOMBRE_GENERADO =
        Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(png|jpg)");
    private static final Map<String, String> FORMATOS = Map.of("image/png", "png", "image/jpeg", "jpg");

    private RecodificadorImagenes() {}

    static String extension(String tipoContenido) {
        String extension = FORMATOS.get(tipoContenido);
        if (extension == null) {
            throw new IllegalArgumentException("Solo se admiten imagenes PNG o JPEG");
        }
        return extension;
    }

    static byte[] recodificar(byte[] contenido, String extension) {
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
