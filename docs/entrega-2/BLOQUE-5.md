# Bloque 5 — Reportes, alertas y tablero

Requisitos cubiertos: RF-011, RF-012, RF-013 y RF-017 (CU-006-001, CU-006-002, CU-006-003 y CU-007-003).
Decisiones: D-23 a D-27 en `REGISTRO-DECISIONES.md`. Migraciones audit-core V4 (alertas) y V5 (reportes).

## API nueva (audit-core-service)

| Método y ruta | Perfiles | Qué hace |
|---|---|---|
| `GET /api/alerts/critical` | Todos | Alertas priorizadas. Filtros: `estado` (VIGENTES por defecto, TODAS, ACTIVA, EN_ATENCION, CERRADA), `severidad`, `criticidad`, `dispositivoId`, `sedeId` |
| `GET /api/alerts/{id}` | Todos | Alerta con el hallazgo de origen (evidencia, impacto, recomendación) y la auditoría |
| `PATCH /api/alerts/{id}/status` | Administrador, responsable IT, analista | `{"estado":"EN_ATENCION","perfil":"ANALISTA_RED","comentario":"..."}` o `{"estado":"ACTIVA","comentario":"motivo"}`. Bitácora: ALERTA |
| `GET /api/dashboard/summary` | Todos | Indicadores, auditorías recientes, equipos con mayor riesgo y evolución de 8 semanas. El administrador puede pasar `organizacionId` |
| `GET /api/client-organizations/{id}/audit-status` | Todos (no administradores: solo la propia) | Resumen de la organización, riesgo por sede y por dispositivo, últimos reportes |
| `POST /api/reports/audits` | Administrador, analista, auditor | Genera y guarda el reporte (201). Sin auditorías para los criterios: 200 con mensaje. Bitácora: REPORTE |
| `GET /api/reports` | Todos | Reportes disponibles (sin contenido) |
| `GET /api/reports/{id}` | Todos | Reporte completo: criterios y contenido |
| `GET /api/reports/{id}/download` | Todos | CSV para Excel |

Cambio en una API existente: `GET /api/findings?estado=PENDIENTES` devuelve ABIERTO y EN_REVISION.

Errores: 400 (filtro o dato inválido), 403 (perfil u organización sin permiso), 404 (no existe), 409 (cambio no
permitido o hecho por otro usuario mientras tanto).

El gateway (nginx) deriva `/api/client-organizations/{id}/audit-status` a audit-core; el resto de
`/api/client-organizations` sigue yendo a management-service.

Ejemplo de cuerpo para generar un reporte:

```json
{
  "desde": "2026-09-01T03:00:00Z",
  "hasta": "2026-10-01T02:59:59Z",
  "dispositivoIds": [4],
  "severidades": ["CRITICA", "ALTA"],
  "estados": [],
  "auditoriaIds": []
}
```

## Alertas críticas

- Se generan solas al auditar, por cada hallazgo CRITICA o ALTA. Hay como máximo una alerta vigente por equipo y
  regla: si se vuelve a auditar sin corregir, el problema mantiene su alerta.
- Estados: **ACTIVA** (nadie la tomó) → **EN_ATENCION** (derivada a analista de redes o auditor técnico) → **CERRADA**.
- Desde Alertas se deriva o se devuelve (devolver exige motivo). Se cierran cuando el hallazgo pasa a RESUELTO o
  ACEPTADO y se reactivan si se reabre.
- No se borran nunca. Son internas: no se envían mails.

## Re-auditoría (D-26)

Al auditar un equipo, los hallazgos pendientes de auditorías anteriores cuya regla ahora se cumple pasan solos a
RESUELTO (usuario SISTEMA, con comentario en el seguimiento y en la bitácora) y su alerta se cierra. Solo se
resuelven los detectados en una versión de configuración igual o anterior a la auditada. Si la regla subió de
severidad, la alerta pasa al hallazgo más grave.

## Nivel general de riesgo

| Nivel | Cuándo |
|---|---|
| ALTO | Hallazgo CRITICA pendiente, o alerta vigente en un equipo de criticidad ALTA o CRITICA |
| MEDIO | Hallazgo ALTA pendiente |
| BAJO | Solo hallazgos MEDIA o BAJA pendientes, o ninguno |
| SIN DATOS | Nunca se auditó |

Se aplica igual a la organización, a cada sede y a cada dispositivo.

## Reportes (D-27)

- Criterios: rango de fechas o auditorías específicas (obligatorio uno de los dos), dispositivo, severidades y
  estados de los hallazgos. El administrador puede elegir la organización o todas.
- Contenido: resumen (resultado general, auditorías, dispositivos, cumplimiento, hallazgos por severidad y estado,
  severidad máxima), auditorías incluidas (dispositivo, baseline y configuración usadas) y hallazgos con severidad,
  impacto, evidencia y recomendación. Si falta algún dato de origen, el reporte lo informa.
- Se guarda como foto fija (no cambia aunque cambien los hallazgos) y no se puede modificar ni borrar.
- **PDF**: botón "Descargar PDF" → el navegador abre la impresión con solo el reporte → "Guardar como PDF".
- **CSV**: una fila por hallazgo (y una por auditoría sin hallazgos), separador ";", se abre directo en Excel.

## Pantallas

- **Tablero (`/dashboard`, ítem `~/`)**: nivel de riesgo, 6 indicadores con enlace al detalle, hallazgos pendientes
  por severidad, desvíos por semana (con tabla), alertas vigentes, auditorías recientes y equipos con mayor riesgo.
  El administrador elige la organización o ve todas, y además ve organizaciones, sedes y usuarios.
- **Estado del cliente (`/client-status`, ítem CLI)**: lo mismo para una organización, con el riesgo por sede, la
  lista completa de dispositivos ordenada por riesgo y los últimos reportes.
- **Alertas (`/alerts`, ítem ALR)**: filtros, métricas, lista priorizada y detalle con derivación.
- **Reportes (`/reports`, ítem REP)**: criterios, reportes generados y vista del reporte con descarga en PDF o CSV.
  Desde el historial, "Generar reporte de esta auditoría" abre la pantalla con la auditoría cargada.
- Hallazgos acepta `?hallazgo=ID`, `?estado=`, `?severidad=` y `?dispositivo=`, y el filtro nuevo "pendientes".

## Casos de prueba manuales (para la matriz de CP)

| CP | Pasos | Resultado esperado |
|---|---|---|
| CP-B5-01 | Auditar una configuración que incumple una regla CRITICA | Aparece una alerta ACTIVA en Alertas y en el tablero |
| CP-B5-02 | Alertas → seleccionar → derivar a ANALISTA_RED | Pasa a EN_ATENCION con el perfil; Bitácoras → TRX → ALERTA muestra el cambio |
| CP-B5-03 | Con una alerta EN_ATENCION, "devolver a ACTIVA" sin motivo | No lo permite; con motivo sí |
| CP-B5-04 | Hallazgos → pasar a RESUELTO el hallazgo de esa alerta | La alerta queda CERRADA (filtro "todas") y deja de contar como vigente |
| CP-B5-05 | Reabrir ese hallazgo | La alerta vuelve a ACTIVA |
| CP-B5-06 | Ingresar como auditor técnico y abrir una alerta | Ve el detalle, pero no puede derivarla |
| CP-B5-07 | Importar otra vez la misma configuración (sin corregir) y auditar | Hay un hallazgo nuevo, pero no una alerta nueva |
| CP-B5-08 | Corregir la configuración, importarla y auditar | Los hallazgos anteriores de esas reglas quedan RESUELTO por SISTEMA y sus alertas cerradas |
| CP-B5-09 | Tablero → tocar "alertas vigentes" o una barra de severidad | Navega a Alertas o Hallazgos con el filtro aplicado |
| CP-B5-10 | Tablero → pasar el mouse por una semana del gráfico; abrir "ver tabla" | Muestra detectados, cerrados y auditorías de esa semana |
| CP-B5-11 | Estado del cliente (como responsable IT) | Ve su organización, el riesgo por sede, los equipos ordenados por riesgo y los reportes |
| CP-B5-12 | Como administrador, elegir otra organización en el tablero y en Estado del cliente | Los indicadores cambian a esa organización |
| CP-B5-13 | Reportes → últimos 30 días → Generar | Se muestra el reporte con resumen, auditorías y hallazgos; aparece en la lista |
| CP-B5-14 | Generar con un rango sin auditorías | Informa que no hay resultados y no guarda nada |
| CP-B5-15 | Abrir un reporte → "Descargar PDF" y "Descargar CSV" | El PDF muestra solo el reporte; el CSV se abre en Excel en columnas |
| CP-B5-16 | Cambiar el estado de un hallazgo incluido y volver a abrir el reporte | El reporte no cambia (foto fija) |
| CP-B5-17 | Ingresar como responsable IT → Reportes | Puede abrir y descargar, pero no generar |
| CP-B5-18 | Con la consola del navegador (F12), como no administrador, pedir el estado de otra organización | 403 |

Para CP-B5-18, en la consola del navegador (F12, con la sesión iniciada):
`fetch('http://localhost:8082/api/client-organizations/99/audit-status', {headers: {Authorization: 'Bearer ' + sessionStorage.getItem('tfi.auth.token')}}).then(r => r.status)`

## Pendiente de actualizar en la carpeta de tesis

- CU-005-001: resolución automática al re-auditar (D-26).
- CU-006-001: criterios, contenido, foto fija, PDF por impresión y CSV; perfiles que generan (D-27); `GET /api/reports`.
- CU-006-002: estados ACTIVA, EN_ATENCION y CERRADA; el cierre sigue al hallazgo; una alerta por equipo y regla.
- CU-006-003 y CU-007-003: endpoints consolidados (D-24) y reglas del nivel de riesgo (D-25).
- Tabla 46 (diccionario): `alertas_criticas` (en la tesis figura como `alertas`), `reportes_auditoria` (sin
  `id_auditoria`: un reporte puede abarcar varias) y `reportes_auditoria_auditorias`.
- Diagrama de despliegue: los reportes se guardan en PostgreSQL, no en un repositorio de archivos (se revisa en el
  Bloque 7, almacenamiento).
