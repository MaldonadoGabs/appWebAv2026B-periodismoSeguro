package com.periodismo.seguro.chat;

import java.util.List;
import java.util.function.Consumer;

/** Callbacks que la sala usa para entregar mensajes, conectados, ingresos y escritura a una conexion. */
public record SuscripcionChat(
    Consumer<MensajeChat> mensajes,
    Consumer<List<String>> conectados,
    Consumer<String> ingresos,
    Consumer<EstadoEscritura> escribiendo) {
}
