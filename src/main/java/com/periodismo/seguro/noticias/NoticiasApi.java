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
     * HU-09 y HU-16: guarda la noticia con encabezado, subtitulo, texto, imagenes y categoria,
     * y devuelve su id. Quien llama (la capa de entrada) ya valido la sesion, resolvio el
     * seudonimo y limpio el HTML. La categoria debe existir en el catalogo.
     *
     * @throws IllegalArgumentException si faltan datos, la categoria no existe o una imagen no es valida
     */
    String crear(String autorCuentaId, String autorSeudonimo, SolicitudNoticia solicitud);

    /** HU-09: contenido de una imagen guardada, para servirla desde la capa web. */
    Optional<ArchivoImagen> imagen(String imagenId);

    /** HU-12: consulta por identificador. La usa ya la confirmacion de HU-09. */
    Optional<NoticiaPublica> obtener(String id);

    /**
     * HU-10: feed paginado, de mas reciente a mas antiguo. {@code pagina} empieza en 0 y
     * {@code tamano} se limita internamente a 50. Devuelve resumenes, nunca entidades ni el cuerpo completo.
     */
    List<ResumenNoticia> listar(int pagina, int tamano);

    /** HU-10: total de noticias, para calcular las paginas del feed. */
    long contar();

    /** HU-11: busqueda y filtro. */
    List<String> buscar(String texto, String categoria);

    /** HU-13: solo el autor edita su noticia. */
    boolean editar(String autorCuentaId, String id, String cuerpo);

    /** HU-14: requiere confirmacion. */
    boolean eliminar(String autorCuentaId, String id, boolean confirmado);

    /** HU-16: catalogo de categorias en el orden definido en data.sql. */
    List<CategoriaNoticia> categorias();
}
