package com.periodismo.seguro.cuentas.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.periodismo.seguro.cuentas.CuentasApi;
import com.periodismo.seguro.cuentas.ResultadoRegistro;
import com.periodismo.seguro.cuentas.SolicitudRegistro;
import jakarta.persistence.Table;
import java.lang.reflect.Field;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/** HU-02: la identidad privada y el perfil publico viven separados. */
@SpringBootTest
class IdentidadSeparadaTest {
    private static final Set<String> PRIVADOS = Set.of("correo", "nombreReal", "claveHash", "clave", "hash");

    @Autowired CuentasApi api;
    @Autowired IdentidadPrivadaRepository identidades;
    @Autowired PerfilPublicoRepository perfiles;

    private static List<String> campos(Class<?> tipo) {
        return Arrays.stream(tipo.getDeclaredFields()).map(Field::getName).toList();
    }

    @Test
    void elPerfilPublicoNoTieneCamposDeIdentidadPrivada() {
        assertTrue(campos(PerfilPublico.class).stream().noneMatch(PRIVADOS::contains));
    }

    @Test
    void laIdentidadPrivadaNoGuardaElSeudonimo() {
        assertFalse(campos(IdentidadPrivada.class).contains("seudonimo"));
    }

    @Test
    void usanTablasDistintas() {
        String privada = IdentidadPrivada.class.getAnnotation(Table.class).name();
        String publica = PerfilPublico.class.getAnnotation(Table.class).name();
        assertEquals("identidades_privadas", privada);
        assertEquals("perfiles_publicos", publica);
    }

    @Test
    void losRecordsDeLaApiPublicaNoExponenCorreoNiNombreReal() {
        for (Class<?> tipo : new Class<?>[] {ResultadoRegistro.class}) {
            List<String> nombres = Arrays.stream(tipo.getRecordComponents()).map(RecordComponent::getName).toList();
            assertTrue(nombres.stream().noneMatch(PRIVADOS::contains), nombres.toString());
        }
    }

    @Test
    void registrarCreaAmbosRegistrosUnidosPorElMismoId() {
        String correo = "u" + UUID.randomUUID() + "@ejemplo.com";
        ResultadoRegistro r = api.registrar(new SolicitudRegistro(correo, "Ana Pérez", "Segura123"));

        assertEquals(r.cuentaId(), identidades.findByCorreo(correo).orElseThrow().getCuentaId());
        assertEquals(r.seudonimo(), perfiles.findById(r.cuentaId()).orElseThrow().getSeudonimo());
    }

    @Test
    void elSeudonimoPublicoNoContieneElNombreRealNiElCorreo() {
        ResultadoRegistro r = api.registrar(
            new SolicitudRegistro("zzqq" + UUID.randomUUID() + "@ejemplo.com", "Xylophon Quimera", "Segura123"));

        assertFalse(r.seudonimo().toLowerCase().contains("xylophon"));
        assertFalse(r.seudonimo().contains("@"));
    }
}
