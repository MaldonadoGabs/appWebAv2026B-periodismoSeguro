package com.periodismo.seguro.chat;

import java.util.List;

/** Epica WebSockets: HU-17 conexion autenticada, HU-18 sala general, HU-19 historial, HU-20 conectados, HU-22 no leidos. */
public interface ChatApi {
    /** HU-17: solo con sesion valida. */
    boolean conectar(String sesionId);

    /** HU-18. */
    void enviar(String sesionId, String texto);

    /** HU-19. */
    List<String> historial(int limite);

    /** HU-20. */
    List<String> usuariosConectados();

    /** HU-22. */
    int mensajesSinLeer(String cuentaId);
}
