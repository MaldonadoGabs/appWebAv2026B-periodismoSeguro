package com.periodismo.seguro.autenticacion.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.periodismo.seguro.autenticacion.AutenticacionApi;
import com.periodismo.seguro.cuentas.CuentasApi;
import com.periodismo.seguro.cuentas.SolicitudRegistro;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.userdetails.User;

/** HU-06: el token se valida (existe, no usado, no caducado), se invalida al usarse y la clave nueva va con BCrypt. */
@SpringBootTest
@ExtendWith(OutputCaptureExtension.class)
class RestablecerClaveTest {
    private static final Pattern ENLACE = Pattern.compile("/restablecer\\?token=([A-Za-z0-9_-]+)");
    private static final String CLAVE_VIEJA = "Segura123";
    private static final String CLAVE_NUEVA = "Distinta456";

    @Autowired AutenticacionApi auth;
    @Autowired CuentasApi cuentas;
    @Autowired TokenRecuperacionRepository tokens;
    @Autowired SessionRegistry sesiones;

    private static String correoNuevo() { return "t" + UUID.randomUUID() + "@ejemplo.com"; }

    private String registrar(String correo) {
        return cuentas.registrar(new SolicitudRegistro(correo, "Ana", CLAVE_VIEJA)).cuentaId();
    }

    private String pedirToken(String correo, CapturedOutput salida) {
        auth.solicitarRecuperacion(correo);
        Matcher m = ENLACE.matcher(salida.getAll());
        String ultimo = null;
        while (m.find()) {
            ultimo = m.group(1);
        }
        return ultimo;
    }

    @Test
    void conUnTokenVigenteSeCambiaLaClaveYSeGuardaConBcrypt(CapturedOutput salida) {
        String correo = correoNuevo();
        String id = registrar(correo);
        String hashViejo = cuentas.credencialesPorCorreo(correo).orElseThrow().claveHash();
        String token = pedirToken(correo, salida);

        assertTrue(auth.tokenRecuperacionVigente(token));
        assertTrue(auth.restablecerClave(token, CLAVE_NUEVA));

        String hashNuevo = cuentas.credencialesPorCorreo(correo).orElseThrow().claveHash();
        assertNotEquals(hashViejo, hashNuevo);
        assertTrue(hashNuevo.startsWith("$2"), "debe ser un hash BCrypt");
        assertFalse(hashNuevo.contains(CLAVE_NUEVA));
        assertEquals(Optional.of(id), auth.verificarCredenciales(correo, CLAVE_NUEVA));
        assertTrue(auth.verificarCredenciales(correo, CLAVE_VIEJA).isEmpty(), "la clave vieja deja de servir");
    }

    @Test
    void elTokenNoSePuedeReutilizar(CapturedOutput salida) {
        String correo = correoNuevo();
        registrar(correo);
        String token = pedirToken(correo, salida);

        assertTrue(auth.restablecerClave(token, CLAVE_NUEVA));

        assertTrue(tokens.findById(RecuperacionClave.hash(token)).orElseThrow().isUsado());
        assertFalse(auth.tokenRecuperacionVigente(token));
        assertFalse(auth.restablecerClave(token, "OtraMas789"), "un token usado no sirve otra vez");
        assertTrue(auth.verificarCredenciales(correo, "OtraMas789").isEmpty());
    }

    @Test
    void unTokenCaducadoSeRechaza() {
        String correo = correoNuevo();
        String id = registrar(correo);
        String token = "caducado-" + UUID.randomUUID();
        tokens.save(new TokenRecuperacion(RecuperacionClave.hash(token), id, Instant.now().minusSeconds(1)));

        assertFalse(auth.tokenRecuperacionVigente(token));
        assertFalse(auth.restablecerClave(token, CLAVE_NUEVA));
        assertTrue(auth.verificarCredenciales(correo, CLAVE_VIEJA).isPresent(), "la clave no cambia");
    }

    @Test
    void unTokenInexistenteOVacioSeRechaza() {
        assertFalse(auth.tokenRecuperacionVigente("no-existe-" + UUID.randomUUID()));
        assertFalse(auth.tokenRecuperacionVigente(null));
        assertFalse(auth.tokenRecuperacionVigente(""));
        assertFalse(auth.restablecerClave("no-existe-" + UUID.randomUUID(), CLAVE_NUEVA));
        assertFalse(auth.restablecerClave(null, CLAVE_NUEVA));
    }

    @Test
    void unaClaveDebilSeRechazaSinGastarElToken(CapturedOutput salida) {
        String correo = correoNuevo();
        registrar(correo);
        String token = pedirToken(correo, salida);

        for (String debil : new String[] {"corta1A", "sinmayuscula1", "SINMINUSCULA1", "SinNumeros", "", null}) {
            assertThrows(IllegalArgumentException.class, () -> auth.restablecerClave(token, debil), String.valueOf(debil));
        }
        assertTrue(auth.tokenRecuperacionVigente(token), "el token sigue vigente tras una clave invalida");
        assertTrue(auth.verificarCredenciales(correo, CLAVE_VIEJA).isPresent());
    }

    @Test
    void usarUnTokenAnulaLosDemasPendientesDeLaCuenta(CapturedOutput salida) {
        String correo = correoNuevo();
        String id = registrar(correo);
        String token = pedirToken(correo, salida);
        String otro = "otro-" + UUID.randomUUID();
        tokens.save(new TokenRecuperacion(RecuperacionClave.hash(otro), id, Instant.now().plusSeconds(600)));

        assertTrue(auth.restablecerClave(token, CLAVE_NUEVA));

        assertTrue(tokens.findByCuentaIdAndUsadoFalse(id).isEmpty());
        assertFalse(auth.restablecerClave(otro, "OtraMas789"));
    }

    @Test
    void alRestablecerSeCierranLasSesionesAbiertasDeLaCuenta(CapturedOutput salida) {
        String correo = correoNuevo();
        String id = registrar(correo);
        String sesionId = "sesion-" + UUID.randomUUID();
        sesiones.registerNewSession(sesionId, User.withUsername(id).password("x").roles("PERIODISTA").build());
        assertEquals(Optional.of(id), auth.validarSesion(sesionId));

        assertTrue(auth.restablecerClave(pedirToken(correo, salida), CLAVE_NUEVA));

        assertTrue(auth.validarSesion(sesionId).isEmpty(), "la sesion anterior queda expirada");
    }

    @Test
    void niLaNuevaClaveNiElCorreoAparecenEnConsola(CapturedOutput salida) {
        String correo = correoNuevo();
        registrar(correo);
        String clave = "Xq7Unica" + "Clave9";

        assertTrue(auth.restablecerClave(pedirToken(correo, salida), clave));

        assertFalse(salida.getAll().contains(clave));
        assertFalse(salida.getAll().contains(correo));
    }
}
