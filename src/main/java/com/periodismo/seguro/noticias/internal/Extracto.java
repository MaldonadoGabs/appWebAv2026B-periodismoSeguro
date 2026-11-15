package com.periodismo.seguro.noticias.internal;

import java.util.regex.Pattern;

/**
 * HU-10: convierte el cuerpo ya saneado en texto plano para el feed.
 * El HTML de entrada paso por la lista blanca de la capa de entrada, y aqui solo se quitan
 * etiquetas: el resultado se muestra siempre con th:text, asi que no puede ejecutarse nada.
 * Se hace con JDK, sin meter librerias de terceros en el modulo noticias.
 */
final class Extracto {
    static final int MAXIMO_CARACTERES = 220;
    private static final Pattern ETIQUETAS = Pattern.compile("<[^>]*>");
    private static final Pattern ESPACIOS = Pattern.compile("\\s+");

    private Extracto() {}

    static String de(String html) {
        if (html == null) {
            return "";
        }
        String texto = ETIQUETAS.matcher(html).replaceAll(" ");
        texto = texto.replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&apos;", "'");
        texto = ESPACIOS.matcher(texto).replaceAll(" ").trim();
        if (texto.length() > MAXIMO_CARACTERES) {
            return texto.substring(0, MAXIMO_CARACTERES).trim() + "...";
        }
        return texto;
    }
}
