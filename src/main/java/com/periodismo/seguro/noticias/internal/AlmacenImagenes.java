package com.periodismo.seguro.noticias.internal;

import com.periodismo.seguro.noticias.ImagenAdjunta;
import java.util.Optional;

/**
 * HU-09: puerto de almacenamiento de imagenes. El dominio no sabe donde viven los bytes:
 * hoy se guardan en BD con copia local en disco, y migrar a almacenamiento en la nube
 * es una sola clase nueva, sin tocar NoticiasService.
 */
interface AlmacenImagenes {
    /** Guarda una imagen valida y devuelve el nombre generado (nunca el nombre original). */
    String guardar(ImagenAdjunta imagen);

    /** Lee los bytes de un nombre generado por este almacen. Vacio si no existe o no es valido. */
    Optional<byte[]> leer(String nombreGuardado);

    /** HU-10: true si la imagen sigue disponible en disco o en la copia persistente. */
    boolean existe(String nombreGuardado);

    /** HU-14: borra el archivo al eliminar la nota. No hace nada si ya no esta. */
    void eliminar(String nombreGuardado);
}
