package com.periodismo.seguro.entrada;

import com.periodismo.seguro.cuentas.ReglasClave;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Formulario para definir una nueva clave con el enlace de recuperacion (HU-06). Mismas reglas que el registro. */
public class RestablecerForm {
    private String token;

    @NotBlank(message = "La contraseña es obligatoria")
    @Pattern(regexp = ReglasClave.PATRON_FORMULARIO, message = ReglasClave.MENSAJE)
    private String clave;

    @NotBlank(message = "Confirma la contraseña")
    private String confirmacion;

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getClave() { return clave; }
    public void setClave(String clave) { this.clave = clave; }
    public String getConfirmacion() { return confirmacion; }
    public void setConfirmacion(String confirmacion) { this.confirmacion = confirmacion; }

    @Override
    public String toString() { return "RestablecerForm[]"; }
}
