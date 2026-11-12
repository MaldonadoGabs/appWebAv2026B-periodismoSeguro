package com.periodismo.seguro.noticias;

import java.util.List;
import java.util.Optional;

/**
 * Epica CRUD de noticias: HU-09 a HU-14 y HU-16 categorias.
 * No depende de otros modulos: guarda el seudonimo del autor junto a la noticia (dato propio),
 * y avisa de lo ocurrido publicando el evento {@link NoticiaCreada}.
 */
public interface NoticiasApi {
    /** HU-09. Quien llama (la capa de entrada) ya valido la sesion y resolvio el seudonimo. */
    String crear(String autorCuentaId, String autorSeudonimo, String titulo, String cuerpo, String categoria);

    /** HU-10: listado paginado. */
    List<String> listar(int pagina, int tamano);

    /** HU-11: busqueda y filtro. */
    List<String> buscar(String texto, String categoria);

    /** HU-12: consulta por identificador o seudonimo. */
    Optional<String> obtener(String id);

    /** HU-13: solo el autor edita su noticia. */
    boolean editar(String autorCuentaId, String id, String cuerpo);

    /** HU-14: requiere confirmacion. */
    boolean eliminar(String autorCuentaId, String id, boolean confirmado);

    /** HU-16. */
    List<String> categorias();
}
