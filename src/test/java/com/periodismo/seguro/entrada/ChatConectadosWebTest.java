package com.periodismo.seguro.entrada;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/** HU-20: la sala muestra el panel de conectados junto al chat. */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:chatConectadosHu20;DB_CLOSE_DELAY=-1"
})
class ChatConectadosWebTest {
    @Autowired MockMvc mvc;

    @Test
    @WithMockUser(username = "cuenta-1", roles = "PERIODISTA")
    void laPaginaDelChatMuestraLaListaDeConectados() throws Exception {
        mvc.perform(get("/chat"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Conectados")))
            .andExpect(content().string(containsString("id=\"conectados\"")))
            .andExpect(content().string(containsString("id=\"total-conectados\"")))
            .andExpect(content().string(containsString("id=\"log\"")))
            .andExpect(content().string(containsString("id=\"escribiendo\"")))
            .andExpect(content().string(containsString("data-yo=")))
            .andExpect(content().string(containsString("/js/chat.js")));
    }
}
