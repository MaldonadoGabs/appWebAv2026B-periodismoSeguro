# Periodismo Seguro — notas del proyecto

- Monolito modular Spring Boot 3.3 / Java 21. Codigo, comentarios y documentacion en espanol. Una rama por HU, Conventional Commits, PR a `main` enlazando `AB#<id>` de Azure Boards.
- Build local: `java`/`mvn` NO estan en el PATH. JDK: `C:\Users\gabri\.jdks\openjdk-26.0.2.1`; Maven: `C:\Program Files\JetBrains\IntelliJ IDEA 2026.2.1\plugins\maven-plugin\lib\maven3\bin\mvn.cmd`. Con JDK 26 ArchUnit avisa `Unsupported class file major version 70` pero pasa igual.
- CUIDADO CON LAS PRUEBAS: `application.properties` fija `jdbc:h2:mem:periodismo`, asi que TODAS las clases `@SpringBootTest` comparten la misma base en memoria aunque tengan contextos distintos. Para pruebas que necesiten estado limpio, sobreescribir `spring.datasource.url` con un nombre unico y `DB_CLOSE_DELAY=-1` en `@TestPropertySource`.
- Orden de historias acordado: HU-09 crear noticias (implementada) -> categorias de noticias. Para categorias ya decidido: tabla propia, carga inicial con `data.sql` idempotente, 9 categorias (Politica, Economia, Derechos Humanos, Sociedad, Seguridad y Justicia, Medio Ambiente, Cultura, Internacional, Otros) e identificadores tipo slug.
- Rama de trabajo actual: `feature/categorias-noticias` (el usuario prefirio conservar el nombre aunque ahora contenga HU-09).
- Reglas de privacidad transversales: nunca correo, nombre real ni IP en logs ni en vistas publicas; seudonimo fijo; hora redondeada a 1 h; cero recursos de otros dominios (fuentes y JS autoalojados).
