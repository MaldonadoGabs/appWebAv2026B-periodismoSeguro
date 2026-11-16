package com.periodismo.seguro.noticias.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * HU-16: tabla propia de categorias. El catalogo lo carga data.sql, no se crea desde la aplicacion:
 * asi el mismo script vale para H2 en local y para PostgreSQL en Azure.
 */
@Entity
@Table(name = "categorias")
public class Categoria {
    @Id
    @Column(length = 40)
    private String slug;

    @Column(nullable = false, length = 80)
    private String nombre;

    @Column(nullable = false)
    private int orden;

    protected Categoria() {}

    public Categoria(String slug, String nombre, int orden) {
        this.slug = slug;
        this.nombre = nombre;
        this.orden = orden;
    }

    public String getSlug() { return slug; }
    public String getNombre() { return nombre; }
    public int getOrden() { return orden; }
}
