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

/** El tema claro es siempre el predeterminado; el oscuro solo se enciende con el interruptor. */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:temaInterruptor;DB_CLOSE_DELAY=-1"
})
class TemaInterruptorWebTest {
    @Autowired MockMvc mvc;

    @Test
    void laCabeceraTieneUnInterruptorAccesibleApagadoPorDefecto() throws Exception {
        for (String ruta : new String[] {"/", "/registro", "/login"}) {
            mvc.perform(get(ruta))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("role=\"switch\"")))
                .andExpect(content().string(containsString("aria-checked=\"false\"")))
                .andExpect(content().string(containsString("id=\"tema\"")));
        }
    }

    @Test
    void laHojaDeEstilosNuncaActivaElOscuroPorLaPreferenciaDelSistema() throws Exception {
        mvc.perform(get("/css/estilo.css"))
            .andExpect(status().isOk())
            .andExpect(content().string(not(containsString("prefers-color-scheme"))))
            .andExpect(content().string(containsString(":root[data-tema=\"oscuro\"]")))
            .andExpect(content().string(containsString(".interruptor")));
    }

    @Test
    void elScriptForzaClaroSalvoQueLaPersonaEligaOscuro() throws Exception {
        mvc.perform(get("/js/tema.js"))
            .andExpect(status().isOk())
            .andExpect(content().string(not(containsString("matchMedia"))))
            .andExpect(content().string(containsString("\"data-tema\", oscuro ? \"oscuro\" : \"claro\"")))
            .andExpect(content().string(containsString("aria-checked")))
            // Una eleccion guardada de visitas anteriores no puede forzar el oscuro: se borra y no se lee
            .andExpect(content().string(not(containsString("localStorage.getItem"))))
            .andExpect(content().string(containsString("localStorage.removeItem(\"tema\")")))
            .andExpect(content().string(containsString("sessionStorage.getItem(\"tema\")")));
    }

    @Test
    void elTipoDeCuentaDelRegistroEsUnSelectorConOpcionesAccesibles() throws Exception {
        mvc.perform(get("/registro"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("<legend>Tipo de cuenta</legend>")))
            .andExpect(content().string(containsString("value=\"PERIODISTA\"")))
            .andExpect(content().string(containsString("value=\"LECTOR\"")))
            .andExpect(content().string(containsString("class=\"opcion-caja\"")));
    }
}
