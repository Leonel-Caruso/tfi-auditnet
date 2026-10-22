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
| V2 | management-service | Bitácora de auditoría de sistema (`bitacora_sistema`, solo inserción) | 2.3a |
| V3 | management-service | Bitácora de transacciones (`bitacora_transacciones`, compartida, solo inserción) | 2.3b |
| V2 | audit-core-service | Identificador de dispositivo único por organización, versionado de baselines, reglas VALOR_ESPERADO e impacto | 3 |
| V3 | audit-core-service | Seguimiento de hallazgos: estados, último cambio y `seguimientos_hallazgo` (solo inserción) | 4 |
| V4 | audit-core-service | Alertas críticas (`alertas_criticas`, sin borrado) y alta de las alertas de los hallazgos CRITICA/ALTA existentes | 5 |
| V5 | audit-core-service | Reportes de auditoría (`reportes_auditoria` y `reportes_auditoria_auditorias`, solo inserción) | 5 |
| V6 | audit-core-service | Cierra las alertas duplicadas que dejó V4 (una vigente por dispositivo y regla, D-26) | 5 |
| V4 | management-service | Seguridad de cuentas: intentos fallidos, bloqueo temporal, último acceso y `sesiones_validas_desde` en `usuarios`; descripciones de los cuatro perfiles (D-29 a D-31) | 6 |
| V7 | audit-core-service | Repositorio de archivos: catálogo `archivos_repositorio` (configuraciones, reportes y respaldos con SHA-256, solo inserción) (D-35) | 7 |
| V5 | management-service | Planes: catálogo `planes`, plan asignado y límites acordados en `organizaciones_cliente` (las existentes quedan en Inicial) y `solicitudes_cambio_plan` (una pendiente por organización) (D-46) | 11 |

Desde la V5 de management, audit-core lee el plan de `planes` y `organizaciones_cliente` con una consulta nativa:
al desplegar, management-service va primero (D-47).

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

## Respaldos y recuperación (Bloque 7)

El historial de Flyway permite reconstruir la **estructura** de cualquier versión (las migraciones están en Git). Los
**datos** se recuperan con los respaldos lógicos que genera el panel de administración: ver `RESPALDOS.md`.
