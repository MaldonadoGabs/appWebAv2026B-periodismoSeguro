package com.periodismo.seguro.noticias;

import java.util.List;

/**
 * HU-09: lo que el periodista escribe en el editor. El autor lo resuelve la capa de entrada
 * (id de cuenta y seudonimo), que es quien valido la sesion.
 * HU-01: {@code categoria} es el id del catalogo ({@link NoticiasApi#categorias()}). El formulario lo exige;
 * la API lo acepta vacio solo para llamadas anteriores a HU-01, y si llega debe existir en el catalogo.
 */
public record SolicitudNoticia(String titulo, String subtitulo, String cuerpo, String categoria,
                               List<ImagenAdjunta> imagenes) {
    public SolicitudNoticia {
        imagenes = imagenes == null ? List.of() : List.copyOf(imagenes);
    }

    /** Llamadas sin categoria (anteriores a HU-01). */
    public SolicitudNoticia(String titulo, String subtitulo, String cuerpo, List<ImagenAdjunta> imagenes) {
        this(titulo, subtitulo, cuerpo, null, imagenes);
    }
}
