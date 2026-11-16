package com.periodismo.seguro.noticias.internal;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** HU-01: catalogo persistido. Solo lo usa el modulo noticias. */
interface CategoriaNoticiaRepository extends JpaRepository<CategoriaNoticiaEntity, String> {
    List<CategoriaNoticiaEntity> findAllByOrderByOrdenAsc();
}
