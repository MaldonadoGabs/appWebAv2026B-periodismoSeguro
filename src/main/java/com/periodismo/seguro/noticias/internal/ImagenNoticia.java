package com.periodismo.seguro.noticias.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * HU-09: referencia a una imagen de la noticia. Los bytes viven en la copia persistente (y en
 * disco local como cache); aqui solo se guarda el nombre generado (UUID), sin el nombre original
 * ni metadatos que puedan identificar al periodista.
 * Se relaciona por id, sin claves foraneas, como pide la arquitectura modular.
 */
@Entity
@Table(name = "imagenes_noticias")
public class ImagenNoticia {
    @Id
    private String id;

    @Column(nullable = false)
    private String noticiaId;

    @Column(nullable = false)
    private String nombreGuardado;

    @Column(nullable = false)
    private String tipoContenido;

    @Column(nullable = false)
    private int orden;

    protected ImagenNoticia() {}

    public ImagenNoticia(String id, String noticiaId, String nombreGuardado, String tipoContenido, int orden) {
        this.id = id;
        this.noticiaId = noticiaId;
        this.nombreGuardado = nombreGuardado;
        this.tipoContenido = tipoContenido;
        this.orden = orden;
    }

    public String getId() { return id; }
    public String getNoticiaId() { return noticiaId; }
    public String getNombreGuardado() { return nombreGuardado; }
    public String getTipoContenido() { return tipoContenido; }
    public int getOrden() { return orden; }
}
