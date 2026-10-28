# Deploy de AuditNet en Azure

Desde la Entrega 2, AuditNet se despliega en **Microsoft Azure** (suscripción *Azure for Students*).
Render queda congelado en la Entrega 1 (rama `main`) como respaldo hasta completar la migración.

## Arquitectura

```text
Navegador ──HTTPS──► ca-auditnet-web  (nginx: sitio Astro + API Gateway)   ingress EXTERNO
                          │  /api/auth, /api/users, /api/roles,
                          │  /api/client-organizations, /api/sites ──► ca-auditnet-management   ingress INTERNO
                          │  resto de /api/ ─────────────────────────► ca-auditnet-core         ingress INTERNO
                          ▼
              psql-auditnet-lcaruso (PostgreSQL Flexible Server, B1ms, backups 7 días)

ca-auditnet-core ──HTTPS──► stauditnetlcaruso / auditnet-archivos (Blob Storage, contenedor privado)
```

| Componente | Servicio de Azure | Configuración |
|---|---|---|
| Frontend + API Gateway | Container Apps (`ca-auditnet-web`) | nginx, 0,25 vCPU / 0,5 GiB, ingress externo con HTTPS |
| management-service | Container Apps (`ca-auditnet-management`) | Quarkus, 0,5 vCPU / 1 GiB, ingress interno |
| audit-core-service | Container Apps (`ca-auditnet-core`) | Quarkus, 0,5 vCPU / 1 GiB, ingress interno |
| Base de datos | PostgreSQL Flexible Server | Burstable B1ms, 32 GB, PostgreSQL 17, backups de 7 días |
| Bitácora de excepciones (NoSQL) | Cosmos DB para MongoDB (`cosmos-auditnet-lcaruso`) | API 4.2, plan gratuito, base `auditnet` con 400 RU/s compartidas, solo accesible desde servicios de Azure |
| Repositorio de archivos (Bloque 7) | Blob Storage (`stauditnetlcaruso`, contenedor `auditnet-archivos`) | Standard LRS, Hot, privado, TLS 1.2, papelera de 7 días; configuraciones, reportes CSV y respaldos |
| Imágenes Docker | GitHub Container Registry | Publicadas por `.github/workflows/imagenes.yml` |
| Despliegue continuo (Bloque 8) | GitHub Actions + identidad administrada `id-auditnet-github` | OIDC sin contraseñas; despliega solo con aprobación manual del entorno `azure` |
| HTTPS (Bloque 8) | Certificado administrado por Azure para `*.azurecontainerapps.io` | HTTP redirige a HTTPS; HSTS desde nginx |
| Región | `brazilsouth` | Región permitida por la suscripción más cercana a Argentina |

### Por qué un API Gateway con nginx

- El navegador habla con **un solo origen**: no hace falta CORS.
- Los servicios Quarkus **no son accesibles desde Internet** (ingress interno).
- Es el API Gateway previsto en el diagrama de componentes, resuelto de la forma más simple posible.

### Réplicas mínimas

Los tres contenedores tienen `min-replicas 0`: se apagan sin tráfico y no consumen créditos. La primera
solicitud después de un rato puede tardar hasta un minuto. Antes de una demo (Bloque 8):

```bash
bash 07-modo-demo.sh on       # 1 réplica fija en las 3 apps (y arranca PostgreSQL si estaba detenido)
bash 07-modo-demo.sh estado   # réplicas, estado de PostgreSQL y URL del sitio
bash 07-modo-demo.sh off      # al terminar: vuelve a 0 réplicas
```

Con el modo demo encendido las apps consumen la cuota gratuita de Container Apps aunque nadie las use: apagarlo
al terminar.

## Procedimiento

Los scripts están en `infra/azure/` y se ejecutan en **Azure Cloud Shell (Bash)**. No contienen secretos:
las contraseñas se piden al ejecutarlos y se guardan como *secrets* de Container Apps.

```bash
git clone -b main https://github.com/Leonel-Caruso/tfi-auditnet.git
cd tfi-auditnet/infra/azure
bash 01-crear-base.sh      # grupo de recursos, PostgreSQL, base y entorno de Container Apps
bash 02-crear-apps.sh      # las 3 Container Apps a partir de las imágenes de GHCR
bash 04-crear-cosmos.sh    # Cosmos DB para MongoDB y su conexión con ambos servicios (Bloque 2.3c)
bash 05-crear-storage.sh   # Blob Storage (repositorio de archivos) y su conexión con audit-core (Bloque 7)
bash 06-configurar-github-oidc.sh   # despliegue desde GitHub Actions sin contraseñas (Bloque 8)
```

Cada script registra antes los proveedores de recursos que necesita (`registrar_proveedores` en `variables.sh`):
en una suscripción nueva, crear el primer recurso de un tipo sin registrar su proveedor da `SubscriptionNotFound`.

`04-crear-cosmos.sh` verifica que la cuenta quede con la API de MongoDB 4.2 (el driver de los servicios no admite
versiones menores). Si Azure la crea con otra versión, el script se detiene e indica cómo subirla desde el Portal
(*Características > Upgrade MongoDB server version*); luego se vuelve a ejecutar sin recrear nada.

### Despliegue de una versión nueva

Después de un push a `main` el workflow queda esperando en **Desplegar en Azure (con aprobación)**: en *Actions → la ejecución → Review deployments → azure → Approve and deploy*. Despliega
`sha-<commit>` y hace una prueba de humo. La configuración de GitHub (entorno `azure` con revisor obligatorio y
variables `AZURE_CLIENT_ID`, `AZURE_TENANT_ID`, `AZURE_SUBSCRIPTION_ID`) está en `docs/entrega-2/BLOQUE-8.md`; desde el
Bloque 16, en *Deployment branches and tags* del entorno la rama permitida es `main`.

A mano, desde Cloud Shell (sigue disponible):

```bash
bash 03-actualizar-imagenes.sh sha-<commit>
```

## Seguridad

- Contraseñas y secreto JWT como *secrets* de Container Apps (no en Git, no en variables planas).
- PostgreSQL con TLS obligatorio (lo exige Azure) y acceso solo desde servicios de Azure.
  Para cargar datos desde una PC se agrega una regla de firewall temporal con su IP y se elimina al terminar.
- Backends sin exposición pública; solo `ca-auditnet-web` tiene ingress externo.
- HTTPS obligatorio con el certificado de Azure (HTTP redirige) y HSTS de un año (D-37).
- GitHub Actions entra a Azure por OIDC con una identidad con permisos solo sobre las 3 Container Apps (puede leer sus
  secretos y cambiar la imagen, no crear ni borrar recursos); por eso cada despliegue requiere aprobación manual (D-38).
- Limitación conocida: la regla "servicios de Azure" admite conexiones desde cualquier recurso de Azure
  que conozca las credenciales. La alternativa (red virtual privada) excede el alcance académico.

## Costos

- PostgreSQL B1ms con 32 GB: incluido en las 750 h/mes gratuitas de los primeros 12 meses.
- Blob Storage: 5 GB LRS (Hot) incluidos los primeros 12 meses; el prototipo usa unos pocos MB.
- Container Apps: cubierto por la cuota mensual gratuita (180.000 vCPU-s, 360.000 GiB-s, 2 M solicitudes).
- Cosmos DB para MongoDB: plan gratuito (1000 RU/s y 25 GB); la base usa 400 RU/s.
- GitHub Container Registry y GitHub Actions: gratuitos para repositorios públicos.
- **Impacto financiero pendiente:** el Excel debe reflejar el costo de operación con precios de Azure pago,
  no los créditos de estudiante.
- Recomendado: crear una alerta de presupuesto en *Cost Management* (50 % y 80 % de USD 100).
