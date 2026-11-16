package com.periodismo.seguro.cuentas.internal;

import com.periodismo.seguro.cuentas.Rol;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
    /** HU-08. Nullable para no romper filas creadas antes del rol: esas cuentas se leen como PERIODISTA. */
    @Enumerated(EnumType.STRING)
    @Column(name = "rol")
    private Rol rol;

    protected IdentidadPrivada() {}

    public IdentidadPrivada(String cuentaId, String correo, String nombreReal, String claveHash, Rol rol) {
        this.cuentaId = cuentaId;
        this.correo = correo;
        this.nombreReal = nombreReal;
        this.claveHash = claveHash;
        this.rol = rol;
    }

    public String getCuentaId() { return cuentaId; }
    public String getCorreo() { return correo; }
    public String getNombreReal() { return nombreReal; }
    public String getClaveHash() { return claveHash; }
    public Rol getRol() { return rol == null ? Rol.PERIODISTA : rol; }

    /** HU-06: solo recibe un hash ya calculado, nunca la clave en claro. */
    public void cambiarClaveHash(String claveHash) { this.claveHash = claveHash; }
}
