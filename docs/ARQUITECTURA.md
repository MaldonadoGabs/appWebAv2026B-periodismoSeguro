# Arquitectura: monolito modular listo para microservicios

Un solo despliegue hoy. Cada modulo se puede extraer a un microservicio sin reescribirlo.

## Modulos y futuro servicio

| Modulo | Historias | Datos propios | Usa | Al extraerlo |
|---|---|---|---|---|
| `cuentas` | HU-01, HU-02 | identidades_privadas (correo, nombre real, hash), perfiles_publicos (seudonimo) | nadie | Servicio de cuentas |
| `autenticacion` | HU-03 a HU-06 | sesiones, tokens de recuperacion | cuentas | Servicio de auth (emite y valida sesiones) |
| `seguridad` | HU-08, HU-25, HU-26 | ninguno | nadie | Libreria compartida o gateway |
| `noticias` | HU-01, HU-09 a HU-14, HU-16 | noticias, imagenes_noticias (referencias), categorias_noticias | nadie | Servicio de noticias |
| `chat` | HU-17 a HU-20, HU-22 | mensajes, no leidos | autenticacion | Servicio de chat (WebSocket) |
| `notificaciones` | HU-21 | ninguno | evento `NoticiaCreada` | Servicio de notificaciones |

`entrada` (paginas, HU-07, y el chat de prueba) hace de gateway: valida la sesion, resuelve el seudonimo y llama a cada modulo.

## Reglas para que la migracion sea barata

1. **Solo la API publica.** Un modulo nunca usa clases de `internal` de otro (lo verifica ArchUnit en cada build).
2. **Contratos simples.** La API publica usa ids, texto y listas, nunca entidades JPA ni repositorios (lo verifica ArchUnit).
3. **Datos propios.** Cada modulo tiene sus tablas; sin claves foraneas ni joins entre modulos. Se refieren por id.
4. **Datos copiados, no consultados.** `noticias` guarda el seudonimo del autor junto a la noticia, en vez de pedirselo a `cuentas` en cada lectura.
5. **Eventos para avisar.** Lo asincrono se publica como evento (`NoticiaCreada`). Hoy viaja en memoria; despues sera un mensaje en una cola.
6. **Sin ciclos** entre modulos.

## Como cambiar las reglas

Edita la tabla `PERMITIDAS` en `src/test/java/com/periodismo/seguro/ReglasArquitecturaTest.java`. El pipeline falla si el codigo la incumple.

## Categorias (HU-01)

El catalogo esta en la tabla propia `categorias_noticias` (id slug, nombre y orden), dentro de `noticias`. `src/main/resources/data.sql` inserta las 9 categorias de forma idempotente (`INSERT ... SELECT ... WHERE NOT EXISTS`) y reclasifica como `otros` las noticias antiguas sin categoria. `spring.sql.init.mode=always` y `spring.jpa.defer-datasource-initialization=true` hacen que el script corra despues de que Hibernate cree o actualice el esquema, tambien en PostgreSQL.

`Noticia.categoria` guarda el slug y es obligatoria. `CatalogoCategorias` lee la tabla y `NoticiasApi.categorias()` devuelve los pares id/nombre; no queda ninguna lista fija en codigo. `NoticiasService` rechaza una nota sin categoria o con un id ausente en la tabla. Anadir o cambiar categorias es un cambio de datos del script, no de codigo.

## Borrado de noticias (HU-14)

`NoticiasApi.eliminar(cuentaId, id, confirmado, administrador)` exige confirmación explícita. El módulo `noticias` solo compara el id del autor con el de la cuenta y acepta el flag `administrador` que le pasa `entrada`, que es quien sabe el rol; así `noticias` sigue sin depender de `cuentas`. El borrado usa el puerto `AlmacenImagenes.eliminar(...)` para retirar los archivos y borra también las referencias de `imagenes_noticias`, sin claves foráneas.

## Conversación del chat (HU-17 a HU-20)

`ChatApi.enviar(cuentaId, texto)` y `ChatApi.avisarEscritura(cuentaId, activo)` resuelven el seudónimo desde `cuentas` dentro del módulo `chat`. `SalaChat` entrega `MensajeChat(autor, texto, Instant)` con la hora truncada al minuto, avisa del ingreso de cada conexión con `SuscripcionChat.ingresos` y difunde `EstadoEscritura` a todos menos a quien escribe. El adaptador de `entrada` serializa `mensaje`, `conectados`, `ingreso` y `escribiendo`; el cliente alinea las burbujas comparando con su propio seudónimo, inyectado como `data-yo` sin exponer la cuenta.

## Avisos de noticias nuevas (HU-21)

`NoticiaCreada` lleva id, título, seudónimo y categoría. `NotificacionesService` escucha el evento con `@TransactionalEventListener(AFTER_COMMIT)` y entrega un `AvisoNoticia` a `SalaAvisos`, que solo guarda callbacks por id de conexión. El puerto `AvisosWebSocketApi` lo implementa el adaptador de `entrada`, que serializa JSON en `/avisos-ws`. El endpoint es público y distinto de `/ws` para no heredar la autorización del chat; `avisos.js` se incluye en la cabecera común, así que está en todas las pantallas. Al migrar a microservicios, el evento pasa a una cola y la sala se sustituye por un broker, sin cambiar el contrato.

## Privacidad (HU-26)

Nunca registrar IP, identificadores del lector ni datos reales del periodista. La hora se redondea a 1 h.

En HU-09 las fotos se re-codifican antes de escribirse en disco (adios EXIF y GPS) y se guardan con nombre UUID, nunca con el nombre original. Los bytes salen del dominio por el puerto `AlmacenImagenes`: hoy es disco local, manana BD o blob, sin cambiar `NoticiasService`. Ojo: el filesystem de Container Apps es efimero; en produccion hace falta un volumen persistente o esa migracion.

El HTML del editor se sanea en `entrada` (lista blanca con jsoup), que es donde se reciben datos del navegador. El modulo `noticias` guarda el resultado ya limpio y lo muestra con `th:utext`.

## Feed (HU-10)

`NoticiasApi.listar(pagina, tamano)` devuelve `ResumenNoticia`: ids, texto y la fecha ya formateada, sin entidades JPA ni el cuerpo completo. El orden es `creadaEn DESC, id ASC`; el id es necesario porque la privacidad trunca la hora a 1 h y varias noticias pueden compartirla. `entrada` normaliza la paginacion y calcula el total con `contar()`.

El feed es publico, por lo que `/noticias/imagenes/**` se abrio en la cadena de filtros; la creacion de noticias sigue autenticada. El puerto `AlmacenImagenes` tambien responde `existe(...)` para que una imagen perdida en el disco efimero se omita en lugar de romper la tarjeta. El extracto se calcula dentro de `noticias` solo con JDK y se pinta con `th:text`: ningun HTML del autor llega al feed.
