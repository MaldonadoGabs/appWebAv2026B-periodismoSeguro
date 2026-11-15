package com.periodismo.seguro.entrada;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Component;

/**
 * HU-09: el texto llega como HTML de un contenteditable. Se reduce a una lista blanca
 * antes de guardarlo: sin script, sin atributos de eventos, sin estilos y sin imagenes
 * incrustadas (estas ultimas se suben como archivos propios, nunca desde otros dominios).
 * La sanitizacion vive en la capa de entrada, que es donde se reciben datos del navegador.
 */
@Component
class SanitizadorHtml {
    private static final Safelist PERMITIDOS = Safelist.basic()
        .addTags("h2", "h3")
        .addEnforcedAttribute("a", "rel", "nofollow noopener noreferrer");

    private static final Document.OutputSettings SALIDA = new Document.OutputSettings().prettyPrint(false);

    String limpiar(String html) {
        return html == null ? "" : Jsoup.clean(html, "", PERMITIDOS, SALIDA);
    }
}
