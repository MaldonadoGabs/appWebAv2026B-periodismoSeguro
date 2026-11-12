package com.periodismo.seguro.entrada;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class InicioWebTest {
    @Autowired MockMvc mvc;

    @Test
    void elInicioMuestraMisionVisionYObjetivo() throws Exception {
        mvc.perform(get("/"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("id=\"mision\"")))
            .andExpect(content().string(containsString("id=\"vision\"")))
            .andExpect(content().string(containsString("id=\"objetivo\"")))
            .andExpect(content().string(containsString("Misión")))
            .andExpect(content().string(containsString("Visión")))
            .andExpect(content().string(containsString("Objetivo")));
    }

    @Test
    void elInicioInvitaARegistrarse() throws Exception {
        mvc.perform(get("/"))
            .andExpect(content().string(containsString("href=\"/registro\"")));
    }
}
