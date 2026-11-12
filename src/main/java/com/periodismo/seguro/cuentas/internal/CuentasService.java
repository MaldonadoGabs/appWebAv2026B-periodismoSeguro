package com.periodismo.seguro.cuentas.internal;

import com.periodismo.seguro.cuentas.CuentasApi;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
class CuentasService implements CuentasApi {
    @Override
    public String registrar(String seudonimo, String clave) {
        throw new UnsupportedOperationException("HU-01 pendiente");
    }

    @Override
    public Optional<String> seudonimoDe(String cuentaId) { return Optional.empty(); }
}
