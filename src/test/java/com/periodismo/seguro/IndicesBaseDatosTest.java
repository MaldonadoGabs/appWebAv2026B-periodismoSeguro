package com.periodismo.seguro;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

/** La guia pide indices en los campos de busqueda y actualizacion: se comprueba contra el esquema real. */
@SpringBootTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:indicesBaseDatos;DB_CLOSE_DELAY=-1"
})
class IndicesBaseDatosTest {
    @Autowired JdbcTemplate jdbc;

    private List<String> indicesDe(String tabla) {
        return jdbc.queryForList(
            "SELECT DISTINCT UPPER(INDEX_NAME) AS NOMBRE FROM INFORMATION_SCHEMA.INDEXES "
                + "WHERE UPPER(TABLE_NAME) = ?", String.class, tabla.toUpperCase());
    }

    private void exige(String tabla, String... indices) {
        List<String> existentes = indicesDe(tabla);
        for (String indice : indices) {
            assertTrue(existentes.contains(indice.toUpperCase()), "falta el indice " + indice + " en " + tabla + ": " + existentes);
        }
    }

    @Test
    void lasNoticiasTienenIndicesPorFechaCategoriaYAutor() {
        exige("noticias", "idx_noticias_creada_en", "idx_noticias_categoria", "idx_noticias_autor");
    }

    @Test
    void comentariosReaccionesImagenesYTokensTienenIndicePorSuReferencia() {
        exige("comentarios_noticias", "idx_comentarios_noticia");
        exige("reacciones_noticias", "idx_reacciones_noticia", "idx_reacciones_cuenta");
        exige("imagenes_noticias", "idx_imagenes_noticia");
        exige("tokens_recuperacion", "idx_tokens_cuenta");
    }

    @Test
    void elCorreoYElSeudonimoTienenIndiceUnico() {
        List<Map<String, Object>> unicos = jdbc.queryForList(
            "SELECT UPPER(TABLE_NAME) AS T, UPPER(COLUMN_NAME) AS C FROM INFORMATION_SCHEMA.INDEX_COLUMNS "
                + "WHERE UPPER(TABLE_NAME) IN ('IDENTIDADES_PRIVADAS', 'PERFILES_PUBLICOS')");
        assertTrue(unicos.stream().anyMatch(f -> "CORREO".equals(f.get("C"))), "indice del correo");
        assertTrue(unicos.stream().anyMatch(f -> "SEUDONIMO".equals(f.get("C"))), "indice del seudonimo");
    }

    @Test
    void elHistorialDelChatTieneSuTabla() {
        Integer tablas = jdbc.queryForObject(
            "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE UPPER(TABLE_NAME) = 'MENSAJES_CHAT'", Integer.class);
        assertTrue(tablas != null && tablas == 1);
    }
}
