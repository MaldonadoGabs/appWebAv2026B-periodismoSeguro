package com.periodismo.seguro.autenticacion.internal;

import com.periodismo.seguro.cuentas.CredencialesCuenta;
import com.periodismo.seguro.cuentas.CuentasApi;
import com.periodismo.seguro.cuentas.ReglasClave;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * HU-05: genera un token aleatorio de un solo uso que caduca en 30 minutos y lo guarda con hash.
 * Si el correo no tiene cuenta no se hace nada visible: la respuesta al usuario es siempre la misma.
 * HU-06: valida el token (existe, no usado, no caducado), lo invalida al usarlo y pide a cuentas el cambio de clave.
 */
@Service
class RecuperacionClave {
    static final Duration CADUCIDAD = Duration.ofMinutes(30);
    private static final int BYTES_TOKEN = 32;

    private final CuentasApi cuentas;
    private final TokenRecuperacionRepository tokens;
    private final EnvioRecuperacion envio;
    private final Clock reloj;
    private final String urlPublica;
    private final SecureRandom azar = new SecureRandom();

    RecuperacionClave(CuentasApi cuentas, TokenRecuperacionRepository tokens, EnvioRecuperacion envio, Clock reloj,
                      @Value("${app.url-publica:http://localhost:${server.port:8080}}") String urlPublica) {
        this.cuentas = cuentas;
        this.tokens = tokens;
        this.envio = envio;
        this.reloj = reloj;
        this.urlPublica = urlPublica.endsWith("/") ? urlPublica.substring(0, urlPublica.length() - 1) : urlPublica;
    }

    @Transactional
    void solicitar(String correo) {
        // El token se genera siempre para que el trabajo sea parecido exista o no la cuenta.
        String token = nuevoToken();
        Optional<CredencialesCuenta> cuenta = cuentas.credencialesPorCorreo(correo);
        if (cuenta.isEmpty()) {
            return;
        }
        String cuentaId = cuenta.get().cuentaId();
        // Solo vale el ultimo enlace pedido: los anteriores quedan invalidados.
        tokens.findByCuentaIdAndUsadoFalse(cuentaId).forEach(TokenRecuperacion::marcarUsado);
        tokens.save(new TokenRecuperacion(hash(token), cuentaId, reloj.instant().plus(CADUCIDAD)));
        envio.enviar(urlPublica + "/restablecer?token=" + token);
    }

    /** HU-06: true si el token existe, no se ha usado y no ha caducado. No modifica nada. */
    @Transactional(readOnly = true)
    boolean vigente(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        return tokens.findById(hash(token))
            .filter(t -> !t.isUsado() && reloj.instant().isBefore(t.getCaducaEn()))
            .isPresent();
    }

    /**
     * HU-06: consume el token y cambia la clave. Devuelve el id de la cuenta, o vacio si el token no era valido.
     * Si la clave no cumple las reglas lanza IllegalArgumentException sin gastar el token.
     */
    @Transactional
    Optional<String> restablecer(String token, String nuevaClave) {
        if (!ReglasClave.cumple(nuevaClave)) {
            throw new IllegalArgumentException(ReglasClave.MENSAJE);
        }
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        String h = hash(token);
        if (tokens.consumirSiVigente(h, reloj.instant()) != 1) {
            return Optional.empty();
        }
        String cuentaId = tokens.findById(h).orElseThrow().getCuentaId();
        // Cualquier otro enlace pendiente de la misma cuenta deja de servir.
        tokens.findByCuentaIdAndUsadoFalse(cuentaId).forEach(TokenRecuperacion::marcarUsado);
        if (!cuentas.cambiarClave(cuentaId, nuevaClave)) {
            // La cuenta ya no existe: se revierte para no dejar el token gastado a medias.
            throw new IllegalStateException("Cuenta inexistente para un token vigente");
        }
        return Optional.of(cuentaId);
    }

    private String nuevoToken() {
        byte[] bytes = new byte[BYTES_TOKEN];
        azar.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** SHA-256 en hexadecimal. Basta para un token aleatorio de 256 bits y permite buscarlo por su hash. */
    static String hash(String token) {
        try {
            byte[] h = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(h);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
