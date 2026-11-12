package com.periodismo.seguro.cuentas;

/** Lo unico que sale del modulo tras registrar: el id de cuenta y el seudonimo publico. */
public record ResultadoRegistro(String cuentaId, String seudonimo) {}
