package com.periodismo.seguro.chat.internal;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MensajeChatRepository extends JpaRepository<MensajeGuardado, Long> {
    /** Los mas recientes primero; el servicio los invierte para entregarlos en orden cronologico. */
    List<MensajeGuardado> findAllByOrderByIdDesc(Pageable pagina);
}
