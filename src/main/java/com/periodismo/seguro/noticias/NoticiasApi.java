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
     * HU-01: toda noticia se guarda con una categoria existente en la tabla del catalogo.
     *
     * @throws IllegalArgumentException si faltan datos, la categoria no existe o una imagen no es valida
     */
    String crear(String autorCuentaId, String autorSeudonimo, SolicitudNoticia solicitud);

    /** HU-09: contenido de una imagen guardada, para servirla desde la capa web. */
    Optional<ArchivoImagen> imagen(String imagenId);

    /** HU-12: consulta por identificador. La usa ya la confirmacion de HU-09. */
    Optional<NoticiaPublica> obtener(String id);

    /** HU-08: true solo si la noticia existe y fue creada por esa cuenta. */
    boolean esAutor(String autorCuentaId, String id);

    /**
     * HU-10: feed paginado, de mas reciente a mas antiguo. {@code pagina} empieza en 0 y
     * {@code tamano} se limita internamente a 50. Devuelve resumenes, nunca entidades ni el cuerpo completo.
     */
    List<ResumenNoticia> listar(int pagina, int tamano);

    /** HU-10: total de noticias, para calcular las paginas del feed. */
    long contar();

    /**
     * HU-01 / HU-08: "mis notas". Solo las noticias de ese autor, con el mismo orden y limites que el feed.
     * Un id de cuenta nulo o desconocido devuelve una lista vacia.
     */
    List<ResumenNoticia> listarDeAutor(String autorCuentaId, int pagina, int tamano);

    /** HU-01 / HU-08: total de noticias de ese autor, para paginar "mis notas". */
    long contarDeAutor(String autorCuentaId);

    /**
     * HU-11: busqueda y filtro de noticias por texto (encabezado o palabras clave del cuerpo)
     * y por categoria. Paginado como el feed, de mas reciente a mas antiguo.
     * Si {@code texto} es nulo o vacio no filtra por texto; si {@code categoria} es nula o vacia
     * no filtra por categoria. Si ambos son vacios, equivale a {@link #listar(int, int)}.
     */
    List<ResumenNoticia> buscar(String texto, String categoria, int pagina, int tamano);

    /** HU-11: total de noticias que coinciden con la busqueda, para calcular las paginas. */
    long contarBusqueda(String texto, String categoria);

    /**
     * HU-13: el autor (o un administrador) actualiza encabezado, subtitulo, cuerpo y categoria.
     * Registra la fecha de modificacion. Devuelve false si la noticia no existe o el autor no coincide.
     *
     * @throws IllegalArgumentException si faltan datos o la categoria no existe
     */
    boolean editar(String autorCuentaId, String id, SolicitudNoticia solicitud);

    /**
     * HU-14: elimina la nota solo con confirmacion explícita y solo si la pide su autor
     * o un administrador. Borra tambien sus imagenes y referencias. Devuelve false si no
     * existe, falta la confirmacion o la cuenta no tiene permiso.
     * El modulo no consulta roles: {@code administrador} lo resuelve la capa de entrada.
     */
    boolean eliminar(String cuentaId, String id, boolean confirmado, boolean administrador);

    /** HU-01: categorias de la tabla de catalogo, en el orden en que se muestran. */
    List<CategoriaNoticia> categorias();
}
