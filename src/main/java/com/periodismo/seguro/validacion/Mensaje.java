package com.periodismo.seguro.validacion;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/** Datos sinteticos de validacion del stack. Sin IP ni identidad; hora redondeada a 1 h. */
@Entity
public class Mensaje {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) Long id;
    @Column(length = 500) String texto;
    Instant hora;

    protected Mensaje() {}
    public Mensaje(String texto) {
        this.texto = texto;
        this.hora = Instant.now().truncatedTo(ChronoUnit.HOURS);
    }
    public Long getId() { return id; }
    public String getTexto() { return texto; }
    public Instant getHora() { return hora; }
}
