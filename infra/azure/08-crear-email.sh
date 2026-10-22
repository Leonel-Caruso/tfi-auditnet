#!/usr/bin/env bash
# Crea Azure Communication Services con un dominio administrado por Azure
# (remitente DoNotReply@<id>.azurecomm.net) y conecta management-service para
# enviar los avisos por correo. El dominio de Azure admite pocos envíos
# (5 por minuto y 10 por hora): alcanza para la demo; para más hace falta un
# dominio propio verificado.
# Requiere 01 y 02; es reejecutable.
# Uso: bash 08-crear-email.sh
set -euo pipefail
cd "$(dirname "$0")"
source ./variables.sh
az config set extension.use_dynamic_install=yes_without_prompt --only-show-errors >/dev/null

registrar_proveedores Microsoft.Communication

echo "[1/5] Servicio de correo $EMAIL_SERVICIO..."
if az communication email show -n "$EMAIL_SERVICIO" -g "$RG" -o none 2>/dev/null; then
  echo "      Ya existe: no se vuelve a crear."
else
  az communication email create -n "$EMAIL_SERVICIO" -g "$RG" --location global \
    --data-location "$ACS_DATOS" --tags proyecto=auditnet -o none
fi

echo "[2/5] Dominio administrado por Azure..."
if ! az communication email domain show --domain-name AzureManagedDomain --email-service-name "$EMAIL_SERVICIO" \
    -g "$RG" -o none 2>/dev/null; then
  az communication email domain create --domain-name AzureManagedDomain --email-service-name "$EMAIL_SERVICIO" \
    -g "$RG" --location global --domain-management AzureManaged -o none
fi
DOMINIO_ID=$(az communication email domain show --domain-name AzureManagedDomain \
  --email-service-name "$EMAIL_SERVICIO" -g "$RG" --query id -o tsv)
DOMINIO_REMITENTE=$(az communication email domain show --domain-name AzureManagedDomain \
  --email-service-name "$EMAIL_SERVICIO" -g "$RG" --query mailFromSenderDomain -o tsv)

echo "[3/5] Communication Services $ACS_RECURSO..."
if az communication show -n "$ACS_RECURSO" -g "$RG" -o none 2>/dev/null; then
  echo "      Ya existe: no se vuelve a crear."
else
  az communication create -n "$ACS_RECURSO" -g "$RG" --location global \
    --data-location "$ACS_DATOS" --tags proyecto=auditnet -o none
fi

echo "[4/5] Vinculando el dominio al recurso..."
ACS_ID=$(az communication show -n "$ACS_RECURSO" -g "$RG" --query id -o tsv)
az rest --method patch --url "https://management.azure.com${ACS_ID}?api-version=2023-04-01" \
  --body "{\"properties\": {\"linkedDomains\": [\"$DOMINIO_ID\"]}}" -o none

echo "[5/5] Conectando management-service (la cadena de conexión no se muestra)..."
CONEXION=$(az communication list-key -n "$ACS_RECURSO" -g "$RG" --query primaryConnectionString -o tsv)
DOMINIO_APPS=$(az containerapp env show -n "$ENTORNO" -g "$RG" --query properties.defaultDomain -o tsv)
az containerapp secret set -n "$APP_MANAGEMENT" -g "$RG" --secrets "correo-conexion=$CONEXION" -o none
az containerapp update -n "$APP_MANAGEMENT" -g "$RG" --set-env-vars \
  "CORREO_CONEXION=secretref:correo-conexion" \
  "CORREO_REMITENTE=DoNotReply@$DOMINIO_REMITENTE" \
  "APP_URL_PUBLICA=https://$APP_WEB.$DOMINIO_APPS" -o none

echo
echo "Listo. Remitente: DoNotReply@$DOMINIO_REMITENTE"
echo "En el panel de administración (ADM), 'Bandeja de salida' debería indicar Azure Communication Services."
echo "Probalo con «¿Olvidaste tu contraseña?» desde el login usando un usuario con correo real."
