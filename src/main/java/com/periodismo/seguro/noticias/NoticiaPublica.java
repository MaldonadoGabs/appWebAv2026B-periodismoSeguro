package com.periodismo.seguro.noticias;

import java.util.List;

/**
 * HU-09: lo unico que puede mostrarse de una noticia. No incluye correo, nombre real,
 * IP ni hora exacta; el autor aparece solo por su seudonimo.
 * HU-01: {@code categoria} es el nombre visible de la categoria, o nulo si no tiene.
 * HU-12: {@code publicadaEn} y {@code modificadaEn} son las fechas formateadas (hora redondeada a 1 h, UTC).
 */
public record NoticiaPublica(
    String id,
    String titulo,
    String subtitulo,
    String cuerpo,
    String autorSeudonimo,
    List<String> imagenesIds,
    String categoria,
    String publicadaEn,
    String modificadaEn) {

    /** Constructor para llamadas anteriores a HU-12 que no enviaban fechas. */
    public NoticiaPublica(String id, String titulo, String subtitulo, String cuerpo, String autorSeudonimo,
                          List<String> imagenesIds, String categoria) {
        this(id, titulo, subtitulo, cuerpo, autorSeudonimo, imagenesIds, categoria, null, null);
    }
}
