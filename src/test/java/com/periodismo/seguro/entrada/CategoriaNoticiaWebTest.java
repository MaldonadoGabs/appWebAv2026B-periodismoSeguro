package com.periodismo.seguro.entrada;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.periodismo.seguro.cuentas.CuentasApi;
import com.periodismo.seguro.cuentas.SolicitudRegistro;
import com.periodismo.seguro.noticias.NoticiasApi;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** HU-01: la categoria se elige en el formulario, se valida, se guarda y se muestra. */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "app.imagenes.directorio=target/imagenes-prueba-categorias",
    "spring.datasource.url=jdbc:h2:mem:categoriasHu01;DB_CLOSE_DELAY=-1"
})
class CategoriaNoticiaWebTest {
    @Autowired MockMvc mvc;
    @Autowired CuentasApi cuentas;
    @Autowired NoticiasApi noticias;

    private String correo;

    private MockHttpSession entrar() throws Exception {
        correo = "c" + UUID.randomUUID() + "@ejemplo.com";
        cuentas.registrar(new SolicitudRegistro(correo, "Ana Periodista", "Segura123"));
        return (MockHttpSession) mvc.perform(post("/login").with(csrf())
                .param("correo", correo).param("clave", "Segura123"))
            .andReturn().getRequest().getSession();
    }

    private long notasDeLaCuenta() {
        String cuentaId = cuentas.credencialesPorCorreo(correo).orElseThrow().cuentaId();
        return noticias.contarDeAutor(cuentaId);
    }

    @Test
    void elFormularioOfreceElCatalogoDeCategorias() throws Exception {
        mvc.perform(get("/noticias/nueva").session(entrar()))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("name=\"categoria\"")))
            .andExpect(content().string(containsString("value=\"derechos-humanos\"")))
            .andExpect(content().string(containsString("Derechos Humanos")));
    }

    @Test
    void sinCategoriaNoSeGuardaLaNoticia() throws Exception {
        MockHttpSession sesion = entrar();
        mvc.perform(multipart("/noticias/nueva").session(sesion).with(csrf())
                .param("titulo", "Titulo").param("subtitulo", "Subtitulo").param("cuerpo", "<p>Texto</p>"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Elige una categoría</div>")));
        assertEquals(0, notasDeLaCuenta());
    }

    @Test
    void unaCategoriaInventadaSeRechazaYNoSeGuarda() throws Exception {
        MockHttpSession sesion = entrar();
        mvc.perform(multipart("/noticias/nueva").session(sesion).with(csrf())
                .param("titulo", "Titulo").param("subtitulo", "Subtitulo").param("cuerpo", "<p>Texto</p>")
                .param("categoria", "inventada"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Elige una categoría de la lista")));
        assertEquals(0, notasDeLaCuenta());
    }

    @Test
    void unaCategoriaValidaSeGuardaYSeMuestraEnLaConfirmacionYEnElFeed() throws Exception {
        MockHttpSession sesion = entrar();
        MvcResult resultado = mvc.perform(multipart("/noticias/nueva").session(sesion).with(csrf())
                .param("titulo", "Nota con categoria HU01").param("subtitulo", "Subtitulo")
                .param("cuerpo", "<p>Texto</p>").param("categoria", "derechos-humanos"))
            .andExpect(status().is3xxRedirection())
            .andReturn();
        String destino = resultado.getResponse().getRedirectedUrl();

        mvc.perform(get(destino).session(sesion))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Categoría: Derechos Humanos")));

        mvc.perform(get("/").param("tamano", "50"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Nota con categoria HU01")))
            .andExpect(content().string(containsString("Derechos Humanos")));
    }
}
