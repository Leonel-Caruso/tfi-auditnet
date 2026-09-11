# Entrega 1 · Bloque 8 — Integración, regresión y cierre

## Objetivo

Cerrar la Primera Entrega sobre el Bloque 7 validado, sin agregar complejidad funcional innecesaria. El foco de este bloque es que lo ya implementado sea reproducible, defendible y estable para la demostración académica.

## Funcionalidad acumulada validable

### Acceso y contexto B2B

- autenticación;
- JWT;
- usuarios y roles;
- organizaciones y sedes;
- permisos por rol;
- aislamiento por organización.

### Inventario técnico

- tipos de dispositivo;
- dispositivos de red;
- criticidad y estado;
- asociación con organización y sede.

### Política de auditoría

- baselines activas;
- versión inicial visible;
- reglas asociadas;
- `DEBE_CONTENER`;
- `NO_DEBE_CONTENER`;
- severidad y recomendación.

### Auditoría

- importación manual `.txt` / `.cfg`;
- conservación de original;
- normalización;
- versionado de configuraciones;
- selección de baseline aplicable;
- evaluación regla por regla;
- auditoría persistida;
- hallazgos persistidos;
- historial y evidencia.

## Casos de uso cubiertos en la Entrega 1

```text
CU-001-001  Iniciar sesión
CU-002-001  Registrar dispositivo de red
CU-002-002  Consultar inventario de dispositivos
CU-003-001  Gestionar línea base — alcance inicial
CU-003-002  Gestionar reglas — alcance inicial
CU-004-001  Importar configuración de dispositivo
CU-004-002  Ejecutar auditoría de configuración
CU-005-001  Consultar y priorizar hallazgos — consulta inicial
CU-007-001  Registrar organización cliente — alcance inicial
CU-007-002  Gestionar sedes — alcance inicial
```

El caso fuerte de la entrega es `CU-004-002`, apoyado por importación, baseline, reglas y consulta de hallazgos.

## Ajustes técnicos de cierre

### Puerto de tests

`management-service` incorpora:

```properties
quarkus.http.test-port=0
```

De esta forma los tests Quarkus eligen un puerto libre y no chocan con una instancia de desarrollo levantada en `8081`.

`audit-core-service` ya utilizaba el mismo criterio.

### CORS local

Durante la validación se detectó que iniciar Astro en `127.0.0.1:4321` no era equivalente a `localhost:4321` para CORS. El Bloque 8 permite explícitamente ambos orígenes en desarrollo, manteniendo la restricción local.

### Sesión vencida

Si el token vence o el backend devuelve `401`, el frontend limpia la sesión y vuelve a `/login?reason=expired`. La pantalla muestra:

```text
Tu sesión expiró. Volvé a iniciar sesión para continuar.
```

Esto no cambia la seguridad; mejora la explicación para el usuario.

## Decisiones deliberadas para el MVP

### Endpoint de reglas

La documentación de tesis utiliza el nombre global:

```text
/api/audit-rules
```

La implementación de Entrega 1 utiliza:

```text
GET /api/rules
GET /api/rules/{id}
GET/POST /api/baselines/{baselineId}/rules
```

La ruta anidada hace explícita la relación regla-baseline. No cambia el concepto de dominio. La nomenclatura puede alinearse o exponerse como alias en una entrega posterior sin alterar el motor.

### Versionado de baseline/regla

En Entrega 1 se crea la política inicial y se conserva el campo de versión de baseline. La modificación con generación completa de historial de versiones queda para la evolución siguiente.

### Hallazgos

En Entrega 1 se implementa consulta y priorización inicial. El cambio de estado:

```text
PATCH /api/findings/{id}/status
```

queda diferido.

### Results / Traceability

Resultados y trazabilidad son responsabilidades lógicas reconocidas, pero no se crea todavía un microservicio físico adicional. Los resultados del flujo fuerte están dentro de `audit-core-service`, y la trazabilidad persistente completa queda diferida.

### Gateway

El frontend consume los dos servicios directamente en desarrollo. El API Gateway definitivo queda para una entrega posterior.

## Regresión esperada

```text
management-service
  2 tests
  0 failures
  0 errors

audit-core-service
  13 tests
  0 failures
  0 errors

frontend
  astro build
  10 rutas estáticas
```

Además de los tests automáticos, la regresión funcional debe demostrar:

```text
CFG-001 v1 → AUD-001 → 1 hallazgo ALTA
CFG-002 v2 → AUD-002 → CUMPLE → 0 hallazgos
```

El hallazgo de `AUD-001` debe conservarse después de la segunda auditoría y después de reiniciar los backends.

## Resultado del Bloque 8

Con este cierre la Primera Entrega queda preparada para ser presentada como un MVP académico funcional: arquitectura base, Site Master, seguridad, persistencia y al menos un caso de uso de negocio fuerte ejecutado de punta a punta.
