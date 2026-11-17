package com.periodismo.seguro.interacciones.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "comentarios_noticias")
class ComentarioNoticia {
    @Id
    private String id;
    @Column(nullable = false)
    private String noticiaId;
    @Column(nullable = false)
    private String cuentaId;
    @Column(name = "autor_seudonimo", nullable = false)
    private String autorSeudonimo;
    @Column(nullable = false, length = 500)
    private String texto;
    @Column(nullable = false)
    private Instant creadaEn;

    protected ComentarioNoticia() {}

    ComentarioNoticia(String id, String noticiaId, String cuentaId, String autorSeudonimo,
                      String texto, Instant creadaEn) {
        this.id = id;
        this.noticiaId = noticiaId;
        this.cuentaId = cuentaId;
        this.autorSeudonimo = autorSeudonimo;
        this.texto = texto;
        this.creadaEn = creadaEn;
    }

    String getId() { return id; }
    String getNoticiaId() { return noticiaId; }
    String getAutorSeudonimo() { return autorSeudonimo; }
    String getTexto() { return texto; }
    Instant getCreadaEn() { return creadaEn; }
}
