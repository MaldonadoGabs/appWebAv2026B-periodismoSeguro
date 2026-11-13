package com.periodismo.seguro.cuentas.internal;

import com.periodismo.seguro.cuentas.CorreoYaRegistradoException;
import com.periodismo.seguro.cuentas.CredencialesCuenta;
import com.periodismo.seguro.cuentas.CuentasApi;
import com.periodismo.seguro.cuentas.ReglasClave;
import com.periodismo.seguro.cuentas.ResultadoRegistro;
import com.periodismo.seguro.cuentas.SolicitudRegistro;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class CuentasService implements CuentasApi {
    private static final int INTENTOS = 5;

    private final IdentidadPrivadaRepository identidades;
    private final PerfilPublicoRepository perfiles;
    private final PasswordEncoder encoder;
    private final GeneradorSeudonimo generador;

    CuentasService(IdentidadPrivadaRepository identidades, PerfilPublicoRepository perfiles,
                   PasswordEncoder encoder, GeneradorSeudonimo generador) {
        this.identidades = identidades;
        this.perfiles = perfiles;
        this.encoder = encoder;
        this.generador = generador;
    }

    @Override
    public ResultadoRegistro registrar(SolicitudRegistro solicitud) {
        String correo = solicitud.correo().trim().toLowerCase(Locale.ROOT);
        if (identidades.existsByCorreo(correo)) {
            throw new CorreoYaRegistradoException();
        }
        String hash = encoder.encode(solicitud.clave());
        String cuentaId = UUID.randomUUID().toString();
        PerfilPublico perfil = guardarPerfil(cuentaId);
        try {
            identidades.saveAndFlush(new IdentidadPrivada(cuentaId, correo, solicitud.nombreReal().trim(), hash));
        } catch (DataIntegrityViolationException e) {
            perfiles.deleteById(cuentaId);
            throw new CorreoYaRegistradoException();
        }
        return new ResultadoRegistro(cuentaId, perfil.getSeudonimo());
    }

    @Override
    public Optional<CredencialesCuenta> credencialesPorCorreo(String correo) {
        if (correo == null) {
            return Optional.empty();
        }
        return identidades.findByCorreo(correo.trim().toLowerCase(Locale.ROOT))
            .map(i -> new CredencialesCuenta(i.getCuentaId(), i.getClaveHash()));
    }

    @Override
    @Transactional
    public boolean cambiarClave(String cuentaId, String nuevaClave) {
        if (!ReglasClave.cumple(nuevaClave)) {
            throw new IllegalArgumentException(ReglasClave.MENSAJE);
        }
        if (cuentaId == null) {
            return false;
        }
        return identidades.findById(cuentaId)
            .map(identidad -> {
                identidad.cambiarClaveHash(encoder.encode(nuevaClave));
                identidades.save(identidad);
                return true;
            })
            .orElse(false);
    }

    private PerfilPublico guardarPerfil(String cuentaId) {
        for (int i = 0; i < INTENTOS; i++) {
            try {
                return perfiles.saveAndFlush(new PerfilPublico(cuentaId, seudonimoLibre()));
            } catch (DataIntegrityViolationException e) {
                // Carrera por el mismo seudonimo: se reintenta con otro.
            }
        }
        throw new IllegalStateException("No se pudo generar un seudonimo unico");
    }

    @Override
    public Optional<String> seudonimoDe(String cuentaId) {
        return perfiles.findById(cuentaId).map(PerfilPublico::getSeudonimo);
    }

    private String seudonimoLibre() {
        String candidato;
        do {
            candidato = generador.nuevo();
        } while (perfiles.existsBySeudonimo(candidato));
        return candidato;
    }
}
