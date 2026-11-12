package com.periodismo.seguro.cuentas.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;

/** Datos propios del modulo cuentas. Nunca sale de este paquete: la API publica usa records. */
@Entity
@Table(name = "cuentas", uniqueConstraints = {
    @UniqueConstraint(columnNames = "correo"),
    @UniqueConstraint(columnNames = "seudonimo")
})
public class Cuenta {
    @Id
    private String id;
    @Column(nullable = false)
    private String correo;
    @Column(name = "nombre_real", nullable = false)
    private String nombreReal;
    @Column(name = "clave_hash", nullable = false)
    private String claveHash;
    @Column(nullable = false)
    private String seudonimo;

    protected Cuenta() {}

    public Cuenta(String correo, String nombreReal, String claveHash, String seudonimo) {
        this.id = UUID.randomUUID().toString();
        this.correo = correo;
        this.nombreReal = nombreReal;
        this.claveHash = claveHash;
        this.seudonimo = seudonimo;
    }

    public String getId() { return id; }
    public String getCorreo() { return correo; }
    public String getNombreReal() { return nombreReal; }
    public String getClaveHash() { return claveHash; }
    public String getSeudonimo() { return seudonimo; }
}
