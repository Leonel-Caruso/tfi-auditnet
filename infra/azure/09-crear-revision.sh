#!/usr/bin/env bash
# Programa la revisión periódica: una Logic App que todos los días a las 6:00 (hora
# de Argentina) llama a POST /internal/review/run, a través de nginx, con un secreto
# compartido. No se usa un trabajo de Container Apps porque Azure deja los entornos
# de esta suscripción en modo express, que no admite trabajos.
# También le indica a audit-core-service la dirección interna de management-service
# para pedirle que envíe los avisos apenas se guardan.
# Cada ejecución genera un secreto nuevo y lo carga en los tres lugares.
# Requiere 01 y 02 (y jq, que viene en Cloud Shell); es reejecutable.
# Uso: bash 09-crear-revision.sh           crea o actualiza todo
#      bash 09-crear-revision.sh probar    ejecuta la revisión ahora y muestra cómo terminó
set -euo pipefail
cd "$(dirname "$0")"
source ./variables.sh
az config set extension.use_dynamic_install=yes_without_prompt --only-show-errors >/dev/null

SUSCRIPCION=$(az account show --query id -o tsv)
LOGIC_URL="https://management.azure.com/subscriptions/$SUSCRIPCION/resourceGroups/$RG/providers/Microsoft.Logic/workflows/$LOGIC_REVISION"

if [[ "${1:-}" == "probar" ]]; then
  echo "Ejecutando la revisión..."
  az rest --method post -o none --url "$LOGIC_URL/triggers/Diario/run?api-version=2019-05-01"
  ESTADO=""
  for _ in $(seq 1 40); do
    sleep 15
    ESTADO=$(az rest --method get --url "$LOGIC_URL/runs?api-version=2019-05-01&\$top=1" \
      --query "value[0].properties.status" -o tsv)
    [[ "$ESTADO" != "Running" && "$ESTADO" != "Waiting" ]] && break
  done
  echo "Resultado: $ESTADO"
  [[ "$ESTADO" == "Succeeded" ]]
  exit
fi

registrar_proveedores Microsoft.Logic

URL_MANAGEMENT=$(az containerapp show -n "$APP_MANAGEMENT" -g "$RG" --query properties.configuration.ingress.fqdn -o tsv)
URL_WEB=$(az containerapp show -n "$APP_WEB" -g "$RG" --query properties.configuration.ingress.fqdn -o tsv)
# Solo hex: sin símbolos que haya que escapar.
SECRETO=$(openssl rand -hex 32)

echo "[1/3] Secreto de tareas en management-service..."
az containerapp secret set -n "$APP_MANAGEMENT" -g "$RG" --secrets "tarea-secreto=$SECRETO" -o none
az containerapp update -n "$APP_MANAGEMENT" -g "$RG" --set-env-vars "TAREA_SECRETO=secretref:tarea-secreto" -o none

echo "[2/3] Secreto de tareas y dirección de management-service en audit-core-service..."
az containerapp secret set -n "$APP_CORE" -g "$RG" --secrets "tarea-secreto=$SECRETO" -o none
az containerapp update -n "$APP_CORE" -g "$RG" --set-env-vars \
  "TAREA_SECRETO=secretref:tarea-secreto" "MANAGEMENT_URL=https://$URL_MANAGEMENT" -o none

echo "[3/3] Logic App $LOGIC_REVISION (todos los días a las 6:00, hora de Argentina)..."
# El secreto va como parámetro seguro y el historial de ejecuciones no muestra el pedido.
# Las apps arrancan desde cero réplicas: si no responden a tiempo, se reintenta.
CUERPO=$(jq -n --arg ubicacion "$UBICACION" --arg url "https://$URL_WEB/internal/review/run" --arg secreto "$SECRETO" \
  '{location: $ubicacion, tags: {proyecto: "auditnet"},
    properties: {
      state: "Enabled",
      parameters: {tareaSecreto: {value: $secreto}},
      definition: {
        "$schema": "https://schema.management.azure.com/providers/Microsoft.Logic/schemas/2016-06-01/workflowdefinition.json#",
        contentVersion: "1.0.0.0",
        parameters: {tareaSecreto: {type: "SecureString"}},
        triggers: {Diario: {type: "Recurrence", recurrence: {
          frequency: "Day", interval: 1, timeZone: "Argentina Standard Time",
          schedule: {hours: [6], minutes: [0]}}}},
        actions: {Revisar: {type: "Http", runAfter: {},
          inputs: {method: "POST", uri: $url,
            headers: {"X-AuditNet-Tarea": "@parameters('"'"'tareaSecreto'"'"')"},
            retryPolicy: {type: "fixed", count: 4, interval: "PT1M"}},
          runtimeConfiguration: {secureData: {properties: ["inputs"]}}}},
        outputs: {}}}}')
az rest --method put -o none --body "$CUERPO" --url "$LOGIC_URL?api-version=2019-05-01"

echo
echo "Listo. La revisión corre todos los días a las 6:00 (hora de Argentina)."
echo "Para probarla ya: bash infra/azure/09-crear-revision.sh probar"
