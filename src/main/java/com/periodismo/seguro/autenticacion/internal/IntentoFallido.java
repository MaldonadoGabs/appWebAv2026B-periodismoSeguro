package com.periodismo.seguro.autenticacion.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * HU-03: intentos fallidos consecutivos por correo. La clave es el hash SHA-256 del correo
 * (HU-26: el correo no se guarda en claro fuera del modulo cuentas).
 */
@Entity
@Table(name = "intentos_login")
public class IntentoFallido {
    @Id
    @Column(name = "clave_correo", length = 64)
    private String claveCorreo;
    @Column(nullable = false)
    private int fallos;
    @Column(name = "bloqueado_hasta")
    private Instant bloqueadoHasta;

    protected IntentoFallido() {}

    public IntentoFallido(String claveCorreo) { this.claveCorreo = claveCorreo; }

    public String getClaveCorreo() { return claveCorreo; }
    public int getFallos() { return fallos; }
    public void setFallos(int fallos) { this.fallos = fallos; }
    public Instant getBloqueadoHasta() { return bloqueadoHasta; }
    public void setBloqueadoHasta(Instant bloqueadoHasta) { this.bloqueadoHasta = bloqueadoHasta; }
}
