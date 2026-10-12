#!/usr/bin/env bash
# Despliega otra versión de las imágenes; Flyway migra al arrancar cada servicio.
# Uso: bash 03-actualizar-imagenes.sh [etiqueta]   (entrega-2 o sha-<commit>, preferible)
# APP_VERSION muestra la etiqueta desplegada en el panel de administración.
set -euo pipefail
cd "$(dirname "$0")"
source ./variables.sh
az config set extension.use_dynamic_install=yes_without_prompt --only-show-errors >/dev/null

ETIQUETA="${1:-entrega-2}"
az containerapp update -n "$APP_MANAGEMENT" -g "$RG" --image "$REGISTRO/auditnet-management:$ETIQUETA" \
  --set-env-vars "APP_VERSION=$ETIQUETA" -o none
az containerapp update -n "$APP_CORE" -g "$RG" --image "$REGISTRO/auditnet-core:$ETIQUETA" \
  --set-env-vars "APP_VERSION=$ETIQUETA" -o none
az containerapp update -n "$APP_WEB" -g "$RG" --image "$REGISTRO/auditnet-web:$ETIQUETA" -o none
echo "Desplegada la etiqueta $ETIQUETA."
