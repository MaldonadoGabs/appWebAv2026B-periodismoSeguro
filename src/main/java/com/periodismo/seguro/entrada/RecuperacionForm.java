package com.periodismo.seguro.entrada;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Formulario de solicitud de recuperacion de clave (HU-05). Mismas reglas de correo que el registro. */
public class RecuperacionForm {
    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "El correo no tiene un formato válido")
    @Pattern(regexp = "^$|.+@.+\\..+", message = "El correo no tiene un formato válido")
    private String correo;

    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }
}
