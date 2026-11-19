package com.periodismo.seguro.chat.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Historial de la sala (HU-19). Guarda solo el seudonimo publico, el texto y la hora con precision de minuto:
 * nunca el id de cuenta, el correo ni el nombre real (HU-26).
 */
@Entity
@Table(name = "mensajes_chat")
public class MensajeGuardado {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "autor_seudonimo", nullable = false)
    private String autorSeudonimo;

    @Column(nullable = false, length = 500)
    private String texto;

    @Column(name = "enviado_en", nullable = false)
    private Instant enviadoEn;

    protected MensajeGuardado() {}

    public MensajeGuardado(String autorSeudonimo, String texto, Instant enviadoEn) {
        this.autorSeudonimo = autorSeudonimo;
        this.texto = texto;
        this.enviadoEn = enviadoEn;
    }

    public Long getId() { return id; }
    public String getAutorSeudonimo() { return autorSeudonimo; }
    public String getTexto() { return texto; }
    public Instant getEnviadoEn() { return enviadoEn; }
}
