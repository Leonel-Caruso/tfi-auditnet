#!/usr/bin/env bash
# =====================================================================
# AuditNet · Paso 5 de infraestructura en Azure (Bloque 7)
# Crea el repositorio de archivos: una cuenta de Blob Storage con un
# contenedor PRIVADO donde audit-core-service guarda las configuraciones
# importadas, los CSV de los reportes y los respaldos de la base.
#   - Standard LRS, nivel Hot, solo HTTPS con TLS 1.2, sin acceso anónimo.
#   - Papelera (soft delete) de 7 días para archivos y contenedores.
#   - La cadena de conexión se guarda solo como secreto de Container Apps.
# Requiere haber ejecutado 01 y 02. Se puede volver a ejecutar.
# Uso: bash 05-crear-storage.sh
# =====================================================================
set -euo pipefail
cd "$(dirname "$0")"
source ./variables.sh
az config set extension.use_dynamic_install=yes_without_prompt --only-show-errors >/dev/null

echo "[1/5] Cuenta de almacenamiento $ST_CUENTA..."
if az storage account show -n "$ST_CUENTA" -g "$RG" -o none 2>/dev/null; then
  echo "      La cuenta ya existe: no se vuelve a crear."
else
  az storage account create -n "$ST_CUENTA" -g "$RG" -l "$UBICACION" \
    --sku Standard_LRS --kind StorageV2 --access-tier Hot \
    --https-only true --min-tls-version TLS1_2 \
    --allow-blob-public-access false --allow-shared-key-access true \
    --tags proyecto=auditnet -o none
fi

echo "[2/5] Papelera de 7 días (un archivo borrado por error se puede recuperar)..."
az storage account blob-service-properties update --account-name "$ST_CUENTA" -g "$RG" \
  --enable-delete-retention true --delete-retention-days 7 \
  --enable-container-delete-retention true --container-delete-retention-days 7 -o none

echo "[3/5] Cadena de conexión (no se muestra)..."
CONEXION=$(az storage account show-connection-string -n "$ST_CUENTA" -g "$RG" --query connectionString -o tsv)
# Azure CLI la toma de esta variable: así no queda escrita en la línea de comandos.
export AZURE_STORAGE_CONNECTION_STRING="$CONEXION"

echo "[4/5] Contenedor privado $ST_CONTENEDOR..."
az storage container create -n "$ST_CONTENEDOR" --public-access off -o none

echo "[5/5] Conectando audit-core-service..."
az containerapp secret set -n "$APP_CORE" -g "$RG" --secrets "storage-conexion=$CONEXION" -o none
az containerapp update -n "$APP_CORE" -g "$RG" \
  --set-env-vars "AZURE_STORAGE_CONNECTION_STRING=secretref:storage-conexion" "AZURE_STORAGE_CONTAINER=$ST_CONTENEDOR" -o none

echo
echo "Listo. Repositorio de archivos: $ST_CUENTA / $ST_CONTENEDOR (privado)."
echo "En el panel de administración (ADM) debería verse 'Repositorio de archivos' operativo."
