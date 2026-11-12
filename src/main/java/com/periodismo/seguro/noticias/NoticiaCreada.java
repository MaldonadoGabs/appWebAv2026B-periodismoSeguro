package com.periodismo.seguro.noticias;

/** Evento publico de dominio. Hoy viaja en memoria; al separar el servicio pasa a ser un mensaje (cola o broker). */
public record NoticiaCreada(String noticiaId, String categoria) {}
