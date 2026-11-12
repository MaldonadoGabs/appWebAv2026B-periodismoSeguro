package com.periodismo.seguro.autenticacion.internal;

import com.periodismo.seguro.autenticacion.AutenticacionApi;
import com.periodismo.seguro.cuentas.CuentasApi;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
class AutenticacionService implements AutenticacionApi {
    private final CuentasApi cuentas;

    AutenticacionService(CuentasApi cuentas) { this.cuentas = cuentas; }

    /** Fail-closed: sin implementacion no se inicia sesion. */
    @Override
    public Optional<String> iniciarSesion(String seudonimo, String clave) { return Optional.empty(); }

    @Override
    public void cerrarSesion(String sesionId) {}

    /** Fail-closed: ninguna sesion es valida hasta implementar HU-03. */
    @Override
    public Optional<String> validarSesion(String sesionId) { return Optional.empty(); }

    @Override
    public void solicitarRecuperacion(String seudonimo) {}

    @Override
    public boolean restablecerClave(String token, String nuevaClave) { return false; }
}
