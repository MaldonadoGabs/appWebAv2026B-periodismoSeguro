package com.periodismo.seguro.autenticacion.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.periodismo.seguro.autenticacion.AutenticacionApi;
import com.periodismo.seguro.cuentas.CuentasApi;
import com.periodismo.seguro.cuentas.SolicitudRegistro;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/** HU-03: verificacion de credenciales con BCrypt y bloqueo temporal tras 5 fallos. */
@SpringBootTest
class AutenticacionServiceTest {
    @Autowired AutenticacionApi auth;
    @Autowired CuentasApi cuentas;
    @Autowired IntentoFallidoRepository intentos;

    private static String correoNuevo() { return "a" + UUID.randomUUID() + "@ejemplo.com"; }

    @Test
    void credencialesCorrectasDevuelvenElIdDeCuenta() {
        String correo = correoNuevo();
        String id = cuentas.registrar(new SolicitudRegistro(correo, "Ana", "Segura123")).cuentaId();

        assertEquals(id, auth.verificarCredenciales(correo, "Segura123").orElseThrow());
    }

    @Test
    void elCorreoSeNormalizaAlVerificar() {
        String correo = correoNuevo();
        cuentas.registrar(new SolicitudRegistro(correo, "Ana", "Segura123"));

        assertTrue(auth.verificarCredenciales("  " + correo.toUpperCase() + " ", "Segura123").isPresent());
    }

    @Test
    void claveIncorrectaYCorreoInexistenteFallanIgual() {
        String correo = correoNuevo();
        cuentas.registrar(new SolicitudRegistro(correo, "Ana", "Segura123"));

        assertTrue(auth.verificarCredenciales(correo, "Incorrecta1").isEmpty());
        assertTrue(auth.verificarCredenciales(correoNuevo(), "Segura123").isEmpty());
    }

    @Test
    void tras5FallosSeBloqueaIncluidaLaClaveCorrecta() {
        String correo = correoNuevo();
        cuentas.registrar(new SolicitudRegistro(correo, "Ana", "Segura123"));

        for (int i = 0; i < 5; i++) {
            assertTrue(auth.verificarCredenciales(correo, "Incorrecta1").isEmpty());
        }
        assertTrue(auth.verificarCredenciales(correo, "Segura123").isEmpty(), "bloqueada con la clave correcta");
    }

    @Test
    void cuatroFallosNoBloqueanYUnExitoReiniciaElContador() {
        String correo = correoNuevo();
        cuentas.registrar(new SolicitudRegistro(correo, "Ana", "Segura123"));

        for (int i = 0; i < 4; i++) {
            auth.verificarCredenciales(correo, "Incorrecta1");
        }
        assertTrue(auth.verificarCredenciales(correo, "Segura123").isPresent());
        assertTrue(intentos.findById(ControlIntentos.clave(correo)).isEmpty(), "contador reiniciado");
    }

    @Test
    void elBloqueoCaducaConElTiempo() {
        String correo = correoNuevo();
        cuentas.registrar(new SolicitudRegistro(correo, "Ana", "Segura123"));
        for (int i = 0; i < 5; i++) {
            auth.verificarCredenciales(correo, "Incorrecta1");
        }
        IntentoFallido intento = intentos.findById(ControlIntentos.clave(correo)).orElseThrow();
        intento.setBloqueadoHasta(Instant.now().minusSeconds(1));
        intentos.save(intento);

        assertTrue(auth.verificarCredenciales(correo, "Segura123").isPresent());
    }

    @Test
    void elBloqueoAplicaTambienACorreosInexistentesSinRevelarlo() {
        String correo = correoNuevo();
        for (int i = 0; i < 5; i++) {
            auth.verificarCredenciales(correo, "Incorrecta1");
        }
        assertTrue(intentos.findById(ControlIntentos.clave(correo)).orElseThrow().getBloqueadoHasta() != null);
    }

    @Test
    void elCorreoNoSeGuardaEnClaroEnLosIntentos() {
        String correo = correoNuevo();
        auth.verificarCredenciales(correo, "Incorrecta1");

        assertFalse(intentos.findAll().stream().anyMatch(i -> i.getClaveCorreo().contains("@")));
    }

    @Test
    void unaSesionDesconocidaOVaciaNoEsValida() {
        assertTrue(auth.validarSesion("no-existe").isEmpty());
        assertTrue(auth.validarSesion(null).isEmpty());
    }
}
