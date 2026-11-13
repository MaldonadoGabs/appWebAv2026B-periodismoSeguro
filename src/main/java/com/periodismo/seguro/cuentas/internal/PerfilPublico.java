package com.periodismo.seguro.cuentas.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/** HU-02: unico dato de la cuenta que puede mostrarse en publicaciones y chat: el seudonimo. */
@Entity
@Table(name = "perfiles_publicos", uniqueConstraints = @UniqueConstraint(columnNames = "seudonimo"))
public class PerfilPublico {
    @Id
    private String cuentaId;
    @Column(nullable = false)
    private String seudonimo;

    protected PerfilPublico() {}

    public PerfilPublico(String cuentaId, String seudonimo) {
        this.cuentaId = cuentaId;
        this.seudonimo = seudonimo;
    }

    public String getCuentaId() { return cuentaId; }
    public String getSeudonimo() { return seudonimo; }
}
