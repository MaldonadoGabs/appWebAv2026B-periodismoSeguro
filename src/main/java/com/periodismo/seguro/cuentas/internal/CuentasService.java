package com.periodismo.seguro.cuentas.internal;

import com.periodismo.seguro.cuentas.CorreoYaRegistradoException;
import com.periodismo.seguro.cuentas.CuentasApi;
import com.periodismo.seguro.cuentas.ResultadoRegistro;
import com.periodismo.seguro.cuentas.SolicitudRegistro;
import java.util.Locale;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
class CuentasService implements CuentasApi {
    private static final int INTENTOS = 5;

    private final CuentaRepository repo;
    private final PasswordEncoder encoder;
    private final GeneradorSeudonimo generador;

    CuentasService(CuentaRepository repo, PasswordEncoder encoder, GeneradorSeudonimo generador) {
        this.repo = repo;
        this.encoder = encoder;
        this.generador = generador;
    }

    @Override
    public ResultadoRegistro registrar(SolicitudRegistro solicitud) {
        String correo = solicitud.correo().trim().toLowerCase(Locale.ROOT);
        if (repo.existsByCorreo(correo)) {
            throw new CorreoYaRegistradoException();
        }
        String hash = encoder.encode(solicitud.clave());
        for (int i = 0; i < INTENTOS; i++) {
            String seudonimo = seudonimoLibre();
            Cuenta cuenta = new Cuenta(correo, solicitud.nombreReal().trim(), hash, seudonimo);
            try {
                repo.saveAndFlush(cuenta);
                return new ResultadoRegistro(cuenta.getId(), seudonimo);
            } catch (DataIntegrityViolationException e) {
                // Carrera por el mismo correo: es duplicado. Si no, fue el seudonimo y se reintenta.
                if (repo.existsByCorreo(correo)) {
                    throw new CorreoYaRegistradoException();
                }
            }
        }
        throw new IllegalStateException("No se pudo generar un seudonimo unico");
    }

    @Override
    public Optional<String> seudonimoDe(String cuentaId) {
        return repo.findById(cuentaId).map(Cuenta::getSeudonimo);
    }

    private String seudonimoLibre() {
        String candidato;
        do {
            candidato = generador.nuevo();
        } while (repo.existsBySeudonimo(candidato));
        return candidato;
    }
}
