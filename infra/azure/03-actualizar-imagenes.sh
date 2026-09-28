#!/usr/bin/env bash
# =====================================================================
# AuditNet · Despliega una nueva versión de las imágenes
# Uso: bash 03-actualizar-imagenes.sh [etiqueta]
#   etiqueta: entrega-2 (última) o sha-<commit> (versión exacta, recomendada)
# Las migraciones Flyway nuevas se aplican solas al arrancar cada servicio.
# =====================================================================
set -euo pipefail
cd "$(dirname "$0")"
source ./variables.sh
az config set extension.use_dynamic_install=yes_without_prompt --only-show-errors >/dev/null

ETIQUETA="${1:-entrega-2}"
az containerapp update -n "$APP_MANAGEMENT" -g "$RG" --image "$REGISTRO/auditnet-management:$ETIQUETA" -o none
az containerapp update -n "$APP_CORE" -g "$RG" --image "$REGISTRO/auditnet-core:$ETIQUETA" -o none
az containerapp update -n "$APP_WEB" -g "$RG" --image "$REGISTRO/auditnet-web:$ETIQUETA" -o none
echo "Desplegada la etiqueta $ETIQUETA."
