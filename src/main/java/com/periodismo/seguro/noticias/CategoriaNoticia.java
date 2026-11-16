package com.periodismo.seguro.noticias;

/**
 * HU-16: una categoria del catalogo cerrado. El slug es el identificador estable que se guarda
 * en la noticia y se usa en las URLs; el nombre es solo para mostrar.
 */
public record CategoriaNoticia(String slug, String nombre) {}
