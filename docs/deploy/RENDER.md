# Deploy de AuditNet en Render

Esta carpeta esta preparada para desplegar la Entrega 1 como monorepo:

- `tfi-auditnet-web`: Astro como Static Site.
- `tfi-auditnet-management`: Quarkus/Docker.
- `tfi-auditnet-core`: Quarkus/Docker.
- `tfi-auditnet-db`: PostgreSQL administrado.

## Seguridad

No subir `.env` a GitHub. El repositorio incluye `.env.example` solamente.
`render.yaml` no contiene contrasenas reales. Render solicitara `BOOTSTRAP_ADMIN_PASSWORD`
durante la creacion inicial del Blueprint. `JWT_SECRET` se genera dentro de Render y se
comparte con `audit-core-service` sin exponerlo en Git.

## Flujo recomendado

1. Crear un repositorio GitHub privado vacio llamado `tfi-auditnet`.
2. Subir el contenido de esta carpeta con Git.
3. En Render elegir **New > Blueprint** y conectar ese repositorio.
4. Render detectara `render.yaml`.
5. Cuando lo solicite, completar `BOOTSTRAP_ADMIN_PASSWORD` con una clave exclusiva para cloud.
6. Crear/sincronizar el Blueprint.
7. Esperar a que PostgreSQL y los dos servicios Quarkus queden disponibles.
8. Esperar a que finalice el build del sitio Astro.
9. Abrir la URL de `tfi-auditnet-web`.

## Primer arranque

La base cloud empieza vacia. `management-service` crea de forma idempotente:

- roles base;
- organizacion `TFI-DEMO`;
- usuario `admin` con la clave definida en Render.

Los datos funcionales de demo (tipo de dispositivo, cliente demo, sede, dispositivo,
baseline, reglas, configuraciones y auditorias) se cargan despues de comprobar que el
deploy base funciona. No se copian automaticamente desde PostgreSQL local.

## Variables principales

Render resuelve automaticamente mediante `render.yaml`:

- URL/contrasena/usuario de PostgreSQL;
- URL publica del frontend para CORS;
- URL publica de ambos backends para Astro;
- secreto JWT compartido entre ambos backends.

## Comportamiento local

La copia sigue funcionando localmente con `.env` y `scripts/load-env.ps1`.
`PORT` es opcional localmente: management usa 8081 y audit-core 8082 por defecto.

## Nota sobre plan gratuito

Los servicios gratuitos pueden entrar en reposo por inactividad. Para una demo,
abrir la aplicacion unos minutos antes. La base gratuita se utiliza solo para la
presentacion/validacion academica y no debe considerarse almacenamiento permanente.
