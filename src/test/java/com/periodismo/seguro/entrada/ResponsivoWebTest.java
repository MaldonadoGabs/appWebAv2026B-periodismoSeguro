package com.periodismo.seguro.entrada;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/** Diseno responsivo: viewport en cada pagina y reglas que evitan el desborde horizontal en el telefono. */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:responsivoWeb;DB_CLOSE_DELAY=-1"
})
class ResponsivoWebTest {
    @Autowired MockMvc mvc;

    @Test
    void todasLasPaginasDeclaranElViewportDelDispositivo() throws Exception {
        for (String ruta : new String[] {"/", "/registro", "/login", "/recuperar"}) {
            mvc.perform(get(ruta))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"viewport\" content=\"width=device-width, initial-scale=1\"")));
        }
    }

    @Test
    void laHojaDeEstilosEvitaElDesbordeHorizontalEnMovil() throws Exception {
        mvc.perform(get("/css/estilo.css"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("overflow-wrap: anywhere")))
            .andExpect(content().string(containsString("max-width: 100%")))
            .andExpect(content().string(containsString("min(19rem, 100%)")))
            .andExpect(content().string(containsString("@media (max-width: 40rem)")));
    }

    @Test
    void lasAreasTactilesTienenAlMenos44Pixeles() throws Exception {
        mvc.perform(get("/css/estilo.css"))
            .andExpect(content().string(containsString("@media (pointer: coarse)")))
            .andExpect(content().string(containsString("min-height: 2.75rem")));
    }
}
