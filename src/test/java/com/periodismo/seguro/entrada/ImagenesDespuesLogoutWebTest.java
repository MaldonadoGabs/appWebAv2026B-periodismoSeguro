package com.periodismo.seguro.entrada;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.periodismo.seguro.cuentas.CuentasApi;
import com.periodismo.seguro.cuentas.SolicitudRegistro;
import com.periodismo.seguro.noticias.ImagenAdjunta;
import com.periodismo.seguro.noticias.NoticiasApi;
import com.periodismo.seguro.noticias.SolicitudNoticia;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/** Las imagenes de una nota publica deben seguir disponibles tras cerrar la sesion del autor. */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "app.imagenes.directorio=target/imagenes-prueba-logout",
    "spring.datasource.url=jdbc:h2:mem:imagenesLogout;DB_CLOSE_DELAY=-1"
})
class ImagenesDespuesLogoutWebTest {
    @Autowired MockMvc mvc;
    @Autowired CuentasApi cuentas;
    @Autowired NoticiasApi noticias;

    @Test
    void laImagenSigueDisponibleSinSesionDespuesDelLogout() throws Exception {
        String correo = "i" + UUID.randomUUID() + "@ejemplo.com";
        cuentas.registrar(new SolicitudRegistro(correo, "Nombre Real", "Segura123"));
        String cuentaId = cuentas.credencialesPorCorreo(correo).orElseThrow().cuentaId();
        MockHttpSession sesion = (MockHttpSession) mvc.perform(post("/login").with(csrf())
                .param("correo", correo).param("clave", "Segura123"))
            .andReturn().getRequest().getSession();

        BufferedImage imagen = new BufferedImage(3, 2, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(imagen, "png", bytes);
        String notaId = noticias.crear(cuentaId, "Condor Silente 482",
            new SolicitudNoticia("Nota con foto", "Subtitulo", "<p>Texto</p>", "cultura",
                List.of(new ImagenAdjunta("image/png", bytes.toByteArray()))));
        String imagenId = noticias.obtener(notaId).orElseThrow().imagenesIds().get(0);

        mvc.perform(post("/logout").session(sesion).with(csrf()))
            .andExpect(status().is3xxRedirection());

        mvc.perform(get("/noticias/imagenes/" + imagenId))
            .andExpect(status().isOk())
            .andExpect(content().contentType("image/png"));
        String html = mvc.perform(get("/noticias/" + notaId))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("/noticias/imagenes/" + imagenId)))
            .andReturn().getResponse().getContentAsString();
        assertFalse(html.contains(cuentaId));
    }
}
