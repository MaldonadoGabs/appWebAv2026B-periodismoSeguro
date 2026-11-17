package com.periodismo.seguro.noticias.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * HU-09: una noticia. Solo guarda el seudonimo del autor (dato copiado, no consultado a cuentas):
 * nunca el correo, el nombre real ni ningun identificador del lector.
 * La hora se redondea a 1 h, como pide la politica de privacidad (HU-26).
 */
@Entity
@Table(name = "noticias")
public class Noticia {
    @Id
    private String id;

    @Column(nullable = false)
    private String autorCuentaId;

    /** Unico dato del autor que puede mostrarse. */
    @Column(nullable = false)
    private String autorSeudonimo;

    @Column(nullable = false, length = 180)
    private String titulo;

    @Column(nullable = false, length = 300)
    private String subtitulo;

    /** HTML ya saneado por la capa de entrada antes de llegar aqui. */
    @Column(nullable = false, length = 50000)
    private String cuerpo;

    /** HU-01: id (slug) de la categoria de la tabla de catalogo. Toda noticia tiene una. */
    @Column(nullable = false)
    private String categoria;

    @Column(nullable = false)
    private Instant creadaEn;

    /** HU-13: fecha de la ultima edicion. Nula si la noticia nunca se edito. Hora redondeada a 1 h (HU-26). */
    @Column
    private Instant modificadaEn;

    protected Noticia() {}

    public Noticia(String id, String autorCuentaId, String autorSeudonimo, String titulo, String subtitulo,
                   String cuerpo, String categoria, Instant creadaEn) {
        this.id = id;
        this.autorCuentaId = autorCuentaId;
        this.autorSeudonimo = autorSeudonimo;
        this.titulo = titulo;
        this.subtitulo = subtitulo;
        this.cuerpo = cuerpo;
        this.categoria = categoria;
        this.creadaEn = creadaEn;
    }

    public String getId() { return id; }
    public String getAutorCuentaId() { return autorCuentaId; }
    public String getAutorSeudonimo() { return autorSeudonimo; }
    public String getTitulo() { return titulo; }
    public String getSubtitulo() { return subtitulo; }
    public String getCuerpo() { return cuerpo; }
    public String getCategoria() { return categoria; }
    public Instant getCreadaEn() { return creadaEn; }
    public Instant getModificadaEn() { return modificadaEn; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public void setSubtitulo(String subtitulo) { this.subtitulo = subtitulo; }
    public void setCuerpo(String cuerpo) { this.cuerpo = cuerpo; }
    public void setCategoria(String categoria) { this.categoria = categoria; }
    public void setModificadaEn(Instant modificadaEn) { this.modificadaEn = modificadaEn; }
}
