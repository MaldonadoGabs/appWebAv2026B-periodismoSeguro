package com.periodismo.seguro.noticias.internal;

import com.periodismo.seguro.noticias.ImagenAdjunta;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Almacen de imagenes con copia persistente en BD y copia local en disco.
 * El disco (temporal por defecto) puede desaparecer al reiniciar o desplegar: si eso pasa,
 * la copia en BD sigue sirviendo la imagen en el detalle y en el feed. El dominio no sabe
 * donde estan los bytes; solo conoce el puerto AlmacenImagenes.
 */
@Service
class AlmacenImagenesPersistente implements AlmacenImagenes {
    private final Path directorio;
    private final ImagenAlmacenadaRepository repositorio;

    AlmacenImagenesPersistente(@Value("${app.imagenes.directorio}") String ruta,
                               ImagenAlmacenadaRepository repositorio) {
        this.directorio = Paths.get(ruta).toAbsolutePath().normalize();
        this.repositorio = repositorio;
    }

    @PostConstruct
    void prepararDirectorio() throws IOException {
        Files.createDirectories(directorio);
    }

    @Override
    public String guardar(ImagenAdjunta imagen) {
        String extension = RecodificadorImagenes.extension(imagen.tipoContenido());
        byte[] original = imagen.contenido();
        if (original == null || original.length == 0 || original.length > RecodificadorImagenes.MAXIMO_BYTES) {
            throw new IllegalArgumentException("La imagen esta vacia o supera los 5 MB");
        }
        byte[] sinMetadatos = RecodificadorImagenes.recodificar(original, extension);
        String nombre = UUID.randomUUID() + "." + extension;
        try {
            Files.write(directorio.resolve(nombre), sinMetadatos);
        } catch (IOException e) {
            // El disco es solo una copia rapida; la BD es la fuente de verdad.
        }
        repositorio.save(new ImagenAlmacenada(nombre, sinMetadatos));
        return nombre;
    }

    @Override
    public Optional<byte[]> leer(String nombreGuardado) {
        if (!esNombreValido(nombreGuardado)) {
            return Optional.empty();
        }
        try {
            Path archivo = rutaSegura(nombreGuardado);
            if (Files.isRegularFile(archivo)) {
                return Optional.of(Files.readAllBytes(archivo));
            }
        } catch (IOException e) {
            // Se cae a la copia persistente.
        }
        return repositorio.findById(nombreGuardado).map(ImagenAlmacenada::getContenido);
    }

    @Override
    public boolean existe(String nombreGuardado) {
        return esNombreValido(nombreGuardado)
            && (Files.isRegularFile(rutaSegura(nombreGuardado)) || repositorio.existsById(nombreGuardado));
    }

    @Override
    public void eliminar(String nombreGuardado) {
        if (!esNombreValido(nombreGuardado)) {
            return;
        }
        try {
            Files.deleteIfExists(rutaSegura(nombreGuardado));
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo eliminar la imagen del disco", e);
        }
        if (repositorio.existsById(nombreGuardado)) {
            repositorio.deleteById(nombreGuardado);
        }
    }

    private static boolean esNombreValido(String nombreGuardado) {
        return nombreGuardado != null && RecodificadorImagenes.NOMBRE_GENERADO.matcher(nombreGuardado).matches();
    }

    /** Solo nombres generados por este almacen; evita cualquier intento de salir del directorio. */
    private Path rutaSegura(String nombreGuardado) {
        Path archivo = directorio.resolve(nombreGuardado).normalize();
        if (!archivo.startsWith(directorio)) {
            return directorio.resolve("no-existe");
        }
        return archivo;
    }
}
