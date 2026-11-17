package com.periodismo.seguro.entrada;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.periodismo.seguro.cuentas.CuentasApi;
import com.periodismo.seguro.cuentas.Rol;
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

/** Solo el lector reacciona y comenta; el detalle publico muestra el resultado sin exponer cuentas. */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "app.imagenes.directorio=target/imagenes-prueba-interacciones",
    "spring.datasource.url=jdbc:h2:mem:interaccionesWeb;DB_CLOSE_DELAY=-1"
})
class ReaccionesComentariosWebTest {
    @Autowired MockMvc mvc;
    @Autowired CuentasApi cuentas;
    @Autowired NoticiasApi noticias;

    private record Usuario(MockHttpSession sesion, String cuentaId) {}

    private Usuario entrar(Rol rol) throws Exception {
        String correo = "u" + UUID.randomUUID() + "@ejemplo.com";
        String cuentaId = cuentas.registrar(new SolicitudRegistro(correo, "Nombre Real", "Segura123", rol)).cuentaId();
        MockHttpSession sesion = (MockHttpSession) mvc.perform(post("/login").with(csrf())
                .param("correo", correo).param("clave", "Segura123"))
            .andReturn().getRequest().getSession();
        return new Usuario(sesion, cuentaId);
    }

    private String publicar(Usuario periodista, String titulo) {
        return noticias.crear(periodista.cuentaId(), "Condor Silente 482",
            new SolicitudNoticia(titulo, "Subtitulo", "<p>Texto</p>", "politica", List.of()));
    }

    @Test
    void elDetallePublicoMuestraReaccionesYComentariosPeroNoFormulariosSinSesion() throws Exception {
        String id = publicar(entrar(Rol.PERIODISTA), "Nota publica");

        mvc.perform(get("/noticias/" + id))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Reacciones")))
            .andExpect(content().string(containsString("Me gusta: 0")))
            .andExpect(content().string(containsString("Inicia sesión como lector")))
            .andExpect(content().string(not(containsString("name=\"tipo\""))));
    }

    @Test
    void elLectorVeLosFormulariosReaccionaYComenta() throws Exception {
        String id = publicar(entrar(Rol.PERIODISTA), "Nota con interacciones");
        Usuario lector = entrar(Rol.LECTOR);

        mvc.perform(get("/noticias/" + id).session(lector.sesion()))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("name=\"tipo\"")))
            .andExpect(content().string(containsString("name=\"texto\"")));

        mvc.perform(post("/noticias/" + id + "/reaccionar").session(lector.sesion()).with(csrf())
                .param("tipo", "ME_GUSTA"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/noticias/" + id));

        mvc.perform(post("/noticias/" + id + "/comentar").session(lector.sesion()).with(csrf())
                .param("texto", "Buen trabajo"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/noticias/" + id));

        mvc.perform(get("/noticias/" + id))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Me gusta: 1")))
            .andExpect(content().string(containsString("Buen trabajo")));
    }

    @Test
    void elPeriodistaNoPuedeReaccionarNiComentar() throws Exception {
        String id = publicar(entrar(Rol.PERIODISTA), "Nota sin interacciones del autor");
        Usuario periodista = entrar(Rol.PERIODISTA);

        mvc.perform(post("/noticias/" + id + "/reaccionar").session(periodista.sesion()).with(csrf())
                .param("tipo", "IMPACTA"))
            .andExpect(status().isForbidden());
        mvc.perform(post("/noticias/" + id + "/comentar").session(periodista.sesion()).with(csrf())
                .param("texto", "No deberia guardarse"))
            .andExpect(status().isForbidden());

        mvc.perform(get("/noticias/" + id))
            .andExpect(content().string(containsString("Impacta: 0")))
            .andExpect(content().string(not(containsString("No deberia guardarse"))));
    }
}
