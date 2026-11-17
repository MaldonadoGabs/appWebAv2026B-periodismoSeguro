package com.periodismo.seguro.entrada;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class EstiloWebTest {
    @Autowired MockMvc mvc;

    @Test
    void lasPaginasCargaranElEstiloYLaCabeceraComun() throws Exception {
        for (String ruta : new String[] {"/", "/registro", "/login", "/recuperar"}) {
            mvc.perform(get(ruta))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/css/estilo.css")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("class=\"marca\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("class=\"pie\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"avisos\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/js/avisos.js")));
        }
    }

    @Test
    void laHojaDeEstilosYLasFuentesSeSirvenSinRecursosExternos() throws Exception {
        mvc.perform(get("/css/estilo.css"))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("--marcador")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString(".imagenes-noticia img")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("height: auto")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString(".aviso-noticia")));
        mvc.perform(get("/fonts/anton.woff2")).andExpect(status().isOk());
        mvc.perform(get("/js/tema.js")).andExpect(status().isOk());
        mvc.perform(get("/js/avisos.js")).andExpect(status().isOk());
    }

    @Test
    void elInicioIncluyeElFeedDeNoticias() throws Exception {
        mvc.perform(get("/"))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"feed\"")));
    }

    @Test
    void elScriptDeAvisosSeCargaConDeferParaEncontrarElContenedor() throws Exception {
        String html = mvc.perform(get("/"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        org.junit.jupiter.api.Assertions.assertTrue(
            html.contains("src=\"/js/avisos.js\" defer"), "el cliente de avisos debe cargarse con defer");
    }

    @Test
    void ningunaPaginaCargaRecursosDeOtrosDominios() throws Exception {
        for (String ruta : new String[] {"/", "/registro", "/login", "/recuperar"}) {
            String html = mvc.perform(get(ruta)).andReturn().getResponse().getContentAsString();
            org.junit.jupiter.api.Assertions.assertFalse(
                html.matches("(?s).*(src|href)=\"https?://.*"), "recurso externo en " + ruta);
        }
    }
}
