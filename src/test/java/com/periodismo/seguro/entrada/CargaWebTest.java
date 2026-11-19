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

/** Estados de carga: barra, velo con mensaje, botones ocupados, esqueleto del chat y estado de la conexion. */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:cargaWeb;DB_CLOSE_DELAY=-1"
})
class CargaWebTest {
    @Autowired MockMvc mvc;

    @Test
    void todasLasPaginasCarganElScriptDeEstadosDeCarga() throws Exception {
        for (String ruta : new String[] {"/", "/registro", "/login", "/recuperar"}) {
            mvc.perform(get(ruta))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/js/carga.js")));
        }
    }

    @Test
    void elScriptDeCargaTieneLosMensajesDeCadaAccion() throws Exception {
        mvc.perform(get("/js/carga.js"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("velo-carga")))
            .andExpect(content().string(containsString("Abriendo el chat")))
            .andExpect(content().string(containsString("Verificando tus credenciales")))
            .andExpect(content().string(containsString("Creando tu cuenta")))
            // Los .js se sirven sin charset: solo se comprueban tramos sin acentos
            .andExpect(content().string(containsString("Cerrando sesi")))
            .andExpect(content().string(containsString("tardando m")))
            .andExpect(content().string(containsString("pageshow")));
    }

    @Test
    void losEstilosDeCargaRespetanReducirMovimiento() throws Exception {
        mvc.perform(get("/css/estilo.css"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString(".velo-carga")))
            .andExpect(content().string(containsString(".barra-carga")))
            .andExpect(content().string(containsString(".esqueleto")))
            .andExpect(content().string(containsString(".estado-conexion")))
            .andExpect(content().string(containsString("prefers-reduced-motion: reduce")));
    }

    @Test
    void elLoginMuestraSuMensajeDeCargaAlEntrar() throws Exception {
        mvc.perform(get("/login"))
            .andExpect(content().string(containsString("data-carga=\"Verificando tus credenciales…\"")));
    }

    @Test
    @WithMockUser(username = "cuenta-1", roles = "LECTOR")
    void elEnlaceDelChatDeLosLectoresAnunciaQueSeEstaAbriendo() throws Exception {
        mvc.perform(get("/chat"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("data-carga=\"Abriendo el chat de lectores…\"")));
    }

    @Test
    @WithMockUser(username = "cuenta-1", roles = "LECTOR")
    void elChatMuestraEsqueletoYEstadoConectandoHastaTenerConexion() throws Exception {
        mvc.perform(get("/chat"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("id=\"cargando-conversacion\"")))
            .andExpect(content().string(containsString("class=\"esqueleto")))
            .andExpect(content().string(containsString("Conectando…")))
            .andExpect(content().string(containsString("disabled")));
    }

    @Test
    void elChatMarcaConectadoYDesconectadoConTextoNoSoloColor() throws Exception {
        mvc.perform(get("/js/chat.js"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("marcar('Conectado', 'ok')")))
            .andExpect(content().string(containsString("marcar('Desconectado', 'error')")))
            .andExpect(content().string(containsString("quitarEsqueleto")));
    }
}
