package com.periodismo.seguro.chat;

import java.time.Instant;

/**
 * Mensaje de la sala. Solo expone el seudonimo del autor, nunca su cuenta, correo ni nombre real.
 * La hora llega con precision de minuto: no se guardan segundos que revelen patrones de actividad.
 */
public record MensajeChat(String autorSeudonimo, String texto, Instant enviadoEn) {}
