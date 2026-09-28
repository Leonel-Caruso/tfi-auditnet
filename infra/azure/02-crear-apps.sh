#!/usr/bin/env bash
# =====================================================================
# AuditNet · Paso 2 de infraestructura en Azure
# Crea las 3 Container Apps a partir de las imágenes publicadas en GHCR:
#   - management-service y audit-core-service con ingress INTERNO
#     (no son accesibles desde Internet).
#   - web (nginx + Astro) con ingress EXTERNO: sirve el sitio y enruta
#     /api/... a los dos servicios (API Gateway simple).
# Requiere haber ejecutado 01-crear-base.sh y que GitHub Actions haya
# publicado las imágenes.
# Uso: bash 02-crear-apps.sh [etiqueta]   (por defecto: entrega-2)
# =====================================================================
set -euo pipefail
cd "$(dirname "$0")"
source ./variables.sh
az config set extension.use_dynamic_install=yes_without_prompt --only-show-errors >/dev/null

ETIQUETA="${1:-entrega-2}"
PG_HOST=$(az postgres flexible-server show -g "$RG" -n "$PG_SERVIDOR" --query fullyQualifiedDomainName -o tsv)
DOMINIO=$(az containerapp env show -n "$ENTORNO" -g "$RG" --query properties.defaultDomain -o tsv)

echo "Imágenes: $REGISTRO/auditnet-*:$ETIQUETA"
echo "PostgreSQL: $PG_HOST"
echo
pedir_clave "Contraseña de PostgreSQL (la del paso 1)" PG_CLAVE
echo "Elegí la contraseña del usuario '$BOOTSTRAP_ADMIN_USERNAME' de AuditNet (guardala fuera del repo)."
pedir_clave "Contraseña del admin de AuditNet" ADMIN_CLAVE

# Secreto compartido para firmar y verificar los JWT (solo hex: sin símbolos).
JWT_SECRETO=$(openssl rand -hex 32)

VARIABLES_DB=(
  "PORT=8080"
  "DB_HOST=$PG_HOST" "DB_PORT=5432" "DB_NAME=$PG_BASE" "DB_USERNAME=$PG_ADMIN"
  "DB_PASSWORD=secretref:db-password"
  "JWT_SECRET=secretref:jwt-secret"
  "FRONTEND_ORIGIN=https://$APP_WEB.$DOMINIO"
)

echo; echo "[1/3] management-service (migra primero su esquema con Flyway)..."
az containerapp create -n "$APP_MANAGEMENT" -g "$RG" --environment "$ENTORNO" \
  --image "$REGISTRO/auditnet-management:$ETIQUETA" \
  --ingress internal --target-port 8080 \
  --cpu 0.5 --memory 1.0Gi --min-replicas 0 --max-replicas 1 \
  --secrets "db-password=$PG_CLAVE" "jwt-secret=$JWT_SECRETO" "bootstrap-admin-password=$ADMIN_CLAVE" \
  --env-vars "${VARIABLES_DB[@]}" \
    "BOOTSTRAP_ADMIN_EMAIL=$BOOTSTRAP_ADMIN_EMAIL" \
    "BOOTSTRAP_ADMIN_USERNAME=$BOOTSTRAP_ADMIN_USERNAME" \
    "BOOTSTRAP_ADMIN_PASSWORD=secretref:bootstrap-admin-password" \
    "BOOTSTRAP_ORG_IDENTIFIER=$BOOTSTRAP_ORG_IDENTIFIER" \
    "BOOTSTRAP_ORG_NAME=$BOOTSTRAP_ORG_NAME" \
  --tags proyecto=auditnet -o none

echo "[2/3] audit-core-service..."
az containerapp create -n "$APP_CORE" -g "$RG" --environment "$ENTORNO" \
  --image "$REGISTRO/auditnet-core:$ETIQUETA" \
  --ingress internal --target-port 8080 \
  --cpu 0.5 --memory 1.0Gi --min-replicas 0 --max-replicas 1 \
  --secrets "db-password=$PG_CLAVE" "jwt-secret=$JWT_SECRETO" \
  --env-vars "${VARIABLES_DB[@]}" \
  --tags proyecto=auditnet -o none

URL_MANAGEMENT=$(az containerapp show -n "$APP_MANAGEMENT" -g "$RG" --query properties.configuration.ingress.fqdn -o tsv)
URL_CORE=$(az containerapp show -n "$APP_CORE" -g "$RG" --query properties.configuration.ingress.fqdn -o tsv)

echo "[3/3] web (Astro + nginx como API Gateway)..."
az containerapp create -n "$APP_WEB" -g "$RG" --environment "$ENTORNO" \
  --image "$REGISTRO/auditnet-web:$ETIQUETA" \
  --ingress external --target-port 8080 \
  --cpu 0.25 --memory 0.5Gi --min-replicas 0 --max-replicas 1 \
  --env-vars "MANAGEMENT_UPSTREAM=https://$URL_MANAGEMENT" "CORE_UPSTREAM=https://$URL_CORE" \
  --tags proyecto=auditnet -o none

echo
echo "Listo. AuditNet quedó publicado en:"
echo "  https://$(az containerapp show -n "$APP_WEB" -g "$RG" --query properties.configuration.ingress.fqdn -o tsv)"
echo "La primera carga puede tardar hasta un minuto: los servicios arrancan cuando reciben tráfico."
