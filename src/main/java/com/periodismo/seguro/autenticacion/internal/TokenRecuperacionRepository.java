package com.periodismo.seguro.autenticacion.internal;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TokenRecuperacionRepository extends JpaRepository<TokenRecuperacion, String> {
    List<TokenRecuperacion> findByCuentaIdAndUsadoFalse(String cuentaId);

    List<TokenRecuperacion> findByCuentaId(String cuentaId);
}
