package com.periodismo.seguro.cuentas;

import java.util.regex.Pattern;

/**
 * Reglas de contrasena compartidas por el registro (HU-01) y el restablecimiento (HU-06):
 * minimo 8 caracteres, con mayuscula, minuscula y numero.
 */
public final class ReglasClave {
    /** Expresion usable en {@code @Pattern}; acepta vacio para que {@code @NotBlank} de su propio mensaje. */
    public static final String PATRON_FORMULARIO = "^$|^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$";
    public static final String MENSAJE = "Mínimo 8 caracteres, con mayúscula, minúscula y número";

    private static final Pattern PATRON = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$");

    private ReglasClave() {}

    public static boolean cumple(String clave) {
        return clave != null && PATRON.matcher(clave).matches();
    }
}
