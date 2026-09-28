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
```

| Componente | Servicio de Azure | Configuración |
|---|---|---|
| Frontend + API Gateway | Container Apps (`ca-auditnet-web`) | nginx, 0,25 vCPU / 0,5 GiB, ingress externo con HTTPS |
| management-service | Container Apps (`ca-auditnet-management`) | Quarkus, 0,5 vCPU / 1 GiB, ingress interno |
| audit-core-service | Container Apps (`ca-auditnet-core`) | Quarkus, 0,5 vCPU / 1 GiB, ingress interno |
| Base de datos | PostgreSQL Flexible Server | Burstable B1ms, 32 GB, PostgreSQL 17, backups de 7 días |
| Imágenes Docker | GitHub Container Registry | Publicadas por `.github/workflows/imagenes.yml` |
| Región | `brazilsouth` | Región permitida por la suscripción más cercana a Argentina |

### Por qué un API Gateway con nginx

- El navegador habla con **un solo origen**: no hace falta CORS.
- Los servicios Quarkus **no son accesibles desde Internet** (ingress interno).
- Es el API Gateway previsto en el diagrama de componentes, resuelto de la forma más simple posible.

### Réplicas mínimas

Los tres contenedores tienen `min-replicas 0`: se apagan sin tráfico y no consumen créditos. La primera
solicitud después de un rato puede tardar hasta un minuto. Antes de una demo:

```bash
az containerapp update -n ca-auditnet-management -g rg-auditnet --min-replicas 1
az containerapp update -n ca-auditnet-core -g rg-auditnet --min-replicas 1
az containerapp update -n ca-auditnet-web -g rg-auditnet --min-replicas 1
# Después de la demo, volver a --min-replicas 0
```

## Procedimiento

Los scripts están en `infra/azure/` y se ejecutan en **Azure Cloud Shell (Bash)**. No contienen secretos:
las contraseñas se piden al ejecutarlos y se guardan como *secrets* de Container Apps.

```bash
git clone -b entrega-2 https://github.com/Leonel-Caruso/tfi-auditnet.git
cd tfi-auditnet/infra/azure
bash 01-crear-base.sh      # grupo de recursos, PostgreSQL, base y entorno de Container Apps
bash 02-crear-apps.sh      # las 3 Container Apps a partir de las imágenes de GHCR
```

Para desplegar una versión nueva después de un push (cuando GitHub Actions termina en verde):

```bash
bash 03-actualizar-imagenes.sh sha-<commit>
```

## Seguridad

- Contraseñas y secreto JWT como *secrets* de Container Apps (no en Git, no en variables planas).
- PostgreSQL con TLS obligatorio (lo exige Azure) y acceso solo desde servicios de Azure.
  Para cargar datos desde una PC se agrega una regla de firewall temporal con su IP y se elimina al terminar.
- Backends sin exposición pública; solo `ca-auditnet-web` tiene ingress externo.
- Limitación conocida: la regla "servicios de Azure" admite conexiones desde cualquier recurso de Azure
  que conozca las credenciales. La alternativa (red virtual privada) excede el alcance académico.

## Costos

- PostgreSQL B1ms con 32 GB: incluido en las 750 h/mes gratuitas de los primeros 12 meses.
- Container Apps: cubierto por la cuota mensual gratuita (180.000 vCPU-s, 360.000 GiB-s, 2 M solicitudes).
- GitHub Container Registry: gratuito para repositorios públicos.
- **Impacto financiero pendiente:** el Excel debe reflejar el costo de operación con precios de Azure pago,
  no los créditos de estudiante.
- Recomendado: crear una alerta de presupuesto en *Cost Management* (50 % y 80 % de USD 100).
