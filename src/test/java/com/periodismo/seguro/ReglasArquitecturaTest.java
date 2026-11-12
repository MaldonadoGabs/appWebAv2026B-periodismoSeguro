package com.periodismo.seguro;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Reglas de dependencia del monolito modular (vault: Modulos del Monolito).
 * Para cambiar la arquitectura, edita SOLO la tabla PERMITIDAS: modulo -> modulos que puede usar.
 */
@AnalyzeClasses(packages = "com.periodismo.seguro", importOptions = ImportOption.DoNotIncludeTests.class)
public class ReglasArquitecturaTest {
    static final String P = "com.periodismo.seguro.";

    static final Map<String, List<String>> PERMITIDAS = Map.ofEntries(
        Map.entry("cuentas", List.of()),
        Map.entry("autenticacion", List.of("cuentas")),
        Map.entry("seguridad", List.of()),
        Map.entry("noticias", List.of()),
        Map.entry("chat", List.of("autenticacion")),
        Map.entry("notificaciones", List.of("noticias"))
    );

    @ArchTest
    public static void lasClasesInternasNoSeUsanDesdeFuera(JavaClasses c) {
        for (String m : PERMITIDAS.keySet()) {
            noClasses().that().resideOutsideOfPackage(P + m + "..")
                .should().dependOnClassesThat().resideInAPackage(P + m + ".internal..")
                .as("Nadie fuera de '" + m + "' usa " + m + ".internal")
                .check(c);
        }
    }

    @ArchTest
    public static void cadaModuloSoloUsaLoPermitido(JavaClasses c) {
        for (var e : PERMITIDAS.entrySet()) {
            List<String> ok = new ArrayList<>(List.of(P + e.getKey() + "..", "java..", "javax..", "jakarta..",
                "org.springframework..", "org.slf4j..", ""));
            e.getValue().forEach(d -> ok.add(P + d));
            classes().that().resideInAPackage(P + e.getKey() + "..")
                .should().onlyDependOnClassesThat().resideInAnyPackage(ok.toArray(String[]::new))
                .as("El modulo '" + e.getKey() + "' solo depende de " + e.getValue())
                .check(c);
        }
    }

    /** Contrato listo para microservicios: la API publica no expone entidades JPA ni repositorios. */
    @ArchTest
    public static void laApiPublicaNoExponePersistencia(JavaClasses c) {
        for (String m : PERMITIDAS.keySet()) {
            noClasses().that().resideInAPackage(P + m)
                .should().dependOnClassesThat().resideInAnyPackage("jakarta.persistence..", "org.springframework.data..")
                .as("La API publica de '" + m + "' no usa JPA ni Spring Data")
                .check(c);
        }
    }

    @ArchTest
    public static void sinCiclosEntreModulos(JavaClasses c) {
        slices().matching(P + "(*)..").should().beFreeOfCycles().check(c);
    }
}
