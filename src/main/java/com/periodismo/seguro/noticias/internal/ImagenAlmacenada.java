package com.periodismo.seguro.noticias.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

/**
 * Copia persistente de una imagen ya saneada. El nombre es el UUID generado al guardarla;
 * nunca se conserva el nombre original ni metadatos que identifiquen al periodista.
 */
@Entity
@Table(name = "imagenes_almacenadas")
public class ImagenAlmacenada {
    @Id
    private String nombreGuardado;

    @Lob
    @Column(nullable = false)
    private byte[] contenido;

    protected ImagenAlmacenada() {}

    public ImagenAlmacenada(String nombreGuardado, byte[] contenido) {
        this.nombreGuardado = nombreGuardado;
        this.contenido = contenido;
    }

    public byte[] getContenido() { return contenido; }
}
