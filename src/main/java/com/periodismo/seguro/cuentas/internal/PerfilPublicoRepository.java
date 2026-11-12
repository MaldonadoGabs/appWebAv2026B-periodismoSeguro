package com.periodismo.seguro.cuentas.internal;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PerfilPublicoRepository extends JpaRepository<PerfilPublico, String> {
    boolean existsBySeudonimo(String seudonimo);
}
