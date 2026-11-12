package com.periodismo.seguro.cuentas;

public class CorreoYaRegistradoException extends RuntimeException {
    public CorreoYaRegistradoException() {
        super("correo ya registrado");
    }
}
