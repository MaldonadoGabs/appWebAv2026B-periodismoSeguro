package com.periodismo.seguro.noticias;

import java.util.List;

/**
 * HU-09: lo unico que puede mostrarse de una noticia. No incluye correo, nombre real,
 * IP ni hora exacta; el autor aparece solo por su seudonimo.
 */
public record NoticiaPublica(
    String id,
    String titulo,
    String subtitulo,
    String cuerpo,
    String autorSeudonimo,
    List<String> imagenesIds,
    String categoriaNombre) {}
