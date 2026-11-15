package com.periodismo.seguro.noticias;

/** HU-09: bytes de una imagen ya guardada, sin exponer la ruta del disco a la capa web. */
public record ArchivoImagen(String tipoContenido, byte[] contenido) {}
