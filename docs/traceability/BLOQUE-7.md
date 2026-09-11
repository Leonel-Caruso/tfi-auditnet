# Entrega 1 · Bloque 7 — Configuración + Auditoría + Hallazgos

## Objetivo

Implementar el caso de uso central de la Primera Entrega: tomar una configuración real de un dispositivo registrado, conservarla como evidencia versionada, compararla contra una baseline aplicable, evaluar sus reglas y persistir los resultados.

## Correspondencia con los casos de uso

### CU-004-001 — Importar configuración de dispositivo

Se implementa:

```text
POST /api/devices/{id}/configurations
```

Responsabilidades:

1. validar autenticación y rol;
2. verificar que el dispositivo exista;
3. verificar aislamiento por organización;
4. rechazar dispositivos inactivos;
5. rechazar contenido vacío;
6. conservar el contenido original;
7. generar una versión normalizada;
8. asignar versión incremental por dispositivo;
9. persistir fecha y usuario responsable;
10. registrar `CONFIGURACION_IMPORTADA` en la trazabilidad temporal.

### CU-004-002 — Ejecutar auditoría de configuración

Se implementa:

```text
GET  /api/configurations/auditable
POST /api/audits
GET  /api/audits/{id}
```

El flujo es:

```text
configuración
    ↓
dispositivo
    ↓
baseline ACTIVA de la misma organización y tipo
    ↓
reglas ACTIVAS
    ↓
MotorAuditoriaService
    ↓
evaluaciones persistidas
    ↓
hallazgos para incumplimientos
    ↓
auditoría FINALIZADA
```

### CU-005-001 — Consultar y priorizar hallazgos

Se implementa para el alcance de la Primera Entrega:

```text
GET /api/findings
GET /api/findings/{id}
```

Los hallazgos se ordenan primero por severidad y luego por fecha.

La modificación de estado `PATCH /api/findings/{id}/status` queda diferida porque no es necesaria para demostrar el flujo fuerte inicial.

## Dominio nuevo

### ConfiguracionDispositivo

Conserva:

- dispositivo;
- organización;
- versión;
- formato;
- nombre de fuente;
- contenido original;
- contenido normalizado;
- fecha de importación;
- usuario responsable.

### AuditoriaConfiguracion

Conserva:

- configuración evaluada;
- dispositivo;
- baseline;
- organización;
- fecha;
- ejecutor;
- reglas totales;
- reglas cumplidas;
- hallazgos;
- severidad máxima;
- resultado;
- estado.

### ResultadoReglaAuditoria

Es la evidencia de cada control aplicado, incluso cuando cumple.

### HallazgoAuditoria

Se crea solamente para una regla incumplida y conserva:

- referencia de auditoría/regla/dispositivo/baseline;
- código y nombre de regla;
- condición y patrón;
- severidad;
- evidencia;
- recomendación;
- estado inicial `ABIERTO`;
- fecha.

## Normalización

`NormalizacionConfiguracionService` no reemplaza la evidencia original.

La versión normalizada:

- convierte saltos CRLF/CR a LF;
- elimina espacios al final de cada línea;
- elimina blancos sobrantes al principio/final del documento.

El contenido original se persiste por separado sin ser sobrescrito.

## Motor de auditoría

`MotorAuditoriaService` interpreta las dos condiciones incorporadas en Bloque 6:

```text
DEBE_CONTENER
```

Cumple cuando el patrón está presente.

```text
NO_DEBE_CONTENER
```

Cumple cuando el patrón no está presente.

La comparación inicial es `case-insensitive` para evitar diferencias irrelevantes de mayúsculas/minúsculas en el MVP.

## Selección de baseline

La auditoría busca una baseline que cumpla:

```text
organizacionId = configuración.organizacionId
tipoDispositivoId = dispositivo.tipoDispositivoId
estado = ACTIVO
```

Si hubiera más de una candidata en este MVP, toma la versión/id más reciente. El versionado completo de políticas se seguirá desarrollando en entregas posteriores.

## Persistencia

Nuevas tablas:

```text
configuraciones_dispositivo
auditorias_configuracion
evaluaciones_regla_auditoria
hallazgos_auditoria
```

Relaciones conceptuales:

```text
dispositivos_red
   1 ── N configuraciones_dispositivo

configuraciones_dispositivo
   1 ── N auditorias_configuracion

baselines_configuracion
   1 ── N auditorias_configuracion

auditorias_configuracion
   1 ── N evaluaciones_regla_auditoria
   1 ── N hallazgos_auditoria

reglas_baseline
   1 ── N evaluaciones_regla_auditoria
   1 ── N hallazgos_auditoria
```

## Capas

### Dominio

Modelos y repositorios sin dependencias REST.

### Aplicación

- `ConfiguracionService`
- `NormalizacionConfiguracionService`
- `MotorAuditoriaService`
- `AuditoriaService`
- `HallazgoService`

### Infraestructura

Entidades JPA y repositorios Panache.

### API

- `ConfiguracionResource`
- `ConfiguracionDispositivoResource`
- `AuditoriaResource`
- `HallazgoResource`

## Frontend

### `/audits`

Integra:

- configuraciones auditables;
- importación de texto o archivo TXT/CFG;
- versionado visible;
- baseline aplicable;
- cantidad de reglas;
- ejecución;
- historial;
- inspector con evaluación regla por regla.

### `/results`

Integra:

- total de hallazgos;
- severidad crítica/alta;
- búsqueda;
- filtro por severidad;
- dispositivo;
- baseline;
- estado;
- evidencia;
- recomendación;
- inspector.

## Seguridad

Los recursos reutilizan el JWT HS256 generado por `management-service`.

Escritura técnica:

```text
ADMINISTRADOR_SISTEMA
ANALISTA_RED
AUDITOR_TECNICO
```

Consulta:

```text
ADMINISTRADOR_SISTEMA
ANALISTA_RED
AUDITOR_TECNICO
RESPONSABLE_GESTION_IT
```

Los perfiles no administradores solo acceden a datos de su propia organización.

## Trazabilidad temporal

Eventos incorporados:

```text
CONFIGURACION_IMPORTADA
AUDITORIA_EJECUTADA
```

La bitácora persistente e inmutable completa permanece como evolución posterior.

## Tests nuevos

- `NormalizacionConfiguracionServiceTest`
- `ConfiguracionServiceTest`
- `MotorAuditoriaServiceTest`

Se suman a los tests ya existentes de status, dispositivos, baselines y reglas.

## Preparación para Bloque 8

Con este bloque queda implementado el flujo funcional fuerte:

```text
dispositivo
→ configuración
→ baseline
→ reglas
→ auditoría
→ hallazgos
```

El Bloque 8 debe concentrarse en regresión completa, correcciones finales, integración, documentación de demo y cierre de la Primera Entrega, evitando sumar complejidad innecesaria.
