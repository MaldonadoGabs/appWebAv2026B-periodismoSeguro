package com.periodismo.seguro.entrada;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/** La guia pide validar en el cliente (requeridos, formato, longitud) ademas del servidor, que valida siempre. */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:validacionCliente;DB_CLOSE_DELAY=-1"
})
class ValidacionClienteWebTest {
    @Autowired MockMvc mvc;

    @Test
    void elRegistroValidaEnElNavegadorRequeridosFormatoYLongitud() throws Exception {
        mvc.perform(get("/registro"))
            .andExpect(status().isOk())
            .andExpect(content().string(not(containsString("novalidate"))))
            .andExpect(content().string(containsString("type=\"email\"")))
            .andExpect(content().string(containsString("required")))
            .andExpect(content().string(containsString("minlength=\"8\"")))
            .andExpect(content().string(containsString("maxlength=\"254\"")))
            .andExpect(content().string(containsString("pattern=\"(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9]).{8,}\"")));
    }

    @Test
    void laRecuperacionExigeUnCorreoValidoEnElNavegador() throws Exception {
        mvc.perform(get("/recuperar"))
            .andExpect(status().isOk())
            .andExpect(content().string(not(containsString("novalidate"))))
            .andExpect(content().string(containsString("type=\"email\"")))
            .andExpect(content().string(containsString("required")));
    }

    @Test
    void todasLasPaginasCarganElScriptDeValidacionConMensajesEnEspanol() throws Exception {
        mvc.perform(get("/registro"))
            .andExpect(content().string(containsString("/js/validacion.js")));
        mvc.perform(get("/js/validacion.js"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("setCustomValidity")))
            .andExpect(content().string(containsString("Completa este campo")))
            .andExpect(content().string(containsString("no coinciden")));
    }
}
