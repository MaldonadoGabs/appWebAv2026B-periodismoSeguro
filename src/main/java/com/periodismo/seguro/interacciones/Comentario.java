package com.periodismo.seguro.interacciones;

/** Comentario publico de una noticia. Solo expone el seudonimo del lector, nunca su cuenta real. */
public record Comentario(String id, String noticiaId, String autorSeudonimo, String texto, String hora) {}
