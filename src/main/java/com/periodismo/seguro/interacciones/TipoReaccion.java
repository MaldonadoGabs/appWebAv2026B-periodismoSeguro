package com.periodismo.seguro.interacciones;

/** Reacciones permitidas en una noticia. El lector puede tener una activa a la vez. */
public enum TipoReaccion {
    ME_GUSTA("Me gusta"),
    IMPACTA("Impacta"),
    IMPORTANTE("Importante");

    private final String etiqueta;

    TipoReaccion(String etiqueta) { this.etiqueta = etiqueta; }

    public String etiqueta() { return etiqueta; }
}
