package com.periodismo.seguro.entrada;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.periodismo.seguro.cuentas.CuentasApi;
import com.periodismo.seguro.cuentas.SolicitudRegistro;
import com.periodismo.seguro.noticias.NoticiasApi;
import com.periodismo.seguro.noticias.SolicitudNoticia;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/** HU-01 y HU-08: "Mis notas" solo muestra las notas del periodista autenticado. */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "app.imagenes.directorio=target/imagenes-prueba-mis-notas",
    "spring.datasource.url=jdbc:h2:mem:misNotasHu01;DB_CLOSE_DELAY=-1"
})
class MisNotasWebTest {
    @Autowired MockMvc mvc;
    @Autowired CuentasApi cuentas;
    @Autowired NoticiasApi noticias;

    private record Sesion(MockHttpSession sesion, String cuentaId, String correo) {}

    private Sesion periodista() throws Exception {
        String correo = "m" + UUID.randomUUID() + "@ejemplo.com";
        cuentas.registrar(new SolicitudRegistro(correo, "Nombre Real Secreto", "Segura123"));
        MockHttpSession sesion = (MockHttpSession) mvc.perform(post("/login").with(csrf())
                .param("correo", correo).param("clave", "Segura123"))
            .andReturn().getRequest().getSession();
        return new Sesion(sesion, cuentas.credencialesPorCorreo(correo).orElseThrow().cuentaId(), correo);
    }

    private String publicar(String cuentaId, String titulo) {
        return noticias.crear(cuentaId, "Seudonimo " + titulo.hashCode(),
            new SolicitudNoticia(titulo, "Subtitulo", "<p>Texto</p>", "otros", List.of()));
    }

    @Test
    void elPeriodistaVeSusPropiasNotas() throws Exception {
        Sesion ana = periodista();
        publicar(ana.cuentaId(), "Nota propia de Ana");

        mvc.perform(get("/mis-notas").session(ana.sesion()))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Nota propia de Ana")))
            .andExpect(content().string(not(containsString(ana.correo()))))
            .andExpect(content().string(not(containsString("Nombre Real Secreto"))));
    }

    @Test
    void unPeriodistaNoVeLasNotasDeOtroEnSuListado() throws Exception {
        Sesion ana = periodista();
        Sesion beto = periodista();
        String notaDeBeto = publicar(beto.cuentaId(), "Nota privada de Beto");
        publicar(ana.cuentaId(), "Nota de Ana");

        // "Mis notas" sigue siendo privado: Ana nunca ve la nota de Beto en su lista.
        mvc.perform(get("/mis-notas").session(ana.sesion()))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Nota de Ana")))
            .andExpect(content().string(not(containsString("Nota privada de Beto"))));

        // HU-12: el detalle es publico, asi que Ana puede leer la nota de Beto como cualquier lector.
        mvc.perform(get("/noticias/" + notaDeBeto).session(ana.sesion()))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Nota privada de Beto")));

        // Pero la confirmacion de la nota ajena si esta restringida al autor.
        mvc.perform(get("/noticias/" + notaDeBeto + "/confirmacion").session(ana.sesion()))
            .andExpect(status().isNotFound());
        // Beto si puede abrir la confirmacion de la suya.
        mvc.perform(get("/noticias/" + notaDeBeto + "/confirmacion").session(beto.sesion()))
            .andExpect(status().isOk());
    }

    @Test
    void sinNotasMuestraElEstadoVacio() throws Exception {
        mvc.perform(get("/mis-notas").session(periodista().sesion()))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Todavía no has publicado notas")));
    }

    @Test
    void losParametrosDePaginaInvalidosSeNormalizan() throws Exception {
        MockHttpSession sesion = periodista().sesion();
        for (String pagina : new String[] {"abc", "-2", "99999", ""}) {
            mvc.perform(get("/mis-notas").param("pagina", pagina).session(sesion))
                .andExpect(status().isOk());
        }
    }
}
