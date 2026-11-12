package com.periodismo.seguro.cuentas.internal;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CuentaRepository extends JpaRepository<Cuenta, String> {
    boolean existsByCorreo(String correo);

    boolean existsBySeudonimo(String seudonimo);

    Optional<Cuenta> findByCorreo(String correo);
}
