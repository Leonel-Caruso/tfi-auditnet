# Guía de demo — Primera Entrega

Duración sugerida: **6 a 8 minutos**.

## 1. Presentación — 30 segundos

Explicar:

> AuditConfig es el MVP de una plataforma para auditar configuraciones de red. La Primera Entrega demuestra la arquitectura base, el Site Master y el flujo principal desde un dispositivo hasta un hallazgo técnico trazable.

## 2. Login y roles — 30 segundos

Ingresar como `analista_prueba`.

Mostrar que el perfil es `ANALISTA_RED` y que las opciones administrativas de alta que no corresponden al rol no aparecen.

Idea a defender:

```text
el frontend adapta la interfaz, pero la seguridad real está también en el backend
```

## 3. Dispositivo — 30 segundos

Ir a `DEV`.

Mostrar `RTR-CORE-01` y explicar:

- pertenece a una organización y sede;
- tiene tipo Cisco IOS/IOS-XE;
- es el activo concreto que se va a auditar.

## 4. Baseline y reglas — 1 minuto

Ir a `BL` y `RUL`.

Mostrar `Hardening Cisco IOS-XE` y las reglas:

```text
SEC-SSH-01
DEBE_CONTENER "ip ssh version 2"

SEC-TELNET-01
NO_DEBE_CONTENER "transport input telnet"
```

Idea a defender:

```text
la baseline representa el estado esperado; cada regla es una condición auditable
```

## 5. Primera configuración — 2 minutos

Ir a `RUN` y cargar:

```text
docs/demo/rtr-core-01-config-con-hallazgo.txt
```

Ejecutar auditoría.

Resultado esperado:

```text
2 reglas
1 cumple
1 hallazgo
severidad máxima = ALTA
resultado = CON_HALLAZGOS
```

Mostrar evaluación de las dos reglas.

## 6. Hallazgo — 1 minuto

Ir a `LOG`.

Mostrar el hallazgo de Telnet:

```text
regla       SEC-TELNET-01
condición   NO_DEBE_CONTENER
patrón      transport input telnet
severidad   ALTA
estado      ABIERTO
```

Mostrar evidencia y recomendación.

## 7. Corrección y nueva versión — 1 minuto

Volver a `RUN` y cargar:

```text
docs/demo/rtr-core-01-config-cumple.txt
```

Debe aparecer una nueva versión del mismo dispositivo.

Ejecutar la auditoría y mostrar:

```text
2 reglas
2 cumplen
0 hallazgos
resultado = CUMPLE
```

Explicar que la segunda versión no borra la evidencia histórica de la primera.

## 8. Dashboard y cierre — 30 segundos

Volver a Inicio y mostrar los contadores.

Frase de cierre sugerida:

> La Primera Entrega no intenta resolver todavía todo el producto final. Demuestra el flujo principal, la separación de responsabilidades, la persistencia y la seguridad sobre un caso de uso fuerte. Las siguientes entregas amplían versionado de políticas, trazabilidad, resultados, reportes, alertas e integración.

## Si el profesor pregunta por qué no hay conexión automática a routers

La carga manual es deliberada en el MVP. Permite validar primero el modelo de auditoría, versionado, baseline, reglas y hallazgos sin sumar todavía credenciales de equipos, protocolos de acceso ni automatización remota.

## Si pregunta por microservicios

La tesis define responsabilidades lógicas separadas. En la Entrega 1 se agrupan físicamente para evitar microservicios vacíos:

```text
management-service → Auth + B2B
audit-core-service → Técnico + Auditoría + resultados iniciales
```

La separación interna por capas y casos de uso permite dividir físicamente más adelante.
