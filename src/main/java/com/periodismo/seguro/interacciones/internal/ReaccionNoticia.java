package com.periodismo.seguro.interacciones.internal;

import com.periodismo.seguro.interacciones.TipoReaccion;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "reacciones_noticias")
class ReaccionNoticia {
    @Id
    private String id;
    @Column(nullable = false)
    private String cuentaId;
    @Column(nullable = false)
    private String noticiaId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoReaccion tipo;

    protected ReaccionNoticia() {}

    ReaccionNoticia(String id, String cuentaId, String noticiaId, TipoReaccion tipo) {
        this.id = id;
        this.cuentaId = cuentaId;
        this.noticiaId = noticiaId;
        this.tipo = tipo;
    }

    TipoReaccion getTipo() { return tipo; }
}
