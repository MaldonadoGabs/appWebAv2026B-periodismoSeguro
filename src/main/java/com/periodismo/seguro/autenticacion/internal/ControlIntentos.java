package com.periodismo.seguro.autenticacion.internal;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** HU-03: bloqueo temporal tras 5 intentos fallidos consecutivos. Aplica igual a correos que no existen. */
@Service
class ControlIntentos {
    static final int MAXIMO_FALLOS = 5;
    static final Duration BLOQUEO = Duration.ofMinutes(15);

    private final IntentoFallidoRepository repo;
    private final Clock reloj;

    ControlIntentos(IntentoFallidoRepository repo, Clock reloj) {
        this.repo = repo;
        this.reloj = reloj;
    }

    @Transactional(readOnly = true)
    boolean estaBloqueado(String correo) {
        Instant ahora = reloj.instant();
        return repo.findById(clave(correo))
            .map(i -> i.getBloqueadoHasta() != null && ahora.isBefore(i.getBloqueadoHasta()))
            .orElse(false);
    }

    @Transactional
    void registrarFallo(String correo) {
        Instant ahora = reloj.instant();
        IntentoFallido intento = repo.findById(clave(correo)).orElseGet(() -> new IntentoFallido(clave(correo)));
        if (intento.getBloqueadoHasta() != null && !ahora.isBefore(intento.getBloqueadoHasta())) {
            intento.setFallos(0);
            intento.setBloqueadoHasta(null);
        }
        intento.setFallos(intento.getFallos() + 1);
        if (intento.getFallos() >= MAXIMO_FALLOS) {
            intento.setBloqueadoHasta(ahora.plus(BLOQUEO));
        }
        repo.save(intento);
    }

    @Transactional
    void registrarExito(String correo) {
        repo.deleteById(clave(correo));
    }

    static String clave(String correo) {
        String normalizado = correo == null ? "" : correo.trim().toLowerCase(Locale.ROOT);
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(normalizado.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
