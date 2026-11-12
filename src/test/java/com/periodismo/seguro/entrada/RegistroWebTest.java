package com.periodismo.seguro.entrada;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
class RegistroWebTest {
    @Autowired MockMvc mvc;

    private static String correoNuevo() { return "u" + UUID.randomUUID() + "@ejemplo.com"; }

    private ResultActions enviar(String correo, String clave, String confirmacion) throws Exception {
        return mvc.perform(post("/registro")
            .param("correo", correo)
            .param("nombreReal", "Ana Pérez")
            .param("clave", clave)
            .param("confirmacion", confirmacion));
    }

    @Test
    void elFormularioSeMuestra() throws Exception {
        mvc.perform(get("/registro")).andExpect(status().isOk()).andExpect(view().name("registro"));
    }

    @Test
    void registroValidoRedirigeAlLoginConMensaje() throws Exception {
        enviar(correoNuevo(), "Segura123", "Segura123")
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login"))
            .andExpect(flash().attributeExists("mensaje"));
    }

    @Test
    void correoConFormatoInvalidoMarcaErrorEnElCampo() throws Exception {
        enviar("no-es-un-correo", "Segura123", "Segura123")
            .andExpect(status().isOk())
            .andExpect(model().attributeHasFieldErrors("form", "correo"));
    }

    @Test
    void claveDebilMarcaErrorEnElCampo() throws Exception {
        enviar(correoNuevo(), "abc", "abc")
            .andExpect(status().isOk())
            .andExpect(model().attributeHasFieldErrors("form", "clave"));
        enviar(correoNuevo(), "sinmayuscula1", "sinmayuscula1")
            .andExpect(model().attributeHasFieldErrors("form", "clave"));
        enviar(correoNuevo(), "SinNumeroAqui", "SinNumeroAqui")
            .andExpect(model().attributeHasFieldErrors("form", "clave"));
    }

    @Test
    void confirmacionDistintaMarcaErrorEnElCampo() throws Exception {
        enviar(correoNuevo(), "Segura123", "Otra12345")
            .andExpect(status().isOk())
            .andExpect(model().attributeHasFieldErrors("form", "confirmacion"));
    }

    @Test
    void correoRepetidoMarcaErrorEnElCampo() throws Exception {
        String correo = correoNuevo();
        enviar(correo, "Segura123", "Segura123").andExpect(status().is3xxRedirection());

        enviar(correo, "Segura123", "Segura123")
            .andExpect(status().isOk())
            .andExpect(model().attributeHasFieldErrors("form", "correo"));
    }
}
