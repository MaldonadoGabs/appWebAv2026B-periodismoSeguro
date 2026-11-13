package com.periodismo.seguro.entrada;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

/** HU-02: las vistas publicas nunca muestran correo ni nombre real. */
@SpringBootTest
@AutoConfigureMockMvc
class IdentidadWebTest {
    @Autowired MockMvc mvc;

    @Test
    void elMensajeTrasRegistrarMuestraElSeudonimoPeroNoLosDatosReales() throws Exception {
        String correo = "w" + UUID.randomUUID() + "@ejemplo.com";
        var res = mvc.perform(post("/registro")
                .param("correo", correo).param("nombreReal", "Xylophon Quimera")
                .param("clave", "Segura123").param("confirmacion", "Segura123"))
            .andExpect(status().is3xxRedirection())
            .andExpect(flash().attribute("mensaje", containsString("seudónimo")))
            .andReturn();

        String mensaje = (String) res.getFlashMap().get("mensaje");
        org.junit.jupiter.api.Assertions.assertFalse(mensaje.contains(correo));
        org.junit.jupiter.api.Assertions.assertFalse(mensaje.contains("Xylophon"));
    }

    @Test
    void lasPaginasPublicasNoMuestranDatosDeIdentidad() throws Exception {
        for (String ruta : new String[] {"/", "/login", "/registro"}) {
            mvc.perform(get(ruta))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("@ejemplo.com"))))
                .andExpect(content().string(not(containsString("Xylophon"))));
        }
    }
}
