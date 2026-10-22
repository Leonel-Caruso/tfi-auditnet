# Bloques 10 y 11 — Administrador de organización y planes

Decisiones: D-45 a D-49 en `docs/entrega-2/REGISTRO-DECISIONES.md`. Migración nueva: management **V5** (`planes`,
plan en `organizaciones_cliente`, `solicitudes_cambio_plan`). audit-core sigue en V7.

## Perfiles

| Nivel | Perfil | Alcance |
|---|---|---|
| 1 | Administrador del sistema | Toda la plataforma; asigna planes y resuelve solicitudes |
| 2 | Administrador de organización | Su organización: usuarios de operación, sedes, dispositivos, Mi plan y pedidos de cambio |
| 3 | Analista, auditor, responsable de gestión IT | Operación diaria de su organización (el responsable además consulta Mi plan) |

## Planes

| Plan | Dispositivos activos | Usuarios activos | Vigencia | Precio de referencia |
|---|---|---|---|---|
| Piloto | 10 | 5 | 60 a 90 días (90 por defecto) | Bonificado |
| Inicial | 50 | 15 | — | USD 150/mes |
| Profesional | 150 | 40 | — | USD 300/mes |
| Empresa | a medida | a medida | — | A cotizar |

Los administradores del sistema no cuentan como usuarios del plan. Precio y condiciones se editan en
*Planes y solicitudes* (quedan en la bitácora). Las organizaciones existentes quedaron en Inicial.

## API nueva o modificada

| Servicio | Método y ruta | Quién |
|---|---|---|
| management | `GET /api/plans` | Todos |
| management | `PUT /api/plans/{codigo}` (precio y condiciones) | Administrador del sistema |
| management | `GET /api/client-organizations/{id}/plan` (plan, uso, vencimiento, solicitud pendiente) | Todos, solo su organización |
| management | `PUT /api/client-organizations/{id}/plan` (asignación directa) | Administrador del sistema |
| management | `GET /api/client-organizations/{id}/plan-requests` | Administradores y responsable, su organización |
| management | `POST /api/client-organizations/{id}/plan-requests` y `…/{solicitud}/cancel` | Administrador de organización |
| management | `GET /api/admin/plan-requests?estado=PENDIENTE` y `POST …/{id}/approve` · `…/{id}/reject` | Administrador del sistema |
| management | `POST /api/client-organizations` ahora recibe `plan`, `diasPrueba`, `limiteDispositivos`, `limiteUsuarios` | Administrador del sistema |
| management | `/api/users`, `/api/roles` (consulta), `/api/sites`, `POST …/sites` | + administrador de organización, con alcance |
| audit-core | `POST /api/devices`, `PUT /api/devices/{id}`, `PATCH …/status` | + administrador de organización, solo su organización |
| audit-core | Consultas (tablero, dispositivos, baselines, hallazgos, alertas, reportes…) | + administrador de organización |

Errores del plan: **409** con un mensaje claro (límite alcanzado, piloto vencido, solicitud ya pendiente, bajar de plan
con uso mayor). Fuera de alcance: **403** (o 404 en dispositivos de otra organización, igual que en la consulta).

## Pantallas

- **Menú por perfil:** Organizaciones y Usuarios para los dos administradores; *Mi plan* para el administrador de
  organización y el responsable; *Planes y solicitudes*, Bitácoras y ADM solo para el administrador del sistema.
- **Usuarios:** el administrador de organización ve los de su organización, con la organización fija y solo los
  perfiles de operación; los administradores aparecen sin acciones.
- **Organizaciones y sedes:** bloque *Plan y uso* con barras; el administrador del sistema elige el plan al dar de
  alta y lo cambia con *Cambiar plan* (días de prueba o límites a medida según el plan). El administrador de
  organización gestiona sus sedes.
- **Dispositivos:** alta, edición y baja también para el administrador de organización (su organización se elige sola).
- **Mi plan:** plan actual, uso, aviso de vencimiento (15 días antes y al vencer), solicitud pendiente con
  *Cancelar*, catálogo e historial. *Solicitar cambio* con plan, modalidad mensual o anual y comentario.
- **Planes y solicitudes:** pendientes con *Aprobar* (pide límites si el plan es a medida) y *Rechazar* (con motivo),
  catálogo con *Editar precio* e historial. El panel ADM muestra cuántas hay pendientes.

## Pruebas automáticas

| Servicio / suite | Nuevas |
|---|---|
| management | `AdministradorOrganizacionTest` (alcance, perfiles, sedes, límite exacto, rehabilitar, piloto vencido), `PlanServiceTest` (estado, vencimiento, asignación, a medida, bajar de plan, circuito de solicitud, catálogo) |
| audit-core | `ControlPlanTest` (límite exacto, reactivar, sin límite, piloto vencido) |
| Pantallas | Cinco perfiles en todas las pantallas, menú por perfil y `test_perfiles.py` (usuarios, sedes y dispositivos del administrador de organización, Mi plan, pedir y cancelar, aprobar a medida, cambiar plan, alta con plan, aviso en ADM) |

## Casos de prueba manuales

Preparación (local o Azure): con el administrador del sistema, en Usuarios crear **jefa.acme** con perfil
*Administrador de organización* en una organización cliente (ACME) y **jefa.beta** en otra (BETA).

| CP | Pasos | Resultado esperado |
|---|---|---|
| CP-B10-01 | Ingresar como jefa.acme | Menú: Organizaciones, Usuarios, Mi plan; sin Bitácoras, ADM ni Planes |
| CP-B10-02 | Usuarios → + usuario | Organización fija en ACME; perfiles: analista, auditor y responsable |
| CP-B10-03 | Crear, editar, deshabilitar, restablecer contraseña de un analista de ACME | Todo funciona y queda en la bitácora de sistema |
| CP-B10-04 | Usuarios: fila de jefa.acme u otro administrador | Sin acciones: "lo gestiona el administrador del sistema" |
| CP-B10-05 | F12 como jefa.acme: `PATCH /api/users/<id de un usuario de BETA>/status` | 403 |
| CP-B10-06 | Organizaciones: + sede, editar y deshabilitar una sede de ACME | Funciona; no aparecen + organización ni Editar organización |
| CP-B10-07 | Dispositivos: registrar uno (ACME ya elegida), editar y dar de baja | Funciona; no aparece + tipo |
| CP-B11-01 | Administrador del sistema → Organizaciones → + organización con plan Piloto | Plan y uso: Piloto, vence en 90 días |
| CP-B11-02 | Con el Piloto (10 dispositivos), registrar hasta 10 y luego el 11.º | El 11.º: 409 "El plan Piloto permite hasta 10 dispositivos activos…" |
| CP-B11-03 | Con el Piloto (5 usuarios), crear el 6.º usuario; o reactivar uno estando en 5 | 409 con el límite |
| CP-B11-04 | Vencer el piloto (ver abajo) y probar importar una configuración, auditar y registrar un dispositivo | 409 "El período de prueba venció…"; Mi plan muestra el aviso rojo; consultar sigue funcionando |
| CP-B11-05 | Como jefa.acme → Mi plan → Solicitar cambio → Profesional, anual | Queda pendiente; el botón se deshabilita |
| CP-B11-06 | Administrador del sistema → ADM | "1 solicitud de cambio de plan por resolver" |
| CP-B11-07 | Planes y solicitudes → Aprobar | ACME pasa a Profesional; Mi plan muestra 150/40 y el historial |
| CP-B11-08 | Pedir otro cambio y rechazarlo con motivo; pedir y cancelar | Rechazada con el motivo visible para el cliente; cancelada |
| CP-B11-09 | Cambiar plan de una organización con 60 dispositivos activos a Inicial | 409: hay que deshabilitar dispositivos o elegir otro plan |
| CP-B11-10 | Planes y solicitudes → Editar precio de Inicial a 168 | Catálogo actualizado; bitácora de transacciones `CATALOGO_PLAN` |

Para vencer un piloto en local (PowerShell, con el compose levantado):

```powershell
"UPDATE organizaciones_cliente SET plan_vence = now() - interval '1 minute' WHERE identificador = 'ACME';" | docker exec -i tfi-postgres sh -c 'psql -U $POSTGRES_USER -d $POSTGRES_DB'
```

## Pendiente de actualizar en la carpeta de tesis (Bloque 16)

- Perfiles: agregar el administrador de organización (CU de gestión de usuarios de la organización).
- Tablas 20 y 32: columna de usuarios activos (5, 15, 40, a medida).
- CU nuevos: consultar plan, solicitar cambio de plan, resolver solicitud, asignar plan, editar precios.
- DER: `planes`, `solicitudes_cambio_plan` y columnas de plan en `organizaciones_cliente`.
