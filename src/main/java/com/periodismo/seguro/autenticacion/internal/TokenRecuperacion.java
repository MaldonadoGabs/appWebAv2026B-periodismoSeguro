package com.periodismo.seguro.autenticacion.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * HU-05: token de recuperacion de clave. Solo se guarda el hash SHA-256 del token, nunca el token en claro.
 * Se refiere a la cuenta por id (sin clave foranea hacia el modulo cuentas).
 */
@Entity
@Table(name = "tokens_recuperacion")
public class TokenRecuperacion {
    @Id
    @Column(name = "token_hash", length = 64)
    private String tokenHash;
    @Column(name = "cuenta_id", nullable = false, length = 36)
    private String cuentaId;
    @Column(name = "caduca_en", nullable = false)
    private Instant caducaEn;
    @Column(nullable = false)
    private boolean usado;

    protected TokenRecuperacion() {}

    public TokenRecuperacion(String tokenHash, String cuentaId, Instant caducaEn) {
        this.tokenHash = tokenHash;
        this.cuentaId = cuentaId;
        this.caducaEn = caducaEn;
    }

    public String getTokenHash() { return tokenHash; }
    public String getCuentaId() { return cuentaId; }
    public Instant getCaducaEn() { return caducaEn; }
    public boolean isUsado() { return usado; }
    public void marcarUsado() { this.usado = true; }

    @Override
    public String toString() { return "TokenRecuperacion[caducaEn=" + caducaEn + ", usado=" + usado + "]"; }
}
