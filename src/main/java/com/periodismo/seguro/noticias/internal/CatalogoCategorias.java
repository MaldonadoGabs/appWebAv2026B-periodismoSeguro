package com.periodismo.seguro.noticias.internal;

import com.periodismo.seguro.noticias.CategoriaNoticia;
import java.util.List;
import java.util.Optional;

/**
 * HU-16 / HU-01: catalogo cerrado de categorias. Es una lista fija en codigo (no una tabla) porque
 * cambia poco y asi no hace falta carga inicial ni migraciones. Los ids son slugs y son los que se guardan.
 */
final class CatalogoCategorias {
    static final List<CategoriaNoticia> TODAS = List.of(
        new CategoriaNoticia("politica", "Política"),
        new CategoriaNoticia("economia", "Economía"),
        new CategoriaNoticia("derechos-humanos", "Derechos Humanos"),
        new CategoriaNoticia("sociedad", "Sociedad"),
        new CategoriaNoticia("seguridad-justicia", "Seguridad y Justicia"),
        new CategoriaNoticia("medio-ambiente", "Medio Ambiente"),
        new CategoriaNoticia("cultura", "Cultura"),
        new CategoriaNoticia("internacional", "Internacional"),
        new CategoriaNoticia("otros", "Otros"));

    private CatalogoCategorias() {}

    static Optional<CategoriaNoticia> buscar(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return TODAS.stream().filter(c -> c.id().equals(id)).findFirst();
    }

    /** Nombre visible de una categoria guardada, o nulo si la noticia no tiene categoria. */
    static String nombreDe(String id) {
        return buscar(id).map(CategoriaNoticia::nombre).orElse(null);
    }
}
