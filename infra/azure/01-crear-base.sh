#!/usr/bin/env bash
# =====================================================================
# AuditNet · Paso 1 de infraestructura en Azure
# Crea: grupo de recursos, PostgreSQL Flexible Server (B1ms, 32 GB,
# backups de 7 días), la base de datos y el entorno de Container Apps.
# Ejecutar en Azure Cloud Shell (Bash): bash 01-crear-base.sh
# Tarda entre 5 y 15 minutos (el servidor PostgreSQL es lo más lento).
# =====================================================================
set -euo pipefail
cd "$(dirname "$0")"
source ./variables.sh
az config set extension.use_dynamic_install=yes_without_prompt --only-show-errors >/dev/null

echo "Suscripción: $(az account show --query name -o tsv)"
echo "Región: $UBICACION · Grupo: $RG · Servidor PostgreSQL: $PG_SERVIDOR"
echo
echo "Elegí la contraseña del administrador de PostgreSQL ($PG_ADMIN)."
echo "Guardala en un lugar seguro FUERA del repositorio: la vas a necesitar en el paso 2."
pedir_clave "Contraseña de PostgreSQL" PG_CLAVE

echo; echo "[1/4] Grupo de recursos..."
az group create -n "$RG" -l "$UBICACION" --tags proyecto=auditnet entorno=tesis -o none

echo "[2/4] PostgreSQL Flexible Server (esto tarda varios minutos)..."
# B1ms + 32 GB = configuración incluida en las 750 h gratuitas de los primeros 12 meses.
# --public-access 0.0.0.0: solo admite conexiones desde servicios de Azure (Container Apps).
az postgres flexible-server create \
  -g "$RG" -n "$PG_SERVIDOR" -l "$UBICACION" \
  --tier Burstable --sku-name Standard_B1ms \
  --storage-size 32 --version "$PG_VERSION" \
  --backup-retention 7 \
  --public-access 0.0.0.0 \
  --admin-user "$PG_ADMIN" --admin-password "$PG_CLAVE" \
  --tags proyecto=auditnet \
  --yes -o none

echo "[3/4] Base de datos $PG_BASE..."
# El nombre del parámetro cambió entre versiones de Azure CLI (--database-name / --name).
if az postgres flexible-server db create --help 2>/dev/null | grep -q -- "--database-name"; then
  az postgres flexible-server db create -g "$RG" -s "$PG_SERVIDOR" --database-name "$PG_BASE" -o none
else
  az postgres flexible-server db create -g "$RG" -s "$PG_SERVIDOR" --name "$PG_BASE" -o none
fi

echo "[4/4] Entorno de Container Apps..."
az containerapp env create -n "$ENTORNO" -g "$RG" -l "$UBICACION" --tags proyecto=auditnet -o none

echo
echo "Listo. Resumen:"
echo "  PostgreSQL: $(az postgres flexible-server show -g "$RG" -n "$PG_SERVIDOR" --query fullyQualifiedDomainName -o tsv)"
echo "  Dominio de Container Apps: $(az containerapp env show -n "$ENTORNO" -g "$RG" --query properties.defaultDomain -o tsv)"
