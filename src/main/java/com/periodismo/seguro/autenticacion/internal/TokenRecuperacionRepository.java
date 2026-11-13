package com.periodismo.seguro.autenticacion.internal;

import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TokenRecuperacionRepository extends JpaRepository<TokenRecuperacion, String> {
    List<TokenRecuperacion> findByCuentaIdAndUsadoFalse(String cuentaId);

    List<TokenRecuperacion> findByCuentaId(String cuentaId);

    /**
     * HU-06: marca el token como usado solo si sigue vigente, en una sola sentencia.
     * Devuelve 1 si se consumio; 0 si no existe, ya se uso o caduco (asi dos envios a la vez no lo usan dos veces).
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update TokenRecuperacion t set t.usado = true "
        + "where t.tokenHash = :hash and t.usado = false and t.caducaEn > :ahora")
    int consumirSiVigente(@Param("hash") String hash, @Param("ahora") Instant ahora);
}
