# Bloque 3 — Activos, baselines y reglas

Requisitos cubiertos: RF-003, RF-004, RF-005, RF-006, RF-015 y RF-016 (CU-002-001, CU-003-001, CU-003-002,
CU-007-001 y CU-007-002). Las decisiones están en `REGISTRO-DECISIONES.md` (D-15 a D-19).

## API nueva o modificada (audit-core-service)

| Método y ruta | Perfiles | Qué hace | Bitácora |
|---|---|---|---|
| `PUT /api/devices/{id}` | Administrador | Modifica nombre, identificador, tipo, sede y criticidad | MODIFICACION |
| `PATCH /api/devices/{id}/status` | Administrador | `{"estado":"INACTIVO"}` baja lógica / `"ACTIVO"` reactiva | BAJA_LOGICA / CAMBIO_ESTADO |
| `POST /api/device-types` | Administrador | Ahora valida duplicados (409) y responde errores en JSON | ALTA (`TIPO_DISPOSITIVO`) |
| `GET /api/baselines` | Todos | Agrega `auditorias`: cuántas auditorías usaron cada versión | — |
| `PUT /api/baselines/{id}` | Administrador, auditor técnico | Modifica la descripción | MODIFICACION |
| `PATCH /api/baselines/{id}/status` | Administrador, auditor técnico | Activa o desactiva (una activa por alcance) | CAMBIO_ESTADO |
| `POST /api/baselines/{id}/versions` | Administrador, auditor técnico | Genera la versión siguiente, copia las reglas activas y la deja vigente | ALTA + CAMBIO_ESTADO |
| `POST /api/baselines/{id}/rules` | Administrador, auditor técnico | Ahora con `valorEsperado` e `impacto` | ALTA |
| `PUT /api/rules/{id}` | Administrador, auditor técnico | Modifica la regla (código y baseline fijos) | MODIFICACION |
| `PATCH /api/rules/{id}/status` | Administrador, auditor técnico | `{"estado":"INACTIVA"}` baja lógica / `"ACTIVA"` reactiva | BAJA_LOGICA / CAMBIO_ESTADO |

El auditor técnico solo opera sobre baselines y reglas de su organización. Errores: 400 (datos inválidos),
404 (no existe), 409 (conflicto de negocio, con el motivo en `message`).

## Reglas de negocio

1. El identificador de dispositivo es único **dentro de la organización**.
2. Un dispositivo inactivo no admite configuraciones nuevas ni auditorías; conserva su historial.
3. Solo una baseline **activa** por alcance (organización + tipo de dispositivo).
4. Una baseline usada en auditorías tiene sus reglas **congeladas**: para crear, cambiar o reactivar reglas se genera
   una versión nueva. La baja lógica de una regla sí se permite.
5. La versión nueva se numera después de la última de la misma baseline, aunque se genere desde una anterior.
6. Una regla `VALOR_ESPERADO` exige el valor esperado; los otros tipos no lo admiten. No puede haber dos reglas
   activas con la misma condición, ni contradictorias (DEBE_CONTENER y NO_DEBE_CONTENER del mismo patrón), en una
   baseline.

## Tipos de regla

| Tipo | Campos | Cumple si… | Ejemplo |
|---|---|---|---|
| `DEBE_CONTENER` | patrón | el patrón aparece en la configuración | `ip ssh version 2` |
| `NO_DEBE_CONTENER` | patrón | el patrón no aparece | `transport input telnet` |
| `VALOR_ESPERADO` | parámetro + valor | alguna línea que empieza con el parámetro tiene ese valor | `logging buffered` = `16384` |

La comparación no distingue mayúsculas; en `VALOR_ESPERADO` además ignora los espacios repetidos y el parámetro
debe coincidir con palabras completas.

## Casos de prueba manuales (para la matriz de CP)

| CP | Pasos | Resultado esperado |
|---|---|---|
| CP-B3-01 | Organizaciones → "+ organización" → "+ sede" | Ambas aparecen; en Bitácoras > TRX figuran ORGANIZACION y SEDE con ALTA |
| CP-B3-02 | Dispositivos → "+ tipo" | El tipo aparece al registrar dispositivos y baselines |
| CP-B3-03 | Registrar en otra organización un dispositivo con un identificador que ya existe en la primera | Se registra; en la misma organización responde "Ya existe…" |
| CP-B3-04 | Seleccionar un dispositivo → Editar → cambiar criticidad | TRX muestra MODIFICACION con valor anterior y nuevo |
| CP-B3-05 | "Dar de baja" y luego intentar importar una configuración para ese equipo | Estado INACTIVO; la importación se rechaza |
| CP-B3-06 | Crear una baseline para un tipo que ya tiene una activa en la organización | 409 con el nombre de la activa |
| CP-B3-07 | Baseline auditada → "Nueva versión" | v2 ACTIVA con las reglas copiadas; v1 INACTIVA; las auditorías previas siguen apuntando a v1 |
| CP-B3-08 | Intentar editar o crear una regla en la v1 auditada | Botón deshabilitado / 409 "generá una nueva versión" |
| CP-B3-09 | En la v2 crear una regla `VALOR_ESPERADO` y auditar una configuración que la cumple y otra que no | Cumple / hallazgo con "se esperaba: …" y el impacto en el detalle del hallazgo |
| CP-B3-10 | Desactivar una regla y volver a auditar | La regla no se evalúa; la auditoría anterior conserva su resultado |

## Pendiente de actualizar en la carpeta de tesis

- CU-002-001: renombrar a "Gestionar dispositivo de red" y agregar modificación y baja lógica (`PUT`, `PATCH`).
- CU-003-001: agregar `POST /api/baselines/{id}/versions` y la regla "reglas congeladas tras la primera auditoría".
- CU-003-002: reemplazar `/api/audit-rules` por `/api/rules` y `/api/baselines/{id}/rules`; agregar los tres tipos
  de regla.
- Tabla 46 (diccionario): `reglas_baseline.valor_esperado`, `reglas_baseline.impacto`, `hallazgos_auditoria.impacto`
  y la unicidad `(id_organizacion, identificador)` de `dispositivos_red`.
