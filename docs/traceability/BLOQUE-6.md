# Entrega 1 · Bloque 6 — Baselines + Reglas

## Objetivo

Implementar el segundo caso de uso de negocio de la Primera Entrega y uno de los casos de uso fuertes: definir el estado esperado de configuración y expresarlo mediante reglas técnicas verificables.

El Bloque 6 se construye acumulativamente sobre Dispositivos. No ejecuta todavía una auditoría; prepara el modelo de política que utilizará el Bloque 7.

## Correspondencia conceptual

```text
Dispositivo real
      ↓
Tipo de dispositivo
      ↓
Baseline esperada
      ↓
Reglas verificables
      ↓
[Bloque 7] comparación contra configuración
      ↓
Hallazgos
```

La separación evita mezclar responsabilidades: el Bloque 6 define política; el Bloque 7 ejecuta evaluación.

## Dominio incorporado

### BaselineConfiguracion

Campos principales:

- `id`;
- `nombre`;
- `descripcion`;
- `version`;
- `tipoDispositivoId`;
- `organizacionId`;
- `estado`.

Reglas de negocio iniciales:

- nombre y descripción obligatorios;
- tipo de dispositivo obligatorio y existente;
- organización obligatoria y existente;
- nombre no duplicado dentro de la misma organización;
- versión inicial `1`;
- estado inicial `ACTIVO`.

### ReglaBaseline

Campos principales:

- `id`;
- `baselineId`;
- `codigo`;
- `nombre`;
- `descripcion`;
- `tipo`;
- `patron`;
- `severidad`;
- `recomendacion`;
- `estado`.

Tipos iniciales:

- `DEBE_CONTENER`: la configuración cumple cuando contiene el patrón;
- `NO_DEBE_CONTENER`: la configuración cumple cuando no contiene el patrón.

Reglas de negocio iniciales:

- la baseline debe existir y estar activa;
- código obligatorio y normalizado a mayúsculas;
- código no duplicado dentro de la misma baseline;
- patrón obligatorio;
- severidad `BAJA`, `MEDIA`, `ALTA` o `CRITICA`;
- recomendación obligatoria;
- estado inicial `ACTIVA`.

## Persistencia

Nuevas tablas:

```text
baselines_configuracion
reglas_baseline
```

Relaciones principales:

```text
organizaciones_cliente 1 ─── N baselines_configuracion

tipos_dispositivo      1 ─── N baselines_configuracion

baselines_configuracion 1 ─── N reglas_baseline
```

Se mantiene PostgreSQL compartido para la Primera Entrega, consistente con la decisión ya adoptada para el MVP académico.

## Capas

### Dominio

`domain/model` contiene `BaselineConfiguracion`, `ReglaBaseline` y sus enums. `domain/repository` contiene puertos de persistencia sin dependencias de REST.

### Aplicación

`BaselineService` y `ReglaBaselineService` concentran reglas de negocio: validaciones, normalización, duplicados, estados iniciales y trazabilidad.

### Infraestructura

`BaselineEntity`, `ReglaBaselineEntity` y los repositorios Panache traducen dominio a PostgreSQL.

### API REST

Los recursos REST convierten DTOs, aplican seguridad por rol/organización y delegan lógica de negocio en los servicios de aplicación.

## Seguridad

Lectura de baselines y reglas:

- `ADMINISTRADOR_SISTEMA`;
- `ANALISTA_RED`;
- `AUDITOR_TECNICO`;
- `RESPONSABLE_GESTION_IT`.

Creación de baselines y reglas:

- `ADMINISTRADOR_SISTEMA`;
- `AUDITOR_TECNICO`.

El administrador puede operar sobre cualquier organización. Los demás perfiles solo reciben datos de la organización indicada por el claim `organizationId` del JWT. Un auditor técnico solo puede crear política para su propia organización.

## Frontend

### `/baselines`

- contador;
- búsqueda;
- filtro por estado;
- alta según rol;
- inventario de baselines;
- tipo de dispositivo;
- organización;
- versión;
- cantidad de reglas;
- inspector de detalle.

### `/rules`

- contador;
- búsqueda;
- filtro por severidad;
- alta según rol;
- catálogo de reglas;
- baseline asociada;
- condición;
- severidad;
- patrón;
- recomendación;
- inspector de detalle.

La interfaz conserva Command Rail, Console Header y estética técnica definida en Bloque 4.2.

## Trazabilidad temporal

Por ahora se mantiene el puerto `TrazabilidadPort` con adapter de logs. Se agregan eventos:

```text
BASELINE_CREADA
REGLA_CREADA
```

La bitácora persistente definitiva continúa diferida al componente de Trazabilidad definido por la arquitectura.

## Preparación para Bloque 7

El Bloque 6 deja disponibles los elementos necesarios para el próximo flujo fuerte:

```text
RTR-CORE-01
    + baseline
    + reglas
    + configuración capturada
        ↓
    ejecución de auditoría
        ↓
    cumplimiento / desvío
        ↓
    hallazgos
```

## Fuera de alcance deliberado

- edición y baja lógica desde UI;
- versionado incremental de baselines;
- reglas regex avanzadas;
- herencia de políticas;
- baseline global reutilizable entre organizaciones;
- aprobación/publicación de política;
- ejecución de auditoría;
- hallazgos persistentes.

Esos puntos no son necesarios para demostrar el caso de uso fuerte de definición de baseline y reglas en la Primera Entrega.
