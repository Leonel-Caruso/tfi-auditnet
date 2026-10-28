## Repositorio y convenciones

```text
backend/management-service/   Quarkus: acceso, usuarios, organizaciones, planes, correo
backend/audit-core-service/   Quarkus: inventario, baselines, auditoría, hallazgos, reportes
frontend/web-app/             Astro: páginas, scripts, diccionarios, estilos y nginx
docker/                       PostgreSQL, MongoDB, Azurite y Mailpit para desarrollo local
infra/azure/                  Scripts de creación y operación en Azure (01 a 09)
scripts/                      load-env.ps1 y datos-demo/ (carga de demostración)
tests/pantallas/              Pruebas de pantalla (Playwright y API simulada)
tests/datos_demo/             Pruebas de los datos de demostración
tests/carga/                  Prueba de carga con k6
docs/                         Decisiones, migraciones, despliegue y manuales
.github/workflows/            CI, imágenes y despliegue
```

### Capas de cada servicio

```text
ar/edu/uai/tfi/<servicio>/
  api/rest/         recursos JAX-RS y DTO: validan la entrada, @RolesAllowed y alcance
  application/      casos de uso y puertos (application/port), sin infraestructura
  domain/           modelo (records y enums) y repositorios como interfaces
  infrastructure/   adaptadores: Panache, seguridad, correo, bitácoras, archivos, i18n
```

Las dependencias van hacia adentro: `api → application → domain`, e `infrastructure` implementa los puertos del
dominio y de la aplicación. Por eso los servicios de aplicación se prueban con implementaciones en memoria
(`src/test/.../support`).

### Convenciones

- **Nombres en español** en el dominio, los servicios y los textos; las rutas de la API en inglés
  (`/api/devices`, `/api/findings`).
- **Código simple y defendible:** sin comentarios extensos; solo los que explican un porqué.
- **Migraciones Flyway** en `src/main/resources/db/migration` de cada servicio: `V<n>__<descripcion>.sql`. Nunca se
  modifica una migración ya aplicada; los cambios van en una nueva. Historial: `flyway_historial_management` y
  `flyway_historial_audit_core`.
- **Decisiones** en `REGISTRO-DECISIONES.md` con número `D-xx`: qué se decidió, por qué, alternativas y prueba.
- **Textos de la interfaz** nunca escritos en el HTML ni en los scripts: siempre desde los diccionarios (ver
  *Cómo extender la plataforma*).
- **Sin secretos en Git:** `.env` está ignorado; `.env.example` lista las variables sin valores.
