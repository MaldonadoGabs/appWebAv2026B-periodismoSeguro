package com.periodismo.seguro.cuentas;

/**
 * HU-08: roles de la plataforma. El registro publico solo crea cuentas PERIODISTA;
 * LECTOR y ADMINISTRADOR existen como roles validos, pero no se pueden elegir desde el formulario.
 */
public enum Rol {
    PERIODISTA,
    LECTOR,
    ADMINISTRADOR
}
