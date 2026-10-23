# Bloques 12 y 13 — Correo, recuperación de contraseña y revisión periódica

Decisiones: D-50 a D-56 en `docs/entrega-2/REGISTRO-DECISIONES.md`. Migraciones nuevas: management **V6**
(`correos_salientes`, preferencias de aviso, `recuperaciones_contrasena`) y audit-core **V8** (`politicas_revision`,
marcas de aviso, origen de la auditoría, `resumenes_mensuales`).

## Avisos por correo

| Aviso | Lo genera | Destinatarios | Preferencia |
|---|---|---|---|
| Recuperación de contraseña | management | El usuario | Siempre |
| Alta de usuario (sin contraseña) | management | El usuario nuevo | Siempre |
| Solicitud de cambio de plan | management | Administradores del sistema | Siempre |
| Solicitud de plan aprobada o rechazada | management | Quien la pidió | Siempre |
| Alertas críticas nuevas (una por auditoría) | audit-core | Analista, responsable y administrador de la organización | Alertas |
| Vencimientos y configuraciones desactualizadas | audit-core (revisión) | Analista, responsable y administrador de la organización | Vencimientos |
| Resumen mensual | audit-core (revisión) | Responsable y administrador de la organización | Resumen |

Proveedor: Azure Communication Services en Azure; Mailpit en local (`http://localhost:8025`). Sin proveedor los avisos
quedan pendientes en la bandeja de salida.

## Configuración

| Variable | Servicio | Uso |
|---|---|---|
| `CORREO_CONEXION` (secreto) | management | Cadena de conexión de Communication Services |
| `CORREO_REMITENTE` | management | `DoNotReply@<dominio>.azurecomm.net` |
| `MAILPIT_URL` | management | Por defecto `http://localhost:8025` en el perfil dev |
| `APP_URL_PUBLICA` | management | Dirección del sitio, para los enlaces de los correos |
| `TAREA_SECRETO` (secreto) | ambos y el trabajo programado | Autoriza `/internal/...`; en el perfil dev, `solo-desarrollo-local` |
| `MANAGEMENT_URL` | audit-core | Dirección interna de management, para pedirle que envíe los avisos |

## API nueva o modificada

| Servicio | Método y ruta | Quién |
|---|---|---|
| management | `POST /api/auth/password-recovery` `{identidad}` → 202 con el mismo mensaje siempre | Público |
| management | `POST /api/auth/password-reset` `{token, password}` → 204 | Público (con el enlace) |
| management | `GET` / `PUT /api/auth/me/notifications` `{alertas, vencimientos, resumen}` | Usuario autenticado |
| management | `GET /api/admin/mail` (proveedor, totales de 24 h, últimos 20) y `POST /api/admin/mail/dispatch` | Administrador del sistema |
| management | `POST /internal/mail/dispatch` (encabezado `X-AuditNet-Tarea`) | audit-core (no pasa por nginx) |
| audit-core | `GET` / `PUT /api/client-organizations/{id}/review-policy` | Todos consultan su organización; modifican los dos administradores |
| audit-core | `GET /api/review/status[?organizationId=]` (vencidos y desactualizados) | Todos, con su alcance |
| audit-core | `POST /api/review/run` | Administrador del sistema |
| audit-core | `POST /internal/review/run` (encabezado `X-AuditNet-Tarea`) | Trabajo programado |
| audit-core | `GET /api/findings` informa `venceEn` y acepta `estado=VENCIDOS` | Igual que antes |
| audit-core | El historial de auditorías informa `origen` (MANUAL o AUTOMATICA) | Igual que antes |

nginx deriva `/api/client-organizations/{id}/review-policy` a audit-core (como `audit-status`). De `/internal` publica
solo `POST /internal/review/run`, para el trabajo programado (que corre en otro entorno, ver D-55); el resto responde
404.

## Pantallas

- **Login:** enlace *¿Olvidaste tu contraseña?* a `/recover`; `/reset-password` recibe el enlace del correo (el token
  se quita de la barra de direcciones apenas se abre).
- **Mi cuenta:** *Avisos por correo*, solo con los avisos que corresponden al perfil.
- **Organizaciones y sedes:** *Plazos de revisión* de la organización elegida (editable por los administradores).
- **Hallazgos:** etiqueta *vencido*, plazo en el detalle, filtro *vencidos* y cantidad de vencidos en la tarjeta de
  pendientes.
- **Tablero y estado del cliente:** tarjetas *hallazgos vencidos* y *configuraciones desactualizadas*.
- **Historial:** etiqueta *automática* en las auditorías de la revisión periódica.
- **ADM:** *Bandeja de salida* (proveedor, pendientes, enviados y fallidos de 24 h, últimos correos, *Enviar
  pendientes*) y *Revisión periódica* (*Ejecutar revisión ahora* con el resultado y lo que no se pudo hacer).

## Pruebas automáticas

| Servicio / suite | Nuevas |
|---|---|
| management | `CorreoServiceTest` (encolado, envío, token borrado, sin proveedor, reintentos, 429, rechazo, enlace vencido), `PlantillasCorreoTest`, `RecuperacionContrasenaServiceTest`, `FirmaAcsTest`; avisos en `UsuarioServiceTest` y `PlanServiceTest` |
| audit-core | `RevisionPeriodicaServiceTest` (re-auditoría, vencidos, desactualizados, resumen, idempotencia, política, omitidos); correo de alertas en `ReauditoriaTest` |
| Pantallas | `test_revision_correo.py`: recuperación, enlace vencido o incompleto, avisos por perfil, plazos, vencidos, tablero, historial y ADM |

## Casos de prueba manuales

Local: levantar el compose (incluye Mailpit) y abrir `http://localhost:8025` para ver los correos. Usar usuarios con
correos inventados: Mailpit los recibe todos.

| CP | Pasos | Resultado esperado |
|---|---|---|
| CP-B12-01 | Login → *¿Olvidaste tu contraseña?* → usuario de un analista | Mensaje genérico; en Mailpit, el correo con el botón naranja |
| CP-B12-02 | Abrir el enlace, poner una contraseña nueva e ingresar con ella | Funciona; la sesión que estaba abierta en otro navegador se cierra |
| CP-B12-03 | Volver a abrir el mismo enlace | "El enlace no es válido o ya venció" |
| CP-B12-04 | Pedir recuperación para un usuario que no existe y dos veces seguidas para uno real | Mismo mensaje; sale un solo correo; bitácora de sistema con el motivo |
| CP-B12-05 | Como administrador del sistema crear un usuario | Correo de alta sin contraseña |
| CP-B12-06 | Como analista: Mi cuenta → desmarcar *Alertas críticas* → ejecutar una auditoría con hallazgos críticos | Al analista no le llega; al responsable sí, un solo correo con todas las alertas |
| CP-B12-07 | Detener Mailpit (`docker stop tfi-mailpit`), generar un aviso y mirar ADM | El aviso queda pendiente con el error y su próximo intento |
| CP-B12-08 | Arrancar Mailpit y tocar *Enviar pendientes* | Se envía; pasa a enviado |
| CP-B12-09 | En Azure: *¿Olvidaste tu contraseña?* con un usuario con correo real | Llega desde `DoNotReply@…azurecomm.net` (revisar spam) |
| CP-B13-01 | Organizaciones → Plazos de revisión: poner crítica 20 y alta 10 | 400: los plazos van de menor a mayor |
| CP-B13-02 | Poner crítica 1 y guardar; ir a Hallazgos | Los críticos pendientes de más de 1 día aparecen *vencidos*; el tablero los cuenta |
| CP-B13-03 | ADM → *Ejecutar revisión ahora* | Resultado con vencidos y desactualizados; correo de *Revisión periódica* |
| CP-B13-04 | Volver a ejecutarla | Todo en 0: no repite avisos ni auditorías |
| CP-B13-05 | Llevar la última auditoría de un equipo a hace 40 días (ver abajo) y ejecutar la revisión | Una auditoría nueva con la etiqueta *automática* en el historial |
| CP-B13-06 | La primera ejecución del mes (o la primera de todas) | Correo *Resumen de <mes anterior>* al responsable y al administrador de organización |
| CP-B13-07 | En Azure: `az containerapp job start` (ver abajo) y mirar el historial de ejecuciones | Ejecución correcta; bitácora de transacciones `REVISION_PERIODICA` con actor `revision-periodica` |

Para simular una auditoría vieja en local (PowerShell, con el compose levantado; cambiar el id):

```powershell
"UPDATE auditorias_configuracion SET fecha_ejecucion = now() - interval '40 days' WHERE id_auditoria = 1;" | docker exec -i tfi-postgres sh -c 'psql -U $POSTGRES_USER -d $POSTGRES_DB'
```

## Despliegue en Azure

1. Publicar las imágenes y desplegar como siempre (management aplica V6 y audit-core V8 al arrancar).
2. `bash infra/azure/08-crear-email.sh`: Communication Services, dominio de Azure y variables de management.
3. `bash infra/azure/09-crear-revision.sh`: secreto de tareas en los dos servicios, `MANAGEMENT_URL` en audit-core, el
   entorno `cae-auditnet-tareas` (el de las apps está en modo express y no admite trabajos) y el trabajo diario.
4. Probar: CP-B12-09 y `az containerapp job start -n caj-auditnet-revision -g rg-auditnet`.

**Impacto financiero pendiente:** correo a USD 0,00025 por mensaje (centavos con el volumen de la demo) y el trabajo
diario dentro de la cuota gratuita de Container Apps. Confirmar en Cost Management al mes de uso.

## Pendiente de actualizar en la carpeta de tesis (Bloque 16)

- CU nuevos: recuperar contraseña, configurar avisos, configurar plazos de revisión, ejecutar revisión periódica,
  consultar bandeja de salida.
- CU de alertas: el aviso externo ahora existe (correo) y se informa si no está configurado.
- DER: `correos_salientes`, `recuperaciones_contrasena`, `politicas_revision`, `resumenes_mensuales` y columnas nuevas.
- Despliegue: Communication Services y el Container Apps Job; Mailpit en el entorno local.
