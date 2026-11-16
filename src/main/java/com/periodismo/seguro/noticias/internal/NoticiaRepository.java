package com.periodismo.seguro.noticias.internal;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface NoticiaRepository extends JpaRepository<Noticia, String> {
    /** HU-10: id como segundo criterio porque la hora esta truncada a 1 h y puede haber empates. */
    List<Noticia> findAllByOrderByCreadaEnDescIdAsc(Pageable pageable);

    /** HU-01 / HU-08: "mis notas", con el mismo criterio de orden que el feed. */
    List<Noticia> findAllByAutorCuentaIdOrderByCreadaEnDescIdAsc(String autorCuentaId, Pageable pageable);

    long countByAutorCuentaId(String autorCuentaId);
}
