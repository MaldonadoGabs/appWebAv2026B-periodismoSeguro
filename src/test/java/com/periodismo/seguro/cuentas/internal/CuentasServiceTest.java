package com.periodismo.seguro.cuentas.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.periodismo.seguro.cuentas.CorreoYaRegistradoException;
import com.periodismo.seguro.cuentas.CuentasApi;
import com.periodismo.seguro.cuentas.ResultadoRegistro;
import com.periodismo.seguro.cuentas.SolicitudRegistro;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class CuentasServiceTest {
    @Autowired CuentasApi api;
    @Autowired CuentaRepository repo;

    private static String correoNuevo() { return "u" + UUID.randomUUID() + "@ejemplo.com"; }

    @Test
    void guardaLaClaveConHashBcryptYNoEnTextoPlano() {
        String correo = correoNuevo();
        api.registrar(new SolicitudRegistro(correo, "Ana Pérez", "Segura123"));

        Cuenta guardada = repo.findByCorreo(correo).orElseThrow();
        assertNotEquals("Segura123", guardada.getClaveHash());
        assertTrue(guardada.getClaveHash().startsWith("$2"), "debe ser un hash BCrypt");
    }

    @Test
    void generaSeudonimoConFormatoYDistintoParaCadaCuenta() {
        ResultadoRegistro a = api.registrar(new SolicitudRegistro(correoNuevo(), "Ana", "Segura123"));
        ResultadoRegistro b = api.registrar(new SolicitudRegistro(correoNuevo(), "Beto", "Segura123"));

        assertTrue(a.seudonimo().matches("^\\p{L}+ \\p{L}+ \\d{3}$"), a.seudonimo());
        assertNotEquals(a.seudonimo(), b.seudonimo());
    }

    @Test
    void rechazaCorreoRepetidoSinImportarMayusculasNiEspacios() {
        String correo = correoNuevo();
        api.registrar(new SolicitudRegistro(correo, "Ana", "Segura123"));

        assertThrows(CorreoYaRegistradoException.class,
            () -> api.registrar(new SolicitudRegistro("  " + correo.toUpperCase() + " ", "Otra", "Segura123")));
    }

    @Test
    void seudonimoDeDevuelveElSeudonimoDeLaCuenta() {
        ResultadoRegistro r = api.registrar(new SolicitudRegistro(correoNuevo(), "Ana", "Segura123"));

        assertEquals(r.seudonimo(), api.seudonimoDe(r.cuentaId()).orElseThrow());
    }

    @Test
    void elToStringDeLaSolicitudNoFiltraDatos() {
        String texto = new SolicitudRegistro("a@b.com", "Nombre Real", "Segura123").toString();

        assertTrue(!texto.contains("a@b.com") && !texto.contains("Nombre Real") && !texto.contains("Segura123"));
    }
}
