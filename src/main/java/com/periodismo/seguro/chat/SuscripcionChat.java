package com.periodismo.seguro.chat;

import java.util.List;
import java.util.function.Consumer;

/** Callbacks que la sala usa para entregar mensajes y avisos de conectados a una conexion. */
public record SuscripcionChat(Consumer<String> mensajes, Consumer<List<String>> conectados) {
}
