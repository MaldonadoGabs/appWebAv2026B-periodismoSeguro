package com.periodismo.seguro.cuentas.internal;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IdentidadPrivadaRepository extends JpaRepository<IdentidadPrivada, String> {
    boolean existsByCorreo(String correo);

    Optional<IdentidadPrivada> findByCorreo(String correo);
}
