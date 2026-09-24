# Migraciones de base de datos (Flyway)

## Por qué

Hasta la Entrega 1, Hibernate creaba y modificaba las tablas automáticamente (`schema-management=update`).
Eso tiene tres problemas para AuditNet:

1. **No deja historial** de cómo evolucionó el esquema. Los Puntos Mandatorios 2026 piden
   "histórico de modelo de datos (schema) … recuperar datos y estructura a través del tiempo".
2. **No es reproducible**: dos bases pueden terminar con estructuras distintas según qué versión del código las tocó.
3. **No actualiza restricciones existentes**. Ejemplo real: Hibernate generó
   `hallazgos_auditoria_estado_check CHECK (estado IN ('ABIERTO'))`. Si agregamos un estado nuevo
   (`EN_REVISION`), `update` no modifica ese CHECK y el `INSERT` falla en ejecución.

Desde el Bloque 2.1 el esquema lo administra **Flyway** y Hibernate solo lo **valida** (`validate`).

## Organización

| Servicio | Tablas propias | Carpeta | Tabla de historial |
|---|---|---|---|
| `management-service` | organizaciones_cliente, sedes, roles, usuarios, usuarios_roles | `backend/management-service/src/main/resources/db/migration` | `flyway_historial_management` |
| `audit-core-service` | tipos_dispositivo, dispositivos_red, configuraciones_dispositivo, baselines_configuracion, reglas_baseline, auditorias_configuracion, evaluaciones_regla_auditoria, hallazgos_auditoria | `backend/audit-core-service/src/main/resources/db/migration` | `flyway_historial_audit_core` |

Ambos servicios comparten la misma base PostgreSQL. Cada uno migra **solo sus tablas** y registra su propio historial.

`audit-core-service` referencia `organizaciones_cliente` y `sedes`, por lo que **`management-service` debe migrar primero**.
En local: levantar `management-service` antes que `audit-core-service`. En la nube, si `audit-core-service` arranca antes,
su migración falla sin dejar cambios (PostgreSQL revierte el DDL) y se completa en el reinicio siguiente.

`baseline-on-migrate=true` con `baseline-version=0` permite que el segundo servicio migre sobre una base que ya contiene
las tablas del primero, **sin saltear su V1**.

## Versiones

| Versión | Servicio | Descripción | Bloque |
|---|---|---|---|
| V1 | management-service | Esquema inicial de la Entrega 1 (acceso y B2B) | 2.1 |
| V1 | audit-core-service | Esquema inicial de la Entrega 1 (núcleo de auditoría) | 2.1 |

La V1 reproduce exactamente la estructura generada por Hibernate en la Entrega 1 (mismas columnas, tipos, nulabilidad,
claves primarias, únicas y CHECK). Única diferencia deliberada: las claves foráneas tienen nombres legibles
(`fk_<tabla>_<referencia>`) en lugar de los nombres aleatorios de Hibernate.

## Reglas

- Nunca editar una migración ya aplicada. Todo cambio es una migración nueva: `V2__descripcion.sql`, `V3__…`.
- Migraciones chicas, con un objetivo por archivo.
- Sin pérdida de datos: si una columna cambia, primero se agrega la nueva, se copian los datos y recién después se retira la anterior.
- Toda migración se prueba sobre una base limpia antes del commit.
- No se hacen cambios manuales sobre bases desplegadas.

## Verificación

Consultar el historial aplicado:

```sql
SELECT version, description, installed_on, success FROM flyway_historial_management ORDER BY installed_rank;
SELECT version, description, installed_on, success FROM flyway_historial_audit_core ORDER BY installed_rank;
```
