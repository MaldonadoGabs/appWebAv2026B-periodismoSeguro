package com.periodismo.seguro.noticias;

import java.util.List;
import java.util.Optional;

/**
 * Epica CRUD de noticias: HU-09 a HU-14 y HU-16 categorias.
 * No depende de otros modulos: guarda el seudonimo del autor junto a la noticia (dato propio),
 * y avisa de lo ocurrido publicando el evento {@link NoticiaCreada}.
 */
public interface NoticiasApi {
    /**
     * HU-09: guarda la noticia con encabezado, subtitulo, texto e imagenes, y devuelve su id.
     * Quien llama (la capa de entrada) ya valido la sesion, resolvio el seudonimo y limpio el HTML.
     *
     * @throws IllegalArgumentException si faltan datos o una imagen no es valida
     */
    String crear(String autorCuentaId, String autorSeudonimo, SolicitudNoticia solicitud);

    /** HU-09: contenido de una imagen guardada, para servirla desde la capa web. */
    Optional<ArchivoImagen> imagen(String imagenId);

    /** HU-12: consulta por identificador. La usa ya la confirmacion de HU-09. */
    Optional<NoticiaPublica> obtener(String id);

    /** HU-10: listado paginado. */
    List<String> listar(int pagina, int tamano);

    /** HU-11: busqueda y filtro. */
    List<String> buscar(String texto, String categoria);

    /** HU-13: solo el autor edita su noticia. */
    boolean editar(String autorCuentaId, String id, String cuerpo);

    /** HU-14: requiere confirmacion. */
    boolean eliminar(String autorCuentaId, String id, boolean confirmado);

    /** HU-16. */
    List<String> categorias();
}
