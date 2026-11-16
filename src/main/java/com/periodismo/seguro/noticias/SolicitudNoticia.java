package com.periodismo.seguro.noticias;

import java.util.List;

/**
 * HU-09: lo que el periodista escribe en el editor. El autor lo resuelve la capa de entrada
 * (id de cuenta y seudonimo), que es quien valido la sesion.
 * HU-01: {@code categoria} es el id de una categoria de la tabla ({@link NoticiasApi#categorias()}).
 * Es obligatoria: el formulario la exige y el servicio la valida contra la tabla.
 */
public record SolicitudNoticia(String titulo, String subtitulo, String cuerpo, String categoria,
                               List<ImagenAdjunta> imagenes) {
    public SolicitudNoticia {
        imagenes = imagenes == null ? List.of() : List.copyOf(imagenes);
    }
}
