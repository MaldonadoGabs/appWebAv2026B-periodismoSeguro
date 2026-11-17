package com.periodismo.seguro.interacciones.internal;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface ReaccionNoticiaRepository extends JpaRepository<ReaccionNoticia, String> {
    List<ReaccionNoticia> findByNoticiaId(String noticiaId);
}
