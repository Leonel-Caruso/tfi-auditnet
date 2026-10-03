# Bloque 4 — Seguimiento de hallazgos e historial de auditorías

Requisitos cubiertos: RF-009 y RF-010 (CU-005-001 y CU-005-002). Decisiones: D-20 a D-22 en `REGISTRO-DECISIONES.md`.

## API nueva o modificada (audit-core-service)

| Método y ruta | Perfiles | Qué hace |
|---|---|---|
| `GET /api/findings` | Todos | Lista priorizada. Filtros: `estado`, `severidad`, `dispositivoId`, `baselineId`, `auditoriaId`, `desde`, `hasta` |
| `GET /api/findings/{id}` | Todos | Detalle con estado, último cambio y criticidad del dispositivo |
| `GET /api/findings/{id}/history` | Todos | Historial de cambios de estado (quién, cuándo, comentario) |
| `PATCH /api/findings/{id}/status` | Administrador, analista, auditor | `{"estado":"RESUELTO","comentario":"..."}`. Bitácora: CAMBIO_ESTADO |
| `GET /api/audits/history` | Todos | Filtros: `dispositivoId`, `baselineId`, `severidad`, `resultado`, `ejecutadoPor`, `desde`, `hasta` |
| `GET /api/audits/{id}` | Todos | Ahora incluye la configuración evaluada y la baseline usada |
| `GET /api/audits/{id}/comparison` | Todos | Comparación con la auditoría anterior del mismo dispositivo |

Fechas en ISO-8601 (`2026-10-01T00:00:00Z`). En la pantalla, el filtro "hasta" incluye todo el minuto elegido.
Errores: 400 (filtro o dato inválido, por ejemplo `dispositivoId=abc`), 403 (sin permiso), 404 (no existe o es de otra
organización), 409 (transición no permitida, o el hallazgo fue cambiado por otro usuario mientras tanto).

Limitación conocida: las listas no están paginadas (ver pendientes en `REGISTRO-DECISIONES.md`).

## Estados de un hallazgo

Transiciones permitidas:

- ABIERTO → EN_REVISION, RESUELTO o ACEPTADO
- EN_REVISION → ABIERTO, RESUELTO o ACEPTADO
- RESUELTO o ACEPTADO → ABIERTO (reabrir)

| Estado | Significado | Comentario |
|---|---|---|
| ABIERTO | Detectado o reabierto, pendiente de análisis | Obligatorio al reabrir |
| EN_REVISION | Alguien lo está analizando o corrigiendo | Opcional |
| RESUELTO | Se aplicó la corrección (se confirma con una nueva auditoría) | Obligatorio |
| ACEPTADO | Riesgo aceptado de forma justificada (excepción o falso positivo) | Obligatorio |

## Priorización

1. Pendientes (ABIERTO, EN_REVISION) antes que cerrados.
2. Severidad, de CRITICA a BAJA.
3. Criticidad del dispositivo, de CRITICA a BAJA (CU-002-001, regla 3).
4. Fecha de detección, de la más reciente a la más vieja.

En la pantalla, los hallazgos críticos pendientes se marcan en rojo y los cerrados se atenúan.

## Comparación entre auditorías

Regla por regla (por código), con la auditoría anterior del mismo dispositivo:

| Categoría | Antes | Ahora |
|---|---|---|
| Nuevo desvío | cumplía o no se evaluaba | no cumple |
| Corregido | no cumplía | cumple |
| Persiste | no cumplía | no cumple |
| Sin cambios | cumplía | cumple |
| Regla nueva | no se evaluaba | cumple |
| Regla retirada | se evaluaba | no se evalúa |

## Casos de prueba manuales (para la matriz de CP)

| CP | Pasos | Resultado esperado |
|---|---|---|
| CP-B4-01 | Hallazgos → filtrar por estado ABIERTO y por dispositivo | La lista se actualiza; la URL de la consulta muestra los filtros |
| CP-B4-02 | Seleccionar un hallazgo → pasar a EN_REVISION sin comentario | Cambia el estado; el seguimiento muestra el cambio con usuario y fecha |
| CP-B4-03 | Pasar a RESUELTO sin comentario | No lo permite; con comentario sí |
| CP-B4-04 | Con un hallazgo RESUELTO, intentar EN_REVISION | Solo ofrece ABIERTO (reabrir, con comentario) |
| CP-B4-05 | Ingresar como responsable de gestión IT | Ve los hallazgos y su seguimiento, pero no puede cambiar el estado |
| CP-B4-06 | Bitácoras → TRX → entidad HALLAZGO | CAMBIO_ESTADO con el valor anterior y el nuevo |
| CP-B4-07 | Historial → filtrar por dispositivo → seleccionar la última auditoría | Configuración, baseline, reglas aplicadas y evolución respecto de la anterior |
| CP-B4-08 | Corregir la configuración, importarla, auditar y abrir esa auditoría en el historial | La regla corregida figura como "corregidos" |
| CP-B4-09 | Historial → "Ver configuración evaluada" | Muestra la configuración normalizada que se auditó |
| CP-B4-10 | Abrir `/results?auditoria=ID` y presionar "Limpiar" | Vuelven a verse todos los hallazgos y la URL queda sin `?auditoria` |
| CP-B4-11 | Abrir el mismo hallazgo ABIERTO en dos pestañas; en una pasarlo a RESUELTO y en la otra a ACEPTADO | La segunda responde 409 (el hallazgo ya está RESUELTO; solo se puede reabrir) y el seguimiento tiene un solo cambio |

## Pendiente de actualizar en la carpeta de tesis

- CU-005-001: listar los cuatro estados y sus transiciones, y agregar `GET /api/findings/{id}/history`.
- CU-005-002: agregar `GET /api/audits/{id}/comparison` y la pantalla de evolución.
- Tabla 46 (diccionario): `hallazgos_auditoria.fecha_estado`, `hallazgos_auditoria.usuario_estado` y la tabla
  `seguimientos_hallazgo`.
