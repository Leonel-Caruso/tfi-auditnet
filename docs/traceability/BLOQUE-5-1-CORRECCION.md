# Bloque 5.1 - Corrección de autorización por organización

Esta corrección se realiza durante la validación del Bloque 5 y no agrega un caso de uso nuevo.

## Problema detectado

Un usuario con rol `ANALISTA_RED` podía autenticarse y consultar `GET /api/devices`, pero la pantalla quedaba vacía porque el frontend necesitaba datos de organizaciones/sedes y esos endpoints estaban limitados al administrador.

Además, la consulta de dispositivos estaba protegida por rol pero no acotada por `organizationId`.

## Corrección

- Los roles técnicos pueden consultar únicamente su propia organización y sus sedes.
- El administrador conserva lectura global y creación de organizaciones/sedes.
- `GET /api/devices` devuelve todos los dispositivos al administrador y solo los de la organización del JWT para roles no administradores.
- `GET /api/devices/{id}` aplica el mismo aislamiento.
- `POST /api/devices` continúa reservado a `ADMINISTRADOR_SISTEMA`.
- Se incorporan las correcciones de validación ya detectadas: `quarkus.http.test-port=0` y clave JWT HS256 explícita en dev para `audit-core-service`.

## Resultado esperado

Un `ANALISTA_RED` asociado a la organización 2 puede ver `RTR-CORE-01` si pertenece a la organización 2, pero no puede registrar nuevos dispositivos ni consultar dispositivos de otras organizaciones.
