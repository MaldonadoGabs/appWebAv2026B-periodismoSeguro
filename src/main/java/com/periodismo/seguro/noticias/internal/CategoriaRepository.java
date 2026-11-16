package com.periodismo.seguro.noticias.internal;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface CategoriaRepository extends JpaRepository<Categoria, String> {
    /** HU-16: orden del catalogo definido en data.sql. */
    List<Categoria> findAllByOrderByOrdenAsc();
}
