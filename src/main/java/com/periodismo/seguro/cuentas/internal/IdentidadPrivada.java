package com.periodismo.seguro.cuentas.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/** HU-02: datos reales y privados (correo, nombre real, hash). No contiene el seudonimo ni sale de este paquete. */
@Entity
@Table(name = "identidades_privadas", uniqueConstraints = @UniqueConstraint(columnNames = "correo"))
public class IdentidadPrivada {
    /** Mismo valor que PerfilPublico.cuentaId; es una referencia por id, sin clave foranea. */
    @Id
    private String cuentaId;
    @Column(nullable = false)
    private String correo;
    @Column(name = "nombre_real", nullable = false)
    private String nombreReal;
    @Column(name = "clave_hash", nullable = false)
    private String claveHash;

    protected IdentidadPrivada() {}

    public IdentidadPrivada(String cuentaId, String correo, String nombreReal, String claveHash) {
        this.cuentaId = cuentaId;
        this.correo = correo;
        this.nombreReal = nombreReal;
        this.claveHash = claveHash;
    }

    public String getCuentaId() { return cuentaId; }
    public String getCorreo() { return correo; }
    public String getNombreReal() { return nombreReal; }
    public String getClaveHash() { return claveHash; }

    /** HU-06: solo recibe un hash ya calculado, nunca la clave en claro. */
    public void cambiarClaveHash(String claveHash) { this.claveHash = claveHash; }
}
