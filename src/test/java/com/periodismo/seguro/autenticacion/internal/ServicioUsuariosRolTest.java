package com.periodismo.seguro.autenticacion.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.periodismo.seguro.cuentas.CredencialesCuenta;
import com.periodismo.seguro.cuentas.CuentasApi;
import com.periodismo.seguro.cuentas.Rol;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/** HU-08: el usuario de Spring Security recibe el rol guardado en la cuenta, no uno fijo. */
class ServicioUsuariosRolTest {

    private static List<String> autoridades(Rol rol) {
        CuentasApi cuentas = mock(CuentasApi.class);
        // Real, con el repositorio simulado: Mockito no puede simular ControlIntentos en este JDK.
        ControlIntentos intentos = new ControlIntentos(mock(IntentoFallidoRepository.class), Clock.systemUTC());
        when(cuentas.credencialesPorCorreo("cuenta@ejemplo.com"))
            .thenReturn(Optional.of(new CredencialesCuenta("id-cuenta", "hash", rol)));
        ServicioUsuarios servicio = new ServicioUsuarios(cuentas, intentos, new BCryptPasswordEncoder());

        UserDetails usuario = servicio.loadUserByUsername("cuenta@ejemplo.com");
        assertEquals("id-cuenta", usuario.getUsername());
        return usuario.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
    }

    @Test
    void elPeriodistaRecibeElRolPeriodista() {
        assertEquals(List.of("ROLE_PERIODISTA"), autoridades(Rol.PERIODISTA));
    }

    @Test
    void elLectorRecibeElRolLectorYNoElDePeriodista() {
        assertEquals(List.of("ROLE_LECTOR"), autoridades(Rol.LECTOR));
    }

    @Test
    void elAdministradorRecibeElRolAdministrador() {
        assertEquals(List.of("ROLE_ADMINISTRADOR"), autoridades(Rol.ADMINISTRADOR));
    }
}
