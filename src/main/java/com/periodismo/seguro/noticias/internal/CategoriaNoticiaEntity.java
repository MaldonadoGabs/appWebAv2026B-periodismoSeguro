package com.periodismo.seguro.noticias.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * HU-01: categoria almacenada en la tabla propia del modulo. El id es el slug que se guarda
 * en cada noticia y el orden es el de despliegue en el formulario. Nunca expone datos de cuentas.
 */
@Entity
@Table(name = "categorias_noticias")
public class CategoriaNoticiaEntity {
    @Id
    private String id;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private int orden;

    protected CategoriaNoticiaEntity() {}

    public CategoriaNoticiaEntity(String id, String nombre, int orden) {
        this.id = id;
        this.nombre = nombre;
        this.orden = orden;
    }

    public String getId() { return id; }
    public String getNombre() { return nombre; }
    public int getOrden() { return orden; }
}
