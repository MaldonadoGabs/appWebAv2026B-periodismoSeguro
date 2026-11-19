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

/** Animaciones en toda la aplicacion: entrada de pagina, formularios, errores, botones, menu y chat. */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:animacionesWeb;DB_CLOSE_DELAY=-1"
})
class AnimacionesWebTest {
    @Autowired MockMvc mvc;

    private String css() throws Exception {
        return mvc.perform(get("/css/estilo.css")).andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
    }

    @Test
    void existenLasAnimacionesDeEntradaFormulariosYAvisos() throws Exception {
        String css = css();
        for (String clave : new String[] {"@keyframes entrada-pagina", "@keyframes subir", "@keyframes aparecer-nota",
            "@keyframes sacudir", "@keyframes brillo-boton", "@keyframes latido", "@keyframes titilar",
            "form > *:nth-child(2)", ".nota.error", ".msg-error", ".boton.principal::after", ".participantes li"}) {
            org.junit.jupiter.api.Assertions.assertTrue(css.contains(clave), "falta " + clave);
        }
    }

    @Test
    void lasAnimacionesDeEntradaNoPisanLosEfectosAlPasarElPuntero() throws Exception {
        String css = css();
        org.junit.jupiter.api.Assertions.assertTrue(css.contains("ease-out backwards"),
            "las entradas usan fill-mode backwards para no fijar transform");
        org.junit.jupiter.api.Assertions.assertFalse(css.contains("ease-out both"),
            "fill-mode both fijaria el estado final y anularia el hover");
    }

    @Test
    void todasLasAnimacionesSeApagaConReducirMovimientoYElInterruptor() throws Exception {
        String css = css();
        int inicio = css.indexOf("Animaciones en toda la aplicacion");
        org.junit.jupiter.api.Assertions.assertTrue(inicio > 0);
        String bloque = css.substring(inicio);
        org.junit.jupiter.api.Assertions.assertTrue(bloque.contains("@media (prefers-reduced-motion: no-preference)"));
        // Cada regla con animation: del bloque debe ir dentro de la guarda y del selector que respeta el interruptor
        for (String linea : bloque.split("\n")) {
            if (linea.contains(" animation:") && !linea.trim().startsWith("*") && !linea.contains("@keyframes")) {
                org.junit.jupiter.api.Assertions.assertTrue(linea.contains(":root:not([data-efectos=\"off\"])")
                    || linea.trim().startsWith("background-repeat") || linea.trim().startsWith("content"),
                    "animacion sin guarda del interruptor: " + linea.trim());
            }
        }
    }

    @Test
    void elLoginYElRegistroTienenFormulariosYAvisosParaAnimar() throws Exception {
        mvc.perform(get("/login").param("error", ""))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("class=\"nota error\"")))
            .andExpect(content().string(containsString("class=\"campo\"")));
        mvc.perform(get("/registro"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("class=\"boton principal\"")));
    }

    @Test
    void alNavegarElContenidoSeDesvaneceYAlVolverSeRestaura() throws Exception {
        mvc.perform(get("/js/carga.js"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("classList.add(\"saliendo\")")))
            .andExpect(content().string(containsString("classList.remove(\"saliendo\")")));
        org.junit.jupiter.api.Assertions.assertTrue(css().contains(".saliendo main"));
    }

    @Test
    void elMenuSubrayaConUnMarcadorQueDeslizaYElSelectorDeCuentaSeLevanta() throws Exception {
        String css = css();
        org.junit.jupiter.api.Assertions.assertTrue(css.contains("transition: background-size .2s ease-out"));
        org.junit.jupiter.api.Assertions.assertTrue(css.contains(".opcion input:checked + .opcion-caja { translate: -2px -2px; }"));
    }
}
