package com.periodismo.seguro.entrada;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.periodismo.seguro.noticias.NoticiasApi;
import com.periodismo.seguro.noticias.SolicitudNoticia;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/** Efectos de movimiento: ultima hora con pausa, entrada en fila, interruptor de animaciones y respeto al sistema. */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "app.imagenes.directorio=target/imagenes-prueba-efectos",
    "spring.datasource.url=jdbc:h2:mem:efectosWeb;DB_CLOSE_DELAY=-1"
})
class EfectosWebTest {
    @Autowired MockMvc mvc;
    @Autowired NoticiasApi noticias;

    @BeforeEach
    void sembrar() {
        for (int i = 0; i < 3; i++) {
            String titulo = "Titular de efectos " + UUID.randomUUID();
            noticias.crear("autor-" + UUID.randomUUID(), "Condor Silente 482",
                new SolicitudNoticia(titulo, "Subtitulo " + i, "<p>Cuerpo " + i + "</p>", "politica", List.of()));
        }
    }

    @Test
    void elInicioMuestraLaBarraDeUltimaHoraConBotonDePausa() throws Exception {
        mvc.perform(get("/"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("class=\"ultima-hora\"")))
            .andExpect(content().string(containsString("Última hora")))
            .andExpect(content().string(containsString("class=\"uh-pausa\"")))
            .andExpect(content().string(containsString("aria-pressed=\"false\"")))
            .andExpect(content().string(containsString("Titular de efectos")));
    }

    @Test
    void laCopiaDelCarruselEstaOcultaParaLectoresDePantalla() throws Exception {
        mvc.perform(get("/"))
            .andExpect(content().string(containsString("class=\"uh-copia\" aria-hidden=\"true\"")));
    }

    @Test
    void siHayBusquedaNoSeMuestraLaBarraDeUltimaHora() throws Exception {
        mvc.perform(get("/").param("q", "palabra-que-no-existe"))
            .andExpect(status().isOk())
            .andExpect(content().string(not(containsString("class=\"ultima-hora\""))));
    }

    @Test
    void elInicioYaNoMuestraElBloqueDeEstadoDelStack() throws Exception {
        mvc.perform(get("/"))
            .andExpect(status().isOk())
            .andExpect(content().string(not(containsString("Estado del stack"))))
            .andExpect(content().string(not(containsString("Mensaje de prueba"))));
    }

    @Test
    void todasLasPaginasCarganElScriptDeEfectosYTienenElInterruptorDeAnimaciones() throws Exception {
        for (String ruta : new String[] {"/", "/registro", "/login"}) {
            mvc.perform(get(ruta))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/js/efectos.js")))
                .andExpect(content().string(containsString("id=\"efectos\"")))
                .andExpect(content().string(containsString("aria-label=\"Animaciones\"")));
        }
    }

    @Test
    void elScriptRespetaReducirMovimientoYElInterruptor() throws Exception {
        mvc.perform(get("/js/efectos.js"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("prefers-reduced-motion: reduce")))
            .andExpect(content().string(containsString("IntersectionObserver")))
            .andExpect(content().string(containsString("con-efectos")))
            .andExpect(content().string(containsString("localStorage.getItem(\"efectos\")")))
            .andExpect(content().string(containsString("aria-pressed")));
    }

    @Test
    void losEstilosSoloAnimanConTransformYOpacidadYSeApaganConReducirMovimiento() throws Exception {
        mvc.perform(get("/css/estilo.css"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("@keyframes uh-corre")))
            .andExpect(content().string(containsString("animation-play-state: paused")))
            .andExpect(content().string(containsString("@media (prefers-reduced-motion: no-preference)")))
            .andExpect(content().string(containsString(":root:not([data-efectos=\"off\"])")))
            .andExpect(content().string(containsString("translate: -3px -3px")))
            .andExpect(content().string(containsString(".con-efectos .tarjeta")));
    }

    @Test
    void elChatYLosAvisosAnimanSusElementosNuevos() throws Exception {
        mvc.perform(get("/css/estilo.css"))
            .andExpect(content().string(containsString("@keyframes burbuja")))
            .andExpect(content().string(containsString(".uh-etiqueta.pulso")));
        mvc.perform(get("/js/avisos.js"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("uh-etiqueta")));
    }
}
