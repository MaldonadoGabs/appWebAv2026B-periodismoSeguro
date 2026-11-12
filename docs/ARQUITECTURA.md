# Arquitectura: monolito modular listo para microservicios

Un solo despliegue hoy. Cada modulo se puede extraer a un microservicio sin reescribirlo.

## Modulos y futuro servicio

| Modulo | Historias | Datos propios | Usa | Al extraerlo |
|---|---|---|---|---|
| `cuentas` | HU-01, HU-02 | cuentas, seudonimos | nadie | Servicio de cuentas |
| `autenticacion` | HU-03 a HU-06 | sesiones, tokens de recuperacion | cuentas | Servicio de auth (emite y valida sesiones) |
| `seguridad` | HU-08, HU-25, HU-26 | ninguno | nadie | Libreria compartida o gateway |
| `noticias` | HU-09 a HU-14, HU-16 | noticias, categorias | nadie | Servicio de noticias |
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

## Privacidad (HU-26)

Nunca registrar IP, identificadores del lector ni datos reales del periodista. La hora se redondea a 1 h.
