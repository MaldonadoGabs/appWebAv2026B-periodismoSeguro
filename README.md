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
| `/` | Página principal: feed público de noticias + misión, visión y objetivo (`?pagina=1&tamano=10&q=&categoria=`) |
| `/registro` | Registro de periodistas |
| `/login` | Inicio de sesión |
| `/recuperar` | Solicitud de recuperación de contraseña (el enlace se imprime en la consola) |
| `/restablecer?token=...` | Definir una nueva contraseña con el enlace de recuperación (un solo uso, 30 minutos) |
| `/inicio` | Panel del periodista (rol PERIODISTA; un LECTOR o ADMINISTRADOR recibe 403) |
| `/mis-notas` | Lista paginada de las noticias del periodista autenticado (rol PERIODISTA) |
| `/chat` | Chat en tiempo real entre periodistas sobre `/ws`: conversación tipo WhatsApp con autor seudónimo, hora, avisos de ingreso y estado de escritura (rol PERIODISTA) |
| `/noticias/nueva` | Editor para redactar una noticia, con categoría obligatoria de la tabla del catálogo (rol PERIODISTA) |
| `/noticias/{id}` | Detalle público de una noticia: título, texto, categoría, seudónimo y fechas. 404 amigable si no existe |
| `/noticias/{id}/editar` | Editor de una noticia propia: carga los datos actuales y registra la fecha de edición (rol PERIODISTA, solo el autor) |
| `/noticias/{id}/confirmacion` | Confirmación tras guardar o editar; solo la ve su autor, para los demás 404 (rol PERIODISTA) |
| `/noticias/{id}/eliminar` | Diálogo de confirmación y borrado de una nota; solo el autor o un administrador |
| `/noticias/imagenes/{id}` | Imagen de una noticia (pública, para el feed) |
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

Compila y corre todas las pruebas. Tienen que pasar en verde para que el pipeline despliegue. El orden de clases es alfabetico y la codificacion UTF-8 esta fijada en el `pom.xml`, para que el resultado no dependa de si se ejecuta en Windows o en el agente Linux de Azure DevOps. El pipeline publica los informes JUnit en la pestana **Tests** aun cuando fallen.

| Prueba | Qué comprueba |
|---|---|
| `CuentasServiceTest` | La contraseña se guarda con hash BCrypt, el seudónimo tiene formato y es único, el correo repetido se rechaza sin importar mayúsculas, los datos no se filtran en `toString` |
| `IdentidadSeparadaTest` | HU-02: el perfil público no tiene correo, nombre real ni hash; la identidad privada no tiene seudónimo; tablas distintas; los records de la API pública no exponen datos reales |
| `IdentidadWebTest` | HU-02: el mensaje de registro y las páginas públicas muestran el seudónimo, nunca correo ni nombre real |
| `AutenticacionServiceTest` | HU-03: credenciales con BCrypt, error igual para clave incorrecta y correo inexistente, bloqueo tras 5 fallos (también a correos inexistentes), reinicio con éxito, caducidad del bloqueo, correo no guardado en claro |
| `InicioSesionWebTest` | HU-03: formulario de login, redirección a `/inicio`, mensaje genérico, aviso de bloqueo, páginas protegidas redirigen al login, rutas públicas abiertas, CSRF |
| `RecuperacionClaveTest` | HU-05: token aleatorio de 256 bits guardado solo con hash, caduca en 30 minutos, una nueva solicitud anula la anterior, un correo inexistente no genera token, el enlace en consola no incluye correo, nombre real ni id de cuenta |
| `RecuperacionWebTest` | HU-05: formulario público, misma respuesta exista o no la cuenta, confirmación genérica, error junto al campo para correo inválido, CSRF exigido, enlace desde el login |
| `RestablecerClaveTest` | HU-06: con un token vigente la clave cambia y se guarda con BCrypt, la clave vieja deja de servir, el token no se reutiliza, tokens caducados o inexistentes se rechazan, una clave débil no gasta el token, se anulan los demás enlaces y se cierran las sesiones abiertas, nada sensible en consola |
| `CambioClaveTest` | HU-06: `CuentasApi.cambiarClave` guarda BCrypt con sal nueva, rechaza claves que no cumplen las reglas del registro y cuentas inexistentes |
| `RestablecerWebTest` | HU-06: formulario público con token vigente y `Referrer-Policy: no-referrer`, error claro para token inválido o ausente, errores junto al campo (clave débil, confirmación distinta), redirección al login con aviso, la nueva clave sirve para entrar, el enlace no sirve dos veces, CSRF exigido |
| `CierreSesionWebTest` | HU-04: botón de cerrar sesión visible solo con sesión, la sesión se invalida, la cookie JSESSIONID se elimina, las páginas protegidas redirigen al login, CSRF exigido |
| `RegistroWebTest` | Validaciones del formulario: correo inválido, contraseña débil, confirmación distinta, correo repetido y registro correcto |
| `InicioWebTest` | El inicio muestra la misión, la visión y el objetivo, e invita a registrarse |
| `ListarNoticiasTest` | HU-10: orden por más reciente con desempate determinista por id, extracto en texto plano sin datos privados, fecha redondeada a 1 h, paginación con valores normalizados y tope de 50, miniatura omitida si el archivo desapareció |
| `FeedNoticiasWebTest` | HU-10: feed público sin sesión, estado vacío, encabezado/subtítulo/extracto/seudónimo, imagen pública sin sesión, paginación y parámetros inválidos, imagen perdida tolerada, cero recursos externos |
| `BuscarNoticiasTest` | HU-11: búsqueda por título y por palabra clave del cuerpo (ignorando mayúsculas), filtro por categoría, combinación de ambos, paginación con total independiente y resúmenes sin datos privados |
| `BuscarNoticiasWebTest` | HU-11: formulario de búsqueda y filtro en el feed, resultados por texto y categoría, mensaje si no hay coincidencias, parámetros preservados en la paginación y cero recursos externos |
| `DetalleNoticiaWebTest` | HU-12: detalle público sin sesión con título, texto, categoría, seudónimo y fechas, sin correo/nombre real/id de cuenta, 404 amigable y enlace desde cada tarjeta |
| `EditarNoticiaTest` | HU-13: solo el autor edita, se actualizan todos los campos, se registra la fecha de modificación redondeada a 1 h, se conserva la publicación original y se aplican validaciones |
| `EditarNoticiaWebTest` | HU-13: ruta protegida y CSRF, carga de datos actuales, otro periodista recibe 404, guardado con fecha, errores junto al campo y HTML peligroso eliminado |
| `EliminarNoticiaTest` | HU-14: sin confirmación no se borra, el autor o un administrador sí, otro periodista no, y se eliminan nota, referencias y archivos de imagen |
| `EliminarNoticiaWebTest` | HU-14: diálogo de confirmación, GET no borra, CSRF, 404 para otro periodista, administrador permitido y mensaje tras la redirección |
| `CabecerasSesionWebTest` | HU-25: CSP con `blob:` en `img-src`, `X-Frame-Options`, `X-Content-Type-Options`, `Referrer-Policy`, cookie HttpOnly/SameSite y recursos propios bajo la CSP |
| `PrivacidadLogsTest` | HU-26: el sanitizador enmascara correos e IPs, publicar una noticia no escribe correo, nombre real ni IP en los logs, y los DTO públicos no exponen datos privados |
| `EnvioRecuperacionResendTest` | El correo de recuperacion se envia por la API de Resend (simulada), sin registrar el correo ni el token en logs, y la consola es el proveedor por defecto |
| `EstiloWebTest` | Las páginas cargan la hoja de estilos, el feed y las fuentes propias, y no cargan recursos de otros dominios |
| `ReglasArquitecturaTest` | Los módulos solo usan la API pública de los demás, sin ciclos y sin exponer JPA |
| `CrearNoticiaTest` | HU-09: se guardan encabezado, subtítulo, texto, seudónimo, categoría y hora redondeada a 1 h; las imágenes se guardan en BD con copia local en disco, nombre UUID y sin metadatos; se rechazan archivos que no son imagen, formatos no permitidos y más de 5 imágenes; se publica `NoticiaCreada` |
| `CrearNoticiaWebTest` | HU-09: ruta protegida, CSRF, varias imágenes en un envío, HTML peligroso eliminado, errores junto al campo, recursos solo propios, imagen servida con `no-store` y 404 para una noticia inexistente |
| `RolesWebTest` | HU-08: el periodista abre panel, mis notas, escritura y chat; sin sesión redirige al login; LECTOR y ADMINISTRADOR reciben 403 en rutas de periodista; el registro no permite elegir rol; el lector ve el feed sin enlaces de periodista |
| `MisNotasWebTest` | HU-01/HU-08: el periodista ve sus notas, nunca las de otro; la nota ajena da 404; estado vacío y paginación normalizada |
| `CategoriaNoticiaWebTest` | HU-01: el formulario ofrece el catálogo de la tabla; sin categoría o con una inventada no se guarda; con una válida se muestra en la confirmación y en el feed |
| `NoticiasAutorTest` | HU-01/HU-08: catálogo de 9 categorías desde la tabla, categoría guardada y validada en el servicio, nota sin categoría rechazada, "mis notas" solo del autor, `esAutor` |
| `CategoriaTablaTest` | HU-01: tabla propia cargada por script sin duplicados, la API lee el catálogo de la tabla (no de una lista fija) y toda nota se guarda con una categoría válida |
| `AlmacenImagenesPersistenteTest` | La imagen sigue disponible desde la BD aunque desaparezca el archivo local, y se borra de ambos sitios al eliminarla |
| `ImagenesDespuesLogoutWebTest` | La imagen de una nota pública sigue sirviéndose sin sesión después del logout y el detalle no expone la cuenta del autor |
| `ServicioUsuariosRolTest` | HU-08: el usuario de Spring Security recibe el rol guardado en la cuenta (PERIODISTA, LECTOR, ADMINISTRADOR) |
| `ChatConectadosWebTest` | HU-20: la sala muestra el panel de conectados con su contador y la estructura de la conversación |
| `ChatSalaTest` / `ChatWebSocketHandlerTest` | Mensajes con seudónimo y hora al minuto, aviso de ingreso, escritura sin eco al emisor y serialización JSON de todos los eventos |
| `NotificacionesServiceTest` | HU-21: el evento de publicación se convierte en aviso con título y seudónimo para todas las conexiones abiertas |
| `AvisosWebSocketHandlerTest` | HU-21: el adaptador envía JSON con `tipo`, título, seudónimo y enlace `/noticias/{id}` |
| `AppTests` | La aplicación completa arranca |

La clasificación por categorías (HU-01), HU-09 (crear noticias), HU-10 (feed público), HU-11 (búsqueda y filtro), HU-12 (detalle público), HU-13 (editar noticia propia), HU-14 (eliminar nota propia), HU-18 y HU-20 (chat y conectados), HU-21 (avisos de noticias nuevas), HU-25 (cabeceras y sesiones seguras) y HU-26 (privacidad en logs) ya están implementadas.

## Editor de noticias (HU-09)

Desde `/noticias/nueva` el periodista redacta y modifica su nota antes de guardarla. La noticia tiene **encabezado, subtítulo, texto e imágenes**; el texto se escribe en un editor HTML propio (`contenteditable`) con barra de formato y vista previa en vivo, sin librerías ni CDN. Las imágenes seleccionadas se pueden quitar antes de guardar. Tras guardar se muestra `/noticias/{id}/confirmacion` con el seudónimo del autor.

| Variable | Para qué sirve |
|---|---|
| `APP_IMAGENES_DIR` | Directorio donde se guardan las imágenes. Por defecto, `<tmp>/periodismo-seguro/imagenes` |
| `PORT`, `DB_*` | Igual que en el resto de la aplicación |

- Formatos: PNG y JPEG, hasta 5 MB por archivo y 5 imágenes por noticia.
- El HTML del editor se reduce a una lista blanca (jsoup) antes de guardarse: sin `script`, sin atributos de eventos y sin imágenes incrustadas de otros dominios.
- Cada imagen se **re-codifica** para eliminar EXIF/GPS y se guarda con un nombre UUID: nunca se conserva el nombre original del archivo. La referencia queda en `imagenes_noticias`, sin claves foráneas.
- **Persistencia:** los bytes se guardan en la tabla `imagenes_almacenadas` y además se escriben en `APP_IMAGENES_DIR` como copia local. Si el disco efímero se pierde al reiniciar o desplegar, la imagen se sirve desde la BD y el detalle público sigue viéndola. El puerto `AlmacenImagenes` aísla el dominio del almacenamiento. La re-codificación no aplica la orientación EXIF de la foto original.
- Desde HU-13, una noticia ya guardada se edita en `/noticias/{id}/editar`: el editor carga los datos actuales y al guardar se registra la fecha de modificación (redondeada a 1 h, como la de creación).
- Desde HU-25, la CSP está activa con `img-src 'self' blob:`, para que el editor pueda mostrar las miniaturas locales de las imágenes seleccionadas antes de guardarlas.
- Desde HU-10, `/noticias/imagenes/{id}` es pública para que el feed pueda mostrar las fotos; crear y editar noticias sigue exigiendo sesión y rol PERIODISTA.

## Categorías de noticias (HU-01)

El catálogo vive en su **tabla propia** `categorias_noticias` (`id` slug, `nombre` y `orden`), dentro del módulo `noticias`. Se carga al arrancar con `src/main/resources/data.sql`: 9 categorías (Política, Economía, Derechos Humanos, Sociedad, Seguridad y Justicia, Medio Ambiente, Cultura, Internacional y Otros) insertadas de forma **idempotente**, tanto en H2 como en PostgreSQL. El mismo script clasifica como `otros` las noticias antiguas que no tenían categoría, para que ninguna quede sin clasificar.

Cada nota guarda el slug de su categoría en una columna obligatoria. El formulario exige una categoría de la tabla y el servicio la valida contra ella; no hay lista fija en el código. Para dar de alta o cambiar categorías se edita el script, no una constante Java. `spring.sql.init.mode=always` y `spring.jpa.defer-datasource-initialization=true` garantizan que el script se ejecute después de que Hibernate cree o actualice el esquema.

## Feed de noticias (HU-10)

La página principal `/` muestra **Últimas noticias**: un feed público, sin cuenta y sin rastreo, con 10 noticias por página (`?pagina=1&tamano=10`, máximo 50). Cada tarjeta muestra encabezado, subtítulo, extracto en texto plano, seudónimo del autor y fecha redondeada a 1 h; solo incluye la primera imagen si sigue disponible en disco o en la copia persistente.

Decisiones tomadas en esta historia:

- El feed convive en `/` con la misión, la visión, el objetivo y el bloque técnico «Estado del stack», que queda al final.
- Orden por `creadaEn` descendente con **desempate determinista por id**, porque la hora está truncada a 1 h (HU-26) y puede haber empates. Dentro de una misma hora el orden es arbitrario a propósito: guardar el instante exacto permitiría reconstruir cuándo publicó cada periodista. Por eso las pruebas no deben asumir que una noticia concreta cae en la página 1 cuando hay más noticias que el tamaño de página.
- `NoticiasApi.listar` devuelve `ResumenNoticia` (ids y texto), nunca entidades JPA ni el cuerpo completo; el extracto se calcula con JDK, sin meter jsoup dentro del módulo `noticias`.
- El extracto se pinta con `th:text`, así que el HTML del autor nunca se ejecuta en el feed.
- Los parámetros `pagina` y `tamano` inválidos se normalizan (nunca 400 ni 500) y una página fuera de rango se ajusta a la última.
- Sin IP, cookies de analítica ni identificadores del lector en logs; cero recursos de otros dominios.

Desde HU-11 el feed acepta `?q=texto&categoria=slug` para buscar por título/palabra clave y filtrar por categoría, preservando ambos parámetros en la paginación y mostrando un mensaje claro cuando no hay coincidencias. Desde HU-12 cada tarjeta enlaza con su detalle público en `/noticias/{id}`.

## Detalle de noticias (HU-12)

`/noticias/{id}` es público: muestra encabezado, subtítulo, texto, categoría, seudónimo del autor y las fechas de publicación y edición (en UTC, hora redondeada a 1 h). No muestra correo, nombre real, id de cuenta ni IP. Si la noticia no existe responde 404 con una página amigable, sin stack traces. La confirmación privada de HU-09 se trasladó a `/noticias/{id}/confirmacion` para no mezclarla con este detalle público. Las imágenes se muestran completas, a lo ancho de la columna de lectura y sin recortar; las miniaturas del feed y del editor son independientes.

## Búsqueda y edición (HU-11 y HU-13)

- La búsqueda es insensible a mayúsculas y mira el encabezado y el cuerpo; la categoría se filtra por el slug del catálogo. Una categoría inventada se ignora en lugar de dejar el feed vacío.
- `/noticias/{id}/editar` solo la abre el autor (el administrador no hereda ese permiso). Carga título, subtítulo, cuerpo y categoría actuales; aplica las mismas validaciones y sanitización HTML que la creación, y guarda la fecha de modificación. Un id ajeno o inexistente devuelve 404.

## Eliminar noticias (HU-14)

Desde «Mis notas» el autor abre `/noticias/{id}/eliminar`, una página de confirmación explícita: el `GET` solo muestra el aviso y el borrado exige un `POST` con CSRF. Solo el autor de la nota o un administrador puede hacerlo; otra cuenta recibe 404 para no revelar que la noticia existe. Al confirmar se eliminan la fila, sus referencias de imágenes, los archivos en disco y su copia en BD, y se redirige a `/mis-notas` (o al feed para el administrador) con un mensaje de confirmación.

## Avisos de noticias nuevas (HU-21)

Al publicar una nota, `NoticiaCreada` viaja con el título y el seudónimo del autor. El módulo `notificaciones` lo convierte, **después del commit**, en un aviso que se emite por el WebSocket público `/avisos-ws`. Ese endpoint es distinto del `/ws` del chat: no exige sesión, así que cualquier persona con una pantalla abierta del portal recibe el aviso sin recargar.

El cliente `static/js/avisos.js` se carga desde la plantilla base, por lo que funciona en **cualquier pantalla**, no solo en el chat. Muestra el título, el seudónimo y un enlace al detalle `/noticias/{id}`; el lector puede cerrarlo y desaparece solo. No se guarda correo, nombre real, IP ni identificador del lector.

## Conversación del chat (HU-17 a HU-20)

`/chat` muestra una conversación tipo WhatsApp: burbujas propias y ajenas, seudónimo del autor, hora local con precisión de minuto, mensajes de sistema cuando alguien se une y un indicador de «está escribiendo…». El cliente solo conoce su propio seudónimo (para alinear sus burbujas); nunca recibe correo, nombre real ni id de cuenta. El aviso de escritura viaja como JSON por `/ws` y no se devuelve a quien lo envía.

## Cabeceras, sesiones y privacidad de logs (HU-25 y HU-26)

Todas las respuestas llevan `Content-Security-Policy` (`default-src 'self'`, `img-src 'self' blob:` para el editor), `X-Frame-Options: DENY`, `X-Content-Type-Options: nosniff` y `Referrer-Policy: no-referrer`. La cookie de sesión es `HttpOnly` y `SameSite=Lax`; con `APP_PRODUCCION=true` se añade `Secure` y HSTS. La sesión caduca por inactividad en 30 minutos (`server.servlet.session.timeout`).

Los logs no incluyen correo, nombre real ni IP: el patrón de Logback no recoge el contexto HTTP y el convertidor `LogSanitizador` enmascara correos e IPv4 en cualquier mensaje antes de escribirlo. Las publicaciones y ediciones solo registran, como mucho, datos propios de la noticia, nunca la identidad del periodista.

## Estructura

```
src/main/java/com/periodismo/seguro/
  cuentas/          registro y seudónimos          (HU-01, HU-02)
  autenticacion/    sesión y recuperación de clave (HU-03 a HU-06)
  seguridad/        rutas, roles y cabeceras       (HU-08, HU-25, HU-26)
  noticias/         CRUD y categorías de noticias  (HU-01, HU-09 a HU-14, HU-16)
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

No se guarda ni se registra en logs el correo, el nombre real ni la IP de nadie, y las vistas públicas solo muestran el seudónimo. Las imágenes se guardan sin metadatos y con nombre aleatorio, y la hora de creación de una noticia se redondea a 1 h.

## Correo de recuperacion de clave (Resend)

Por defecto el enlace de recuperacion solo se imprime en consola (`APP_CORREO_PROVEEDOR=consola`), util para desarrollar. Para enviar correos reales se usa Resend.

| Variable | Para que sirve |
|---|---|
| `APP_CORREO_PROVEEDOR` | `consola` (por defecto) o `resend` |
| `RESEND_API_KEY` | API key de Resend. Es un secreto: nunca va en el repositorio |
| `APP_CORREO_REMITENTE` | Remitente, por ejemplo `Periodismo Seguro <noreply@tudominio.com>` |
| `APP_URL_PUBLICA` | URL publica de la aplicacion, que se usa en el enlace del correo |

Notas de Resend: sin un dominio verificado solo permite enviar al correo del dueño de la cuenta (remitente `onboarding@resend.dev`). Para escribir a cualquier periodista hay que verificar un dominio.

### Activarlo en Azure (una sola vez, lo hace quien tiene la suscripcion)

```powershell
az containerapp secret set -n ca-periodismo-psa26 -g rg-periodismo-psa26 --secrets resend-api-key=TU_API_KEY
az containerapp update -n ca-periodismo-psa26 -g rg-periodismo-psa26 --set-env-vars APP_CORREO_PROVEEDOR=resend RESEND_API_KEY=secretref:resend-api-key "APP_CORREO_REMITENTE=Periodismo Seguro <noreply@tudominio.com>" APP_URL_PUBLICA=https://TU-URL.azurecontainerapps.io
```

El pipeline solo cambia la imagen: estas variables y el secreto se conservan en cada despliegue. Si falta `RESEND_API_KEY` con el proveedor `resend`, la aplicacion no arranca (el despliegue falla y la version anterior sigue activa).
