# Periodismo Seguro

Plataforma de periodismo independiente que protege la identidad del periodista: publica con un **seudónimo** y su nombre real y su correo nunca son públicos.

Es un **monolito modular** hecho con Spring Boot, preparado para separarse en microservicios más adelante (ver [docs/ARQUITECTURA.md](docs/ARQUITECTURA.md)).

## Stack

| Capa | Tecnología |
|---|---|
| Aplicación | Java 21, Spring Boot 3.3 |
| Vistas | Thymeleaf (HTML en el servidor) |
| Datos | Spring Data JPA, H2 en local y PostgreSQL en Azure |
| Tiempo real | WebSocket nativo |
| Seguridad | BCrypt (spring-security-crypto), Bean Validation |
| Pruebas | JUnit 5, MockMvc y ArchUnit |
| Despliegue | Docker, Azure Container Apps y Azure DevOps Pipelines |

## Requisitos

- Java 21 (JDK)
- Maven 3.9 o superior
- Docker (solo si quieres correrlo en contenedor)

## Cómo ejecutar el proyecto

### 1. En tu computador (lo más simple)

```bash
git clone https://github.com/MaldonadoGabs/appWebAv2026B-periodismoSeguro.git
cd appWebAv2026B-periodismoSeguro
mvn spring-boot:run
```

Abre http://localhost:8080. Usa una base de datos H2 en memoria, así que no hay que instalar nada más. Los datos se borran al apagar la aplicación.

Páginas disponibles:

| Ruta | Qué es |
|---|---|
| `/` | Página de validación del stack (Thymeleaf, JPA y WebSocket) |
| `/registro` | Registro de periodistas |
| `/login` | Inicio de sesión |
| `/recuperar` | Solicitud de recuperación de contraseña (el enlace se imprime en la consola) |
| `/inicio` | Pantalla principal del periodista (requiere sesión) |
| `/actuator/health` | Estado de la aplicación (`{"status":"UP"}`) |

### 2. Con PostgreSQL

La aplicación lee estas variables de entorno. Si no existen, usa H2.

| Variable | Ejemplo |
|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/periodismo` |
| `DB_USER` | `periodismo` |
| `DB_PASSWORD` | `tu-clave` |
| `PORT` | `8080` |

```bash
DB_URL=jdbc:postgresql://localhost:5432/periodismo DB_USER=periodismo DB_PASSWORD=tu-clave mvn spring-boot:run
```

### 3. Con Docker

```bash
docker build -t periodismo-seguro .
docker run -p 8080:8080 periodismo-seguro
```

## Pruebas automatizadas

```bash
mvn verify
```

Compila y corre todas las pruebas. Tienen que pasar en verde para que el pipeline despliegue.

| Prueba | Qué comprueba |
|---|---|
| `CuentasServiceTest` | La contraseña se guarda con hash BCrypt, el seudónimo tiene formato y es único, el correo repetido se rechaza sin importar mayúsculas, los datos no se filtran en `toString` |
| `IdentidadSeparadaTest` | HU-02: el perfil público no tiene correo, nombre real ni hash; la identidad privada no tiene seudónimo; tablas distintas; los records de la API pública no exponen datos reales |
| `IdentidadWebTest` | HU-02: el mensaje de registro y las páginas públicas muestran el seudónimo, nunca correo ni nombre real |
| `AutenticacionServiceTest` | HU-03: credenciales con BCrypt, error igual para clave incorrecta y correo inexistente, bloqueo tras 5 fallos (también a correos inexistentes), reinicio con éxito, caducidad del bloqueo, correo no guardado en claro |
| `InicioSesionWebTest` | HU-03: formulario de login, redirección a `/inicio`, mensaje genérico, aviso de bloqueo, páginas protegidas redirigen al login, rutas públicas abiertas, CSRF |
| `RecuperacionClaveTest` | HU-05: token aleatorio de 256 bits guardado solo con hash, caduca en 30 minutos, una nueva solicitud anula la anterior, un correo inexistente no genera token, el enlace en consola no incluye correo, nombre real ni id de cuenta |
| `RecuperacionWebTest` | HU-05: formulario público, misma respuesta exista o no la cuenta, confirmación genérica, error junto al campo para correo inválido, CSRF exigido, enlace desde el login |
| `CierreSesionWebTest` | HU-04: botón de cerrar sesión visible solo con sesión, la sesión se invalida, la cookie JSESSIONID se elimina, las páginas protegidas redirigen al login, CSRF exigido |
| `RegistroWebTest` | Validaciones del formulario: correo inválido, contraseña débil, confirmación distinta, correo repetido y registro correcto |
| `InicioWebTest` | El inicio muestra la misión, la visión y el objetivo, e invita a registrarse |
| `EstiloWebTest` | Las páginas cargan la hoja de estilos y las fuentes propias, y no cargan recursos de otros dominios |
| `ReglasArquitecturaTest` | Los módulos solo usan la API pública de los demás, sin ciclos y sin exponer JPA |
| `AppTests` | La aplicación completa arranca |

Las pruebas de restablecer la clave con el token se agregan con la historia HU-06, y las de CRUD con las de noticias (HU-09 a HU-14).

## Estructura

```
src/main/java/com/periodismo/seguro/
  cuentas/          registro y seudónimos          (HU-01, HU-02)
  autenticacion/    sesión y recuperación de clave (HU-03 a HU-06)
  seguridad/        rutas, roles y cabeceras       (HU-08, HU-25, HU-26)
  noticias/         CRUD de noticias               (HU-09 a HU-14, HU-16)
  chat/             chat en tiempo real            (HU-17 a HU-20, HU-22)
  notificaciones/   avisos de noticias nuevas      (HU-21)
  entrada/          controladores web (gateway)
  validacion/       chat de prueba del stack
```

Cada módulo tiene su API pública en el paquete raíz y su lógica en `internal`. Las reglas se verifican en cada build con `ReglasArquitecturaTest`.

## Flujo de trabajo

1. Una branch por historia: `feature/hu-0N-nombre-corto`.
2. Commits con [Conventional Commits](https://www.conventionalcommits.org/es/): `tipo(módulo): descripción`, por ejemplo `feat(cuentas): registrar cuentas con hash BCrypt`.
3. Pull Request hacia `main`, enlazando la historia con `AB#<id>` de Azure Boards.
4. Al hacer merge a `main`, el pipeline compila, prueba y despliega solo.

## Despliegue (Azure)

- `azure-pipelines.yml`: compila y prueba con Maven, construye la imagen con Docker, la sube a Azure Container Registry y actualiza el Container App. Al final prueba `/actuator/health`.
- `DESPLEGAR.ps1`: crea la infraestructura de Azure y el pipeline (una sola vez, lo corre quien tiene la suscripción). Ver `scripts/`.

## Estilo visual

Interfaz de "Periodismo Seguro": base blanca con tinta casi negra, marcador amarillo, titulares en Anton, lectura en Source Serif 4 e interfaz en Public Sans (licencia OFL, autoalojadas en `static/fonts`). Tema claro y oscuro con botón. Nada se carga de otros dominios. Los estilos están en `src/main/resources/static/css/estilo.css`.

## Privacidad

No se guarda ni se registra en logs el correo, el nombre real ni la IP de nadie, y las vistas públicas solo muestran el seudónimo.
