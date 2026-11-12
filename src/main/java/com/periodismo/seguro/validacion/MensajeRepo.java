package com.periodismo.seguro.validacion;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MensajeRepo extends JpaRepository<Mensaje, Long> {
    List<Mensaje> findTop20ByOrderByIdDesc();
}
