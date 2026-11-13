package com.periodismo.seguro.cuentas.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.periodismo.seguro.cuentas.CuentasApi;
import com.periodismo.seguro.cuentas.ReglasClave;
import com.periodismo.seguro.cuentas.SolicitudRegistro;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

/** HU-06: el modulo cuentas cambia el hash de la clave (BCrypt con sal) y aplica las reglas del registro. */
@SpringBootTest
class CambioClaveTest {
    @Autowired CuentasApi api;
    @Autowired IdentidadPrivadaRepository repo;
    @Autowired PasswordEncoder encoder;

    private String registrar() {
        return api.registrar(new SolicitudRegistro("c" + UUID.randomUUID() + "@ejemplo.com", "Ana", "Segura123")).cuentaId();
    }

    @Test
    void guardaLaNuevaClaveComoHashBcryptConSalDistinta() {
        String id = registrar();

        assertTrue(api.cambiarClave(id, "Distinta456"));
        String primero = repo.findById(id).orElseThrow().getClaveHash();
        assertTrue(api.cambiarClave(id, "Distinta456"));
        String segundo = repo.findById(id).orElseThrow().getClaveHash();

        assertTrue(primero.startsWith("$2"));
        assertNotEquals(primero, segundo, "la sal cambia en cada hash");
        assertTrue(encoder.matches("Distinta456", segundo));
        assertFalse(encoder.matches("Segura123", segundo));
    }

    @Test
    void unaCuentaInexistenteDevuelveFalse() {
        assertFalse(api.cambiarClave(UUID.randomUUID().toString(), "Distinta456"));
        assertFalse(api.cambiarClave(null, "Distinta456"));
    }

    @Test
    void rechazaClavesQueNoCumplenLasReglasDelRegistro() {
        String id = registrar();
        String antes = repo.findById(id).orElseThrow().getClaveHash();

        assertThrows(IllegalArgumentException.class, () -> api.cambiarClave(id, "corta1A"));
        assertThrows(IllegalArgumentException.class, () -> api.cambiarClave(id, "sinmayuscula1"));
        assertThrows(IllegalArgumentException.class, () -> api.cambiarClave(id, null));
        assertEquals(antes, repo.findById(id).orElseThrow().getClaveHash(), "el hash no cambia");
    }

    @Test
    void lasReglasCoincidenConLasDelRegistro() {
        assertTrue(ReglasClave.cumple("Segura123"));
        assertFalse(ReglasClave.cumple("Segura1"));
        assertFalse(ReglasClave.cumple("segura123"));
        assertFalse(ReglasClave.cumple("SEGURA123"));
        assertFalse(ReglasClave.cumple("SeguraSin"));
        assertTrue("".matches(ReglasClave.PATRON_FORMULARIO), "el vacio lo reporta @NotBlank");
        assertTrue("Segura123".matches(ReglasClave.PATRON_FORMULARIO));
        assertFalse("segura123".matches(ReglasClave.PATRON_FORMULARIO));
    }
}
