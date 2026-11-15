package com.periodismo.seguro.noticias.internal;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface ImagenNoticiaRepository extends JpaRepository<ImagenNoticia, String> {
    List<ImagenNoticia> findByNoticiaIdOrderByOrdenAsc(String noticiaId);

    /** HU-10: miniatura del feed. */
    Optional<ImagenNoticia> findFirstByNoticiaIdOrderByOrdenAsc(String noticiaId);
}
