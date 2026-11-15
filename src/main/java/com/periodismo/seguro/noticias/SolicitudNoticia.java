package com.periodismo.seguro.noticias;

import java.util.List;

/**
 * HU-09: lo que el periodista escribe en el editor. El autor lo resuelve la capa de entrada
 * (id de cuenta y seudonimo), que es quien valido la sesion.
 * La categoria no esta aqui todavia: se anade con la historia de categorias.
 */
public record SolicitudNoticia(String titulo, String subtitulo, String cuerpo, List<ImagenAdjunta> imagenes) {
    public SolicitudNoticia {
        imagenes = imagenes == null ? List.of() : List.copyOf(imagenes);
    }
}
