package com.periodismo.seguro.autenticacion.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.periodismo.seguro.autenticacion.AutenticacionApi;
import com.periodismo.seguro.cuentas.CuentasApi;
import com.periodismo.seguro.cuentas.SolicitudRegistro;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

/** HU-05: token aleatorio de un solo uso, con hash, caducidad de 30 minutos y envio simulado sin datos reales. */
@SpringBootTest
@ExtendWith(OutputCaptureExtension.class)
class RecuperacionClaveTest {
    private static final Pattern ENLACE = Pattern.compile("/restablecer\\?token=([A-Za-z0-9_-]+)");

    @Autowired AutenticacionApi auth;
    @Autowired CuentasApi cuentas;
    @Autowired TokenRecuperacionRepository tokens;

    private static String correoNuevo() { return "r" + UUID.randomUUID() + "@ejemplo.com"; }

    private static List<String> tokensImpresos(CapturedOutput salida) {
        List<String> encontrados = new ArrayList<>();
        Matcher m = ENLACE.matcher(salida.getAll());
        while (m.find()) {
            encontrados.add(m.group(1));
        }
        return encontrados;
    }

    @Test
    void unaCuentaExistenteRecibeUnEnlaceYSoloSeGuardaElHashDelToken(CapturedOutput salida) {
        String correo = correoNuevo();
        String id = cuentas.registrar(new SolicitudRegistro(correo, "Ana", "Segura123")).cuentaId();

        auth.solicitarRecuperacion(correo);

        List<String> impresos = tokensImpresos(salida);
        assertEquals(1, impresos.size(), "se imprime un enlace");
        String token = impresos.get(0);
        TokenRecuperacion guardado = tokens.findById(RecuperacionClave.hash(token)).orElseThrow();
        assertEquals(id, guardado.getCuentaId());
        assertFalse(guardado.isUsado());
        assertTrue(tokens.findById(token).isEmpty(), "el token en claro no es la clave guardada");
        assertTrue(tokens.findAll().stream().noneMatch(t -> t.getTokenHash().equals(token)));
    }

    @Test
    void elTokenCaducaEn30Minutos() {
        String correo = correoNuevo();
        String id = cuentas.registrar(new SolicitudRegistro(correo, "Ana", "Segura123")).cuentaId();
        Instant antes = Instant.now();

        auth.solicitarRecuperacion(correo);

        Instant caduca = tokens.findByCuentaId(id).get(0).getCaducaEn();
        Instant despues = Instant.now();
        assertFalse(caduca.isBefore(antes.plus(Duration.ofMinutes(30))));
        assertFalse(caduca.isAfter(despues.plus(Duration.ofMinutes(30))));
    }

    @Test
    void unCorreoInexistenteNoGeneraTokenNiEnlace(CapturedOutput salida) {
        long total = tokens.count();

        auth.solicitarRecuperacion(correoNuevo());

        assertEquals(total, tokens.count());
        assertTrue(tokensImpresos(salida).isEmpty());
    }

    @Test
    void elEnlaceImpresoNoIncluyeElCorreoNiElNombreReal(CapturedOutput salida) {
        String correo = correoNuevo();
        String id = cuentas.registrar(new SolicitudRegistro(correo, "Xylophon Quimera", "Segura123")).cuentaId();

        auth.solicitarRecuperacion(correo);

        assertFalse(tokensImpresos(salida).isEmpty());
        assertFalse(salida.getAll().contains(correo), "el correo no aparece en consola");
        assertFalse(salida.getAll().contains("Xylophon"), "el nombre real no aparece en consola");
        assertFalse(salida.getAll().contains(id), "el id de cuenta no aparece en consola");
    }

    @Test
    void cadaSolicitudGeneraUnTokenDistintoYAnulaElAnterior(CapturedOutput salida) {
        String correo = correoNuevo();
        String id = cuentas.registrar(new SolicitudRegistro(correo, "Ana", "Segura123")).cuentaId();

        auth.solicitarRecuperacion(correo);
        auth.solicitarRecuperacion(correo);

        List<String> impresos = tokensImpresos(salida);
        assertEquals(2, impresos.size());
        assertNotEquals(impresos.get(0), impresos.get(1));
        assertTrue(tokens.findById(RecuperacionClave.hash(impresos.get(0))).orElseThrow().isUsado(), "anterior anulado");
        assertFalse(tokens.findById(RecuperacionClave.hash(impresos.get(1))).orElseThrow().isUsado(), "nuevo vigente");
        assertEquals(1, tokens.findByCuentaIdAndUsadoFalse(id).size());
    }

    @Test
    void elTokenEsLargoYAleatorio(CapturedOutput salida) {
        String correo = correoNuevo();
        cuentas.registrar(new SolicitudRegistro(correo, "Ana", "Segura123"));

        auth.solicitarRecuperacion(correo);

        assertTrue(tokensImpresos(salida).get(0).length() >= 43, "al menos 256 bits en base64url");
    }

    @Test
    void elCorreoSeNormalizaAlSolicitar(CapturedOutput salida) {
        String correo = correoNuevo();
        cuentas.registrar(new SolicitudRegistro(correo, "Ana", "Segura123"));

        auth.solicitarRecuperacion("  " + correo.toUpperCase() + " ");

        assertEquals(1, tokensImpresos(salida).size());
    }
}
