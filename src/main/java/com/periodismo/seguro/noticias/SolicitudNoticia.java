package com.periodismo.seguro.noticias;

import java.util.List;

/**
 * HU-09 y HU-16: lo que el periodista escribe en el editor. El autor lo resuelve la capa de entrada
 * (id de cuenta y seudonimo), que es quien valido la sesion.
 * Toda noticia pertenece a una categoria: si no se indica, se usa {@link #CATEGORIA_OTROS}.
 */
public record SolicitudNoticia(
    String titulo,
    String subtitulo,
    String cuerpo,
    List<ImagenAdjunta> imagenes,
    String categoriaSlug) {

    public static final String CATEGORIA_OTROS = "otros";

    /** Sin categoria explicita, la nota queda en 'Otros' (siempre pertenece a alguna). */
    public SolicitudNoticia(String titulo, String subtitulo, String cuerpo, List<ImagenAdjunta> imagenes) {
        this(titulo, subtitulo, cuerpo, imagenes, CATEGORIA_OTROS);
    }

    public SolicitudNoticia {
        imagenes = imagenes == null ? List.of() : List.copyOf(imagenes);
        categoriaSlug = (categoriaSlug == null || categoriaSlug.isBlank())
            ? CATEGORIA_OTROS : categoriaSlug.trim();
    }
}
