package com.periodismo.seguro.noticias.internal;

import com.periodismo.seguro.noticias.CategoriaNoticia;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * HU-01: catalogo leido de la tabla categorias_noticias, cargada por data.sql.
 * No hay lista fija en codigo: para dar de alta o cambiar categorias se usa el script.
 */
@Component
class CatalogoCategorias {
    private final CategoriaNoticiaRepository categorias;

    CatalogoCategorias(CategoriaNoticiaRepository categorias) { this.categorias = categorias; }

    List<CategoriaNoticia> todas() {
        return categorias.findAllByOrderByOrdenAsc().stream()
            .map(c -> new CategoriaNoticia(c.getId(), c.getNombre()))
            .toList();
    }

    boolean existe(String id) {
        return id != null && categorias.existsById(id);
    }

    Optional<CategoriaNoticia> buscar(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return categorias.findById(id).map(c -> new CategoriaNoticia(c.getId(), c.getNombre()));
    }

    /** Nombre visible de una categoria guardada en la tabla. */
    String nombreDe(String id) {
        return buscar(id).map(CategoriaNoticia::nombre).orElse(null);
    }
}
