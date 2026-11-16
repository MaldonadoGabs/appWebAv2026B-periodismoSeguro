package com.periodismo.seguro.noticias;

/**
 * HU-10: una entrada del feed. Es un contrato de solo lectura: trae un extracto en texto plano,
 * nunca el cuerpo completo, y solo el seudonimo del autor (sin correo, nombre real, IP ni id de cuenta).
 * La fecha llega ya redondeada a 1 h, como pide HU-26.
 * HU-01: {@code categoria} es el nombre visible de la categoria, o nulo si no tiene.
 */
public record ResumenNoticia(
    String id,
    String titulo,
    String subtitulo,
    String extracto,
    String autorSeudonimo,
    String publicadaEn,
    String primeraImagenId,
    String categoria) {}
