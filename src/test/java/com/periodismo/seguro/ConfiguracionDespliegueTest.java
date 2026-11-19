package com.periodismo.seguro;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/** Guarda los acuerdos de seguridad del despliegue y de las entregas: cookie Secure, .env.example y sin secretos. */
class ConfiguracionDespliegueTest {
    private static String leer(String archivo) throws IOException {
        return Files.readString(Path.of(archivo), StandardCharsets.UTF_8);
    }

    @Test
    void elPipelineActivaLaCookieSecureYLaUrlPublicaEnCadaDespliegue() throws IOException {
        String pipeline = leer("azure-pipelines.yml");
        assertTrue(pipeline.contains("APP_PRODUCCION=true"), "la cookie de sesion debe ser Secure en Azure");
        assertTrue(pipeline.contains("APP_URL_PUBLICA=https://"), "los enlaces de recuperacion deben usar la URL publica");
    }

    @Test
    void existeEnvExampleConLasVariablesYSinSecretosReales() throws IOException {
        String ejemplo = leer(".env.example");
        for (String variable : new String[] {"DB_URL", "DB_USER", "DB_PASSWORD", "APP_PRODUCCION", "APP_URL_PUBLICA",
            "APP_CORREO_PROVEEDOR", "RESEND_API_KEY"}) {
            assertTrue(ejemplo.contains(variable + "="), "falta " + variable);
        }
        Pattern secretoReal = Pattern.compile("(re_[A-Za-z0-9]{16,}|ghp_[A-Za-z0-9]{20,}|AKIA[0-9A-Z]{16})");
        assertFalse(secretoReal.matcher(ejemplo).find(), ".env.example no debe tener secretos reales");
    }

    @Test
    void elEnvRealNuncaSeSube() throws IOException {
        String ignorados = leer(".gitignore");
        assertTrue(ignorados.contains(".env"), ".env debe estar en .gitignore");
        assertTrue(ignorados.contains("!.env.example"), ".env.example si se sube");
    }

    @Test
    void ningunArchivoDeConfiguracionTraeContrasenasEscritasAMano() throws IOException {
        String propiedades = leer("src/main/resources/application.properties");
        assertFalse(Pattern.compile("(?m)^[^#\\n]*(password|api-key)\\s*=\\s*[^$\\s][^\\n]*$").matcher(propiedades).find(),
            "las claves se leen de variables de entorno");
    }
}
