# Bitácoras de AuditNet

Requisito: Puntos Mandatorios de Carpeta 2026 (cátedra) y RF-014 / RNF-003 / RNF-012 de la tesis.

| Bitácora | Qué registra | Almacenamiento | Estado |
|---|---|---|---|
| Auditoría de sistema | Inicios de sesión (exitosos y rechazados), creación de usuarios, asignación de roles, creación de roles | PostgreSQL · `bitacora_sistema` | ✅ Bloque 2.3a |
| Transacciones | Altas, modificaciones y cambios de estado de entidades críticas (valor anterior y nuevo), ejecución de auditorías | PostgreSQL · `bitacora_transacciones` | ✅ Bloque 2.3b |
| Excepciones | Errores inesperados, con nivel de log configurable | MongoDB (Cosmos DB para MongoDB) | ✅ Bloque 2.3c |

## Bitácora de auditoría de sistema

### Eventos

| Evento | Cuándo | Resultado |
|---|---|---|
| `LOGIN_EXITOSO` | Inicio de sesión correcto | EXITO |
| `LOGIN_RECHAZADO` | Usuario inexistente, contraseña incorrecta, cuenta deshabilitada o sin roles (el motivo queda en `detalle`) | FALLO |
| `USUARIO_CREADO` | Alta de un usuario (también el administrador inicial del bootstrap) | EXITO |
| `ROLES_ASIGNADOS` | Asignación de roles a un usuario | EXITO |
| `ROL_CREADO` | Alta de un rol | EXITO |

### Campos

| Campo | Descripción |
|---|---|
| `fecha` | Momento del evento (UTC) |
| `evento`, `resultado` | Qué pasó y si se concretó |
| `actor` | Quién lo hizo. En un login, la identidad ingresada. `SISTEMA` para el bootstrap |
| `id_usuario_afectado`, `id_organizacion` | Sobre qué usuario y organización (sin clave foránea, a propósito) |
| `origen_ip` | Cadena `X-Forwarded-For` recibida (detrás del gateway) |
| `user_agent` | Navegador o cliente |
| `correlacion` | `X-Request-ID` generado por nginx; permite seguir la misma operación en todas las bitácoras |
| `detalle` | Información adicional. Nunca contiene contraseñas ni tokens |

### Garantías

- **Solo inserción:** triggers en PostgreSQL rechazan `UPDATE`, `DELETE` y `TRUNCATE`.
- **Rechazos siempre registrados:** se guardan en una transacción propia.
- **Éxitos consistentes:** se guardan en la transacción de la operación.

### Consulta

`GET /api/admin/bitacoras/sistema` (solo `ADMINISTRADOR_SISTEMA`). Filtros opcionales:
`evento`, `resultado`, `actor` (contiene), `desde`, `hasta` (ISO-8601, por ejemplo `2026-09-28T00:00:00Z`)
y `limite` (1 a 500, por defecto 100). Devuelve los más recientes primero.

## Bitácora de transacciones

Registra las operaciones de negocio sobre entidades críticas, con el **estado anterior y el nuevo en JSON**,
para poder reconstruir qué cambió, quién lo hizo y cuándo.

### Operaciones registradas

| Servicio | Entidad | Operación | Valor nuevo |
|---|---|---|---|
| management-service | `ORGANIZACION` | `ALTA` | Organización creada |
| management-service | `SEDE` | `ALTA` | Sede creada |
| audit-core-service | `DISPOSITIVO` | `ALTA` | Dispositivo creado |
| audit-core-service | `BASELINE` | `ALTA` | Baseline creada |
| audit-core-service | `REGLA` | `ALTA` | Regla creada |
| audit-core-service | `CONFIGURACION` | `IMPORTACION` | **Resumen**: id, dispositivo, versión, formato, tamaño y **hash SHA-256** del contenido original. La configuración completa no se copia (puede ser extensa y contener datos sensibles); el hash permite verificar que no fue alterada |
| audit-core-service | `AUDITORIA` | `EJECUCION` | Resultado: reglas evaluadas, cumplidas, hallazgos, severidad máxima, baseline utilizada |

Las operaciones `MODIFICACION`, `CAMBIO_ESTADO` y `BAJA_LOGICA` ya están previstas y se registrarán, con valor
anterior y nuevo, cuando se implementen la edición y las bajas lógicas (Bloque 3).

### Garantías

- **Consistencia:** el registro se guarda en la **misma transacción** que la operación. Si la operación no se
  confirma, no queda registro; si el registro no se puede guardar, la operación no se confirma.
- **Solo inserción:** los mismos triggers de la bitácora de sistema.
- **Tabla compartida:** la crea management-service (migración V3). audit-core-service no la mapea como entidad:
  inserta con SQL explícito, por lo que su arranque no depende de esa migración.

### Consulta

`GET /api/admin/bitacoras/transacciones` (solo `ADMINISTRADOR_SISTEMA`). Filtros opcionales: `entidad`,
`entidadId` (historial de un objeto puntual), `operacion`, `actor`, `organizacionId`, `desde`, `hasta` y `limite`.

## Bitácora de excepciones

Es la **entidad NoSQL** que exigen los Puntos Mandatorios 2026. Cada error se guarda como un documento en la
colección `bitacora_excepciones` de MongoDB (en Azure, Cosmos DB para MongoDB con el plan gratuito; en local,
el contenedor `tfi-mongo` del docker compose).

### Por qué NoSQL para esta bitácora

- Los errores son **documentos semiestructurados** (tipo, mensaje, traza de longitud variable, contexto HTTP) que no
  se relacionan con otras tablas: no necesitan claves foráneas ni transacciones con el negocio.
- Se escriben de forma **independiente** del modelo relacional: un problema en PostgreSQL puede registrarse igual.
- Separa el volumen técnico (trazas) de la base transaccional.

### Documento

| Campo | Descripción |
|---|---|
| `fecha` | Momento del error (índice descendente) |
| `servicio` | `management-service` o `audit-core-service` |
| `nivel` | `ERROR` (falla inesperada, 5xx) o `WARN` (error de uso rechazado, 4xx) |
| `tipo`, `mensaje`, `traza` | Clase de la excepción, mensaje y traza (hasta 4000 caracteres) |
| `metodoHttp`, `ruta`, `estadoHttp` | Solicitud que produjo el error |
| `usuario`, `correlacion` | Usuario autenticado y `X-Request-ID` (el mismo de las otras bitácoras) |

### Nivel configurable

| Variable | Valores | Efecto |
|---|---|---|
| `BITACORA_EXCEPCIONES_NIVEL` | `ERROR` (defecto) · `WARN` · `OFF` | Qué se guarda en MongoDB |
| `LOG_LEVEL` / `LOG_LEVEL_APP` | `DEBUG`, `INFO`, `WARN`, `ERROR`… | Nivel del log de consola de Quarkus |

### Garantías

- **No afecta al usuario:** la escritura es asíncrona y con timeouts de 3 segundos. Si MongoDB no responde, el error
  queda igual en el log del servicio.
- **Sin filtración de detalles:** ante un error inesperado el cliente recibe un 500 genérico con el código de
  correlación; la traza solo queda en la bitácora.

### Consulta y diagnóstico

- `GET /api/admin/bitacoras/excepciones` (solo `ADMINISTRADOR_SISTEMA`). Filtros: `servicio`, `nivel`, `desde`, `hasta`, `limite`.
- `POST /api/admin/diagnostico/excepcion` (solo `ADMINISTRADOR_SISTEMA`): genera un error controlado para verificar
  de punta a punta que la bitácora funciona.

## Pantalla de consulta (Bloque 2.3d)

Ruta **`/logs`** del frontend (ítem **BIT** del menú lateral, o `bitacoras` en la barra de comandos). Visible solo para
`ADMINISTRADOR_SISTEMA`: el ítem se oculta para otros perfiles y, si alguien entra por URL, la API responde 403 y la
pantalla lo informa. No modifica ningún registro.

| Pestaña | Endpoint | Filtros enviados a la API |
|---|---|---|
| SYS · Auditoría de sistema | `/api/admin/bitacoras/sistema` | evento, resultado, actor, desde, hasta, límite |
| TRX · Transacciones | `/api/admin/bitacoras/transacciones` | entidad, id de entidad, operación, actor, desde, hasta, límite |
| EXC · Excepciones | `/api/admin/bitacoras/excepciones` | servicio, nivel, desde, hasta, límite |

- Las fechas se ingresan en hora local y se envían en UTC (ISO-8601).
- El inspector muestra todos los campos del registro; en transacciones, el valor anterior y el nuevo con formato JSON;
  en excepciones, el mensaje y la traza.
- **Seguimiento de una solicitud:** el cuadro de búsqueda filtra los registros ya cargados por cualquier texto, incluido
  el código de seguimiento (correlación) que recibe el usuario en un error 500. El filtro se mantiene al cambiar de
  pestaña, así se ve lo que dejó la misma solicitud en las tres bitácoras. Alcance: busca dentro de los registros
  cargados (según el límite elegido), no en toda la base.
- La pestaña activa queda en la URL (`/logs#transacciones`), para abrirla directamente.
