package com.periodismo.seguro.noticias.internal;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface ImagenNoticiaRepository extends JpaRepository<ImagenNoticia, String> {
    List<ImagenNoticia> findByNoticiaIdOrderByOrdenAsc(String noticiaId);
}
