package com.periodismo.seguro.autenticacion.internal;

import com.periodismo.seguro.cuentas.CredencialesCuenta;
import com.periodismo.seguro.cuentas.CuentasApi;
import com.periodismo.seguro.cuentas.Rol;
import java.util.Optional;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/** HU-03: carga la cuenta por correo (via CuentasApi). El nombre de usuario del principal es el id de cuenta. */
@Service
class ServicioUsuarios implements UserDetailsService {
    private final CuentasApi cuentas;
    private final ControlIntentos intentos;
    private final String hashFalso;

    ServicioUsuarios(CuentasApi cuentas, ControlIntentos intentos, PasswordEncoder encoder) {
        this.cuentas = cuentas;
        this.intentos = intentos;
        this.hashFalso = encoder.encode("clave-inexistente");
    }

    @Override
    public UserDetails loadUserByUsername(String correo) {
        boolean bloqueado = intentos.estaBloqueado(correo);
        Optional<CredencialesCuenta> credenciales = cuentas.credencialesPorCorreo(correo);
        if (credenciales.isEmpty()) {
            if (bloqueado) {
                // Un correo inexistente bloqueado se comporta igual que uno real: no se revela si existe.
                return construir("desconocida", hashFalso, Rol.LECTOR, true);
            }
            throw new UsernameNotFoundException("Credenciales invalidas");
        }
        CredencialesCuenta c = credenciales.get();
        return construir(c.cuentaId(), c.claveHash(), c.rol(), bloqueado);
    }

    /** HU-08: el rol sale de la cuenta guardada, no de una constante. */
    private static UserDetails construir(String cuentaId, String hash, Rol rol, boolean bloqueado) {
        return User.withUsername(cuentaId).password(hash).roles(rol.name()).accountLocked(bloqueado).build();
    }
}
