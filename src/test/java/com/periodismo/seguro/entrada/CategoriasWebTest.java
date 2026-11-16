package com.periodismo.seguro.entrada;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.periodismo.seguro.cuentas.CuentasApi;
import com.periodismo.seguro.cuentas.SolicitudRegistro;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/** HU-16: el periodista elige una categoria y esta se muestra en la confirmacion y en el feed. */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "app.imagenes.directorio=target/imagenes-prueba-catweb",
    "spring.datasource.url=jdbc:h2:mem:catwebhu16;DB_CLOSE_DELAY=-1"
})
class CategoriasWebTest {
    @Autowired MockMvc mvc;
    @Autowired CuentasApi cuentas;

    private MockHttpSession entrar() throws Exception {
        String correo = "c" + UUID.randomUUID() + "@ejemplo.com";
        cuentas.registrar(new SolicitudRegistro(correo, "Ana Periodista", "Segura123"));
        return (MockHttpSession) mvc.perform(post("/login").with(csrf())
            .param("correo", correo).param("clave", "Segura123"))
            .andReturn().getRequest().getSession();
    }

    @Test
    void elEditorOfreceLas9CategoriasDelCatalogo() throws Exception {
        String html = mvc.perform(get("/noticias/nueva").session(entrar()))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("id=\"categoriaSlug\"")))
            .andExpect(content().string(containsString("value=\"politica\"")))
            .andExpect(content().string(containsString("value=\"derechos-humanos\"")))
            .andExpect(content().string(containsString("value=\"otros\"")))
            .andExpect(content().string(containsString("Derechos Humanos")))
            .andExpect(content().string(containsString("Seguridad y Justicia")))
            .andReturn().getResponse().getContentAsString();

        // El select tiene una opcion vacia ("Elige una categoria") + 9 del catalogo = 10 <option>
        long opciones = countOccurrences(html, "<option ");
        org.junit.jupiter.api.Assertions.assertEquals(10, opciones,
            "debe haber 1 opcion vacia + 9 del catalogo");
    }

    private static long countOccurrences(String html, String sub) {
        long count = 0;
        int idx = 0;
        while ((idx = html.indexOf(sub, idx)) != -1) {
            count++;
            idx += sub.length();
        }
        return count;
    }

    @Test
    void sinCategoriaElFormularioVuelveConError() throws Exception {
        mvc.perform(multipart("/noticias/nueva").session(entrar()).with(csrf())
                .param("titulo", "Titulo").param("subtitulo", "Subtitulo").param("cuerpo", "<p>Texto</p>"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Selecciona una categoria de la lista")));
    }

    @Test
    void unaCategoriaQueNoExisteSeRechaza() throws Exception {
        mvc.perform(multipart("/noticias/nueva").session(entrar()).with(csrf())
                .param("titulo", "Titulo").param("subtitulo", "Subtitulo")
                .param("cuerpo", "<p>Texto</p>").param("categoriaSlug", "inventada"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("La categoria elegida no es valida")));
    }

    @Test
    void laCategoriaElegidaSeMuestraEnLaConfirmacionYEnElFeed() throws Exception {
        MockHttpSession sesion = entrar();
        MvcResult resultado = mvc.perform(multipart("/noticias/nueva").session(sesion).with(csrf())
                .param("titulo", "Nota de economia")
                .param("subtitulo", "Subtitulo")
                .param("cuerpo", "<p>Texto</p>")
                .param("categoriaSlug", "economia"))
            .andExpect(status().is3xxRedirection())
            .andReturn();

        mvc.perform(get(resultado.getResponse().getRedirectedUrl()).session(sesion))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("class=\"etiqueta\"")))
            .andExpect(content().string(containsString("Economía")));

        mvc.perform(get("/").param("tamano", "50"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Nota de economia")))
            .andExpect(content().string(containsString("Economía")))
            .andExpect(content().string(not(containsString("http://"))));
    }
}
