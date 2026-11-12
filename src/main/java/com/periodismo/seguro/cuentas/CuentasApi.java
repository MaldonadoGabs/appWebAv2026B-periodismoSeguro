package com.periodismo.seguro.cuentas;

import java.util.Optional;

/** Epica Registro y autenticacion: HU-01 registro de periodistas, HU-02 identidad real separada del seudonimo. */
public interface CuentasApi {
    /** HU-01 y HU-02: crea la cuenta. La identidad real nunca se mezcla con el seudonimo. */
    String registrar(String seudonimo, String clave);

    Optional<String> seudonimoDe(String cuentaId);
}
