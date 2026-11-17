package com.periodismo.seguro.noticias;

/**
 * Evento publico de dominio. Hoy viaja en memoria; al separar el servicio pasa a ser un mensaje (cola o broker).
 * HU-21: lleva titulo y seudonimo para que el aviso no tenga que consultar la noticia de nuevo.
 */
public record NoticiaCreada(String noticiaId, String titulo, String autorSeudonimo, String categoria) {}
