package com.periodismo.seguro.interacciones.internal;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface ComentarioNoticiaRepository extends JpaRepository<ComentarioNoticia, String> {
    List<ComentarioNoticia> findByNoticiaIdOrderByCreadaEnAscIdAsc(String noticiaId);
}
