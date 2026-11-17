package com.periodismo.seguro.entrada;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/** HU-14: dialogo de confirmacion, permisos, CSRF y mensaje tras la redireccion. */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "app.imagenes.directorio=target/imagenes-prueba-eliminar-web",
    "spring.datasource.url=jdbc:h2:mem:eliminarWebHu14;DB_CLOSE_DELAY=-1"
})
class EliminarNoticiaWebTest {
    @Autowired MockMvc mvc;
    @Autowired CuentasApi cuentas;
    @Autowired NoticiasApi noticias;

    private record Sesion(MockHttpSession http, String cuentaId) {}

    private Sesion entrar() throws Exception {
        String correo = "d" + UUID.randomUUID() + "@ejemplo.com";
        cuentas.registrar(new SolicitudRegistro(correo, "Nombre Real Secreto", "Segura123"));
        String cuentaId = cuentas.credencialesPorCorreo(correo).orElseThrow().cuentaId();
        MockHttpSession http = (MockHttpSession) mvc.perform(post("/login").with(csrf())
                .param("correo", correo).param("clave", "Segura123"))
            .andReturn().getRequest().getSession();
        return new Sesion(http, cuentaId);
    }

    private String publicar(String cuentaId, String titulo) {
        return noticias.crear(cuentaId, "Condor Silente 482",
            new SolicitudNoticia(titulo, "Subtitulo", "<p>Texto</p>", "otros", List.of()));
    }

    @Test
    void sinSesionNoSePuedeAbrirLaConfirmacion() throws Exception {
        String id = publicar("autor-" + UUID.randomUUID(), "Nota");

        mvc.perform(get("/noticias/" + id + "/eliminar"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("http://localhost/login"));
    }

    @Test
    void elAutorVeElDialogoDeConfirmacionAntesDeBorrar() throws Exception {
        Sesion autor = entrar();
        String id = publicar(autor.cuentaId(), "Titulo que se va a borrar");

        mvc.perform(get("/noticias/" + id + "/eliminar").session(autor.http()))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Confirmar")))
            .andExpect(content().string(containsString("Titulo que se va a borrar")))
            .andExpect(content().string(containsString("method=\"post\"")))
            .andExpect(content().string(containsString("Sí, eliminar la noticia")));
        assertTrue(noticias.obtener(id).isPresent(), "el GET no elimina la nota");
    }

    @Test
    void elAutorEliminaYVuelveAMisNotasConMensaje() throws Exception {
        Sesion autor = entrar();
        String id = publicar(autor.cuentaId(), "Nota borrada");

        mvc.perform(post("/noticias/" + id + "/eliminar").session(autor.http()).with(csrf()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/mis-notas"));
        assertTrue(noticias.obtener(id).isEmpty());

        mvc.perform(get("/mis-notas").session(autor.http()))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("La noticia se eliminó correctamente.")));
    }

    @Test
    void otroPeriodistaNoVeElDialogoNiPuedeEliminar() throws Exception {
        Sesion autor = entrar();
        Sesion otro = entrar();
        String id = publicar(autor.cuentaId(), "Nota ajena");

        mvc.perform(get("/noticias/" + id + "/eliminar").session(otro.http()))
            .andExpect(status().isNotFound());
        mvc.perform(post("/noticias/" + id + "/eliminar").session(otro.http()).with(csrf()))
            .andExpect(status().isNotFound());
        assertTrue(noticias.obtener(id).isPresent());
    }

    @Test
    @WithMockUser(username = "cuenta-admin", roles = "ADMINISTRADOR")
    void elAdministradorPuedeEliminarYVuelveAlFeedConMensaje() throws Exception {
        String id = publicar("autor-" + UUID.randomUUID(), "Nota retirada por admin");
        MockHttpSession sesion = new MockHttpSession();

        mvc.perform(get("/noticias/" + id + "/eliminar").session(sesion))
            .andExpect(status().isOk());
        mvc.perform(post("/noticias/" + id + "/eliminar").session(sesion).with(csrf()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/"));
        assertTrue(noticias.obtener(id).isEmpty());

        mvc.perform(get("/").session(sesion))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("La noticia se eliminó correctamente.")));
    }

    @Test
    void sinCsrfNoSeElimina() throws Exception {
        Sesion autor = entrar();
        String id = publicar(autor.cuentaId(), "Nota protegida");

        mvc.perform(post("/noticias/" + id + "/eliminar").session(autor.http()))
            .andExpect(status().isForbidden());
        assertTrue(noticias.obtener(id).isPresent());
    }
}
