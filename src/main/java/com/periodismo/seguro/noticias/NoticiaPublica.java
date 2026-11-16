package com.periodismo.seguro.noticias;

import java.util.List;

/**
 * HU-09: lo unico que puede mostrarse de una noticia. No incluye correo, nombre real,
 * IP ni hora exacta; el autor aparece solo por su seudonimo.
 * HU-01: {@code categoria} es el nombre visible de la categoria, o nulo si no tiene.
 */
public record NoticiaPublica(
    String id,
    String titulo,
    String subtitulo,
    String cuerpo,
    String autorSeudonimo,
    List<String> imagenesIds,
    String categoria) {}
