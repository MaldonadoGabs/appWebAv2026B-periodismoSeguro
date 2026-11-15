package com.periodismo.seguro.noticias;

/**
 * HU-09: imagen seleccionada en el editor, ya leida en memoria.
 * No se transporta el nombre original del archivo: podria contener el nombre del periodista.
 */
public record ImagenAdjunta(String tipoContenido, byte[] contenido) {}
