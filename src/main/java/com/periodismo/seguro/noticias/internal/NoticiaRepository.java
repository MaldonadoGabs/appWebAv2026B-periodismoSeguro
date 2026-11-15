package com.periodismo.seguro.noticias.internal;

import org.springframework.data.jpa.repository.JpaRepository;

interface NoticiaRepository extends JpaRepository<Noticia, String> {}
