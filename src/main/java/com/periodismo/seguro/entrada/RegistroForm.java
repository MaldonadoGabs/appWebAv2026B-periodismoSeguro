package com.periodismo.seguro.entrada;

import com.periodismo.seguro.cuentas.ReglasClave;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Formulario de registro (HU-01). Los errores se muestran junto a cada campo. */
public class RegistroForm {
    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "El correo no tiene un formato válido")
    @Pattern(regexp = "^$|.+@.+\\..+", message = "El correo no tiene un formato válido")
    private String correo;

    @NotBlank(message = "El nombre real es obligatorio")
    @Size(max = 100, message = "Máximo 100 caracteres")
    private String nombreReal;

    @NotBlank(message = "La contraseña es obligatoria")
    @Pattern(regexp = ReglasClave.PATRON_FORMULARIO, message = ReglasClave.MENSAJE)
    private String clave;

    @NotBlank(message = "Confirma la contraseña")
    private String confirmacion;

    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }
    public String getNombreReal() { return nombreReal; }
    public void setNombreReal(String nombreReal) { this.nombreReal = nombreReal; }
    public String getClave() { return clave; }
    public void setClave(String clave) { this.clave = clave; }
    public String getConfirmacion() { return confirmacion; }
    public void setConfirmacion(String confirmacion) { this.confirmacion = confirmacion; }
}
