## Integración continua y despliegue en Azure

### GitHub Actions

`.github/workflows/imagenes.yml` corre en cada push a `main` o `test` que toque código, pruebas o el workflow:

| Trabajo | Qué hace |
|---|---|
| Tests | `mvn test` de los dos servicios |
| Frontend | `npm ci`, `npm run check` y build |
| Pruebas de pantalla | Playwright contra el sitio compilado, y las de datos de demostración |
| Imágenes | Publica las 3 imágenes en GitHub Container Registry con las etiquetas `sha-<commit>` y la rama |
| Desplegar en Azure | Solo desde `main`: espera la **aprobación** del entorno `azure`, entra a Azure por OIDC (sin contraseñas), actualiza las 3 Container Apps a `sha-<commit>` y hace una prueba de humo |

Para aprobar: *Actions → la ejecución → Review deployments → azure → Approve and deploy*.

### Recursos en Azure

| Recurso | Nombre | Para qué |
|---|---|---|
| Grupo de recursos | `rg-auditnet` (Brazil South) | Todo lo de la plataforma |
| PostgreSQL Flexible Server | `psql-auditnet-lcaruso`, base `tfi_auditnet` | Datos y bitácoras (B1ms, copias 7 días) |
| Container Apps | `ca-auditnet-web`, `ca-auditnet-management`, `ca-auditnet-core` en `cae-auditnet` | La consola con ingreso público y los dos servicios con ingreso interno |
| Cosmos DB para MongoDB | `cosmos-auditnet-lcaruso` | Bitácora de excepciones (plan gratuito) |
| Storage | `stauditnetlcaruso`, contenedor `auditnet-archivos` | Repositorio de archivos |
| Communication Services | dominio administrado por Azure | Correo (5 por minuto y 10 por hora) |
| Logic App | revisión diaria a las 6:00 | Revisión periódica |

### Crear todo desde cero

En **Azure Cloud Shell (Bash)**, con el repositorio clonado:

```bash
git clone -b main https://github.com/Leonel-Caruso/tfi-auditnet.git
cd tfi-auditnet/infra/azure
bash 01-crear-base.sh            # grupo, PostgreSQL, base y entorno de Container Apps
bash 02-crear-apps.sh            # las 3 Container Apps desde las imágenes publicadas
bash 04-crear-cosmos.sh          # bitácora de excepciones
bash 05-crear-storage.sh         # repositorio de archivos
bash 06-configurar-github-oidc.sh  # despliegue desde GitHub sin contraseñas
bash 08-crear-email.sh           # correo
bash 09-crear-revision.sh        # revisión periódica
```

Los scripts piden las contraseñas al ejecutarse y las guardan como *secrets* de Container Apps; no hay secretos en el
repositorio. Son reejecutables. `variables.sh` tiene los nombres de los recursos y registra los proveedores que hacen
falta.

### Desplegar una versión a mano

```bash
cd ~/tfi-auditnet/infra/azure && bash 03-actualizar-imagenes.sh sha-<commit>
```

Cada servicio aplica sus migraciones al arrancar (management primero). El panel de administración muestra la etiqueta
desplegada.

### Seguridad del despliegue

- HTTPS obligatorio con HSTS; nginx agrega `Content-Security-Policy`, `Permissions-Policy`, `X-Frame-Options` y
  `Referrer-Policy`.
- Los servicios no tienen ingreso público: solo se llega por nginx. `/internal` no se publica, salvo la revisión
  periódica, que exige un secreto de 256 bits.
- PostgreSQL acepta conexiones de servicios de Azure (incluido Cloud Shell). Para conectarse desde una PC hace falta
  una regla de firewall temporal con su IP.
- GitHub Actions solo puede actualizar las 3 apps, y cada despliegue requiere aprobación manual.
