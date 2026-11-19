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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/** El chat es exclusivo de lectores: la pagina nunca debe hablar de periodistas. */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:chatLectoresTexto;DB_CLOSE_DELAY=-1"
})
class ChatLectoresTextoTest {
    @Autowired MockMvc mvc;

    @Test
    @WithMockUser(username = "cuenta-1", roles = "LECTOR")
    void laPaginaDelChatSeLlamaChatDeLectoresYNoMencionaPeriodistas() throws Exception {
        mvc.perform(get("/chat"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("de lectores</h1>")))
            .andExpect(content().string(containsString("Conversación entre lectores.")))
            .andExpect(content().string(containsString("aria-label=\"Lectores conectados\"")))
            .andExpect(content().string(not(containsString("de periodistas"))))
            .andExpect(content().string(not(containsString("entre periodistas"))))
            .andExpect(content().string(not(containsString("Periodistas conectados"))));
    }

    @Test
    void elScriptDelChatUsaNombresDeLector() throws Exception {
        mvc.perform(get("/js/chat.js"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("|| 'Lector'")))
            .andExpect(content().string(containsString("|| 'Un lector'")))
            .andExpect(content().string(not(containsString("'Un periodista'"))));
    }
}
