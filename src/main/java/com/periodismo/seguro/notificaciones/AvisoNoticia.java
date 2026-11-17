package com.periodismo.seguro.notificaciones;

/**
 * HU-21: aviso publico de una noticia recien publicada. Solo viaja el titulo y el seudonimo,
 * nunca correo, nombre real ni identificador de cuenta.
 */
public record AvisoNoticia(String noticiaId, String titulo, String autorSeudonimo) {}
