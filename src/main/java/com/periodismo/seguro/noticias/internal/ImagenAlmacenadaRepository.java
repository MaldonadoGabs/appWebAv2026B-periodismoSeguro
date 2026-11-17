package com.periodismo.seguro.noticias.internal;

import org.springframework.data.jpa.repository.JpaRepository;

/** Copia de los bytes de las imagenes, independiente del disco local. */
interface ImagenAlmacenadaRepository extends JpaRepository<ImagenAlmacenada, String> {
}
