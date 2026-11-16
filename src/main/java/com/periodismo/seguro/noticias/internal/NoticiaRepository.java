package com.periodismo.seguro.noticias.internal;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface NoticiaRepository extends JpaRepository<Noticia, String> {
    /** HU-10: id como segundo criterio porque la hora esta truncada a 1 h y puede haber empates. */
    List<Noticia> findAllByOrderByCreadaEnDescIdAsc(Pageable pageable);

    /** HU-01 / HU-08: "mis notas", con el mismo criterio de orden que el feed. */
    List<Noticia> findAllByAutorCuentaIdOrderByCreadaEnDescIdAsc(String autorCuentaId, Pageable pageable);

    long countByAutorCuentaId(String autorCuentaId);

    /** HU-11: busca por texto en encabezado o cuerpo (LIKE insensible) filtrando por categoria. */
    @Query("""
        SELECT n FROM Noticia n
        WHERE (:texto IS NULL OR LOWER(n.titulo) LIKE LOWER(CONCAT('%', :texto, '%'))
               OR LOWER(n.cuerpo) LIKE LOWER(CONCAT('%', :texto, '%')))
          AND (:categoria IS NULL OR n.categoria = :categoria)
        ORDER BY n.creadaEn DESC, n.id ASC
        """)
    List<Noticia> buscar(@Param("texto") String texto, @Param("categoria") String categoria, Pageable pageable);

    /** HU-11: cuenta los resultados de una busqueda. */
    @Query("""
        SELECT COUNT(n) FROM Noticia n
        WHERE (:texto IS NULL OR LOWER(n.titulo) LIKE LOWER(CONCAT('%', :texto, '%'))
               OR LOWER(n.cuerpo) LIKE LOWER(CONCAT('%', :texto, '%')))
          AND (:categoria IS NULL OR n.categoria = :categoria)
        """)
    long contarBusqueda(@Param("texto") String texto, @Param("categoria") String categoria);
}
