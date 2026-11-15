package com.periodismo.seguro.entrada;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** HU-09: campos de texto del editor. Las imagenes llegan aparte, como archivos multipart. */
class NoticiaForm {
    @NotBlank(message = "Escribe el encabezado")
    @Size(max = 180, message = "El encabezado admite hasta 180 caracteres")
    private String titulo;

    @NotBlank(message = "Escribe el subtitulo")
    @Size(max = 300, message = "El subtitulo admite hasta 300 caracteres")
    private String subtitulo;

    @NotBlank(message = "Escribe el texto de la noticia")
    @Size(max = 50000, message = "El texto es demasiado largo")
    private String cuerpo;

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getSubtitulo() { return subtitulo; }
    public void setSubtitulo(String subtitulo) { this.subtitulo = subtitulo; }
    public String getCuerpo() { return cuerpo; }
    public void setCuerpo(String cuerpo) { this.cuerpo = cuerpo; }
}
