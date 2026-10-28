#!/usr/bin/env bash
# Despliega otra versión de las imágenes; Flyway migra al arrancar cada servicio.
# Uso: bash 03-actualizar-imagenes.sh [etiqueta]   (main o sha-<commit>, preferible)
# APP_VERSION muestra la etiqueta desplegada en el panel de administración.
set -euo pipefail
cd "$(dirname "$0")"
source ./variables.sh
az config set extension.use_dynamic_install=yes_without_prompt --only-show-errors >/dev/null

# En el entorno rápido, actualizar la imagen puede dejar la app sin ingreso: se comprueba y se restituye.
asegurar_ingreso() {
  local app="$1" tipo="$2"
  if [ -z "$(az containerapp show -n "$app" -g "$RG" --query properties.configuration.ingress.fqdn -o tsv)" ]; then
    echo "$app quedó sin ingreso: se vuelve a habilitar ($tipo, puerto 8080)."
    az containerapp ingress enable -n "$app" -g "$RG" --type "$tipo" --target-port 8080 --transport auto -o none
  fi
}

ETIQUETA="${1:-main}"
az containerapp update -n "$APP_MANAGEMENT" -g "$RG" --image "$REGISTRO/auditnet-management:$ETIQUETA" \
  --set-env-vars "APP_VERSION=$ETIQUETA" -o none
asegurar_ingreso "$APP_MANAGEMENT" internal
az containerapp update -n "$APP_CORE" -g "$RG" --image "$REGISTRO/auditnet-core:$ETIQUETA" \
  --set-env-vars "APP_VERSION=$ETIQUETA" -o none
asegurar_ingreso "$APP_CORE" internal
az containerapp update -n "$APP_WEB" -g "$RG" --image "$REGISTRO/auditnet-web:$ETIQUETA" -o none
asegurar_ingreso "$APP_WEB" external
echo "Desplegada la etiqueta $ETIQUETA."
