package com.periodismo.seguro.chat;

/** Aviso de que un participante esta escribiendo, o de que dejo de hacerlo. */
public record EstadoEscritura(String usuario, boolean activo) {}
