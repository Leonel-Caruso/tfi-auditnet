# Bitácoras de AuditNet

Requisito: Puntos Mandatorios de Carpeta 2026 (cátedra) y RF-014 / RNF-003 / RNF-012 de la tesis.

| Bitácora | Qué registra | Almacenamiento | Estado |
|---|---|---|---|
| Auditoría de sistema | Inicios de sesión (exitosos y rechazados), creación de usuarios, asignación de roles, creación de roles | PostgreSQL · `bitacora_sistema` | ✅ Bloque 2.3a |
| Transacciones | Altas, modificaciones y cambios de estado de entidades críticas (valor anterior y nuevo), ejecución de auditorías | PostgreSQL · `bitacora_transacciones` | Bloque 2.3b |
| Excepciones | Errores inesperados, con nivel de log configurable | MongoDB (Cosmos DB para MongoDB) | Bloque 2.3c |

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
