#!/usr/bin/env bash
# Programa la revisión periódica: un trabajo de Container Apps que todos los días
# llama a POST /internal/review/run (a través de nginx) con un secreto compartido.
# El trabajo va en un entorno aparte (cae-auditnet-tareas): el de las apps está en
# modo express y Azure no admite trabajos ahí.
# También le indica a audit-core-service la dirección interna de management-service
# para pedirle que envíe los avisos apenas se guardan.
# Cada ejecución genera un secreto nuevo y lo carga en los tres lugares.
# Requiere 01 y 02 (y jq, que viene en Cloud Shell); es reejecutable.
# Uso: bash 09-crear-revision.sh
set -euo pipefail
cd "$(dirname "$0")"
source ./variables.sh
az config set extension.use_dynamic_install=yes_without_prompt --only-show-errors >/dev/null

URL_MANAGEMENT=$(az containerapp show -n "$APP_MANAGEMENT" -g "$RG" --query properties.configuration.ingress.fqdn -o tsv)
URL_WEB=$(az containerapp show -n "$APP_WEB" -g "$RG" --query properties.configuration.ingress.fqdn -o tsv)
# Solo hex: sin símbolos que haya que escapar.
SECRETO=$(openssl rand -hex 32)

echo "[1/4] Secreto de tareas en management-service..."
az containerapp secret set -n "$APP_MANAGEMENT" -g "$RG" --secrets "tarea-secreto=$SECRETO" -o none
az containerapp update -n "$APP_MANAGEMENT" -g "$RG" --set-env-vars "TAREA_SECRETO=secretref:tarea-secreto" -o none

echo "[2/4] Secreto de tareas y dirección de management-service en audit-core-service..."
az containerapp secret set -n "$APP_CORE" -g "$RG" --secrets "tarea-secreto=$SECRETO" -o none
az containerapp update -n "$APP_CORE" -g "$RG" --set-env-vars \
  "TAREA_SECRETO=secretref:tarea-secreto" "MANAGEMENT_URL=https://$URL_MANAGEMENT" -o none

echo "[3/4] Entorno $ENTORNO_TAREAS para el trabajo programado..."
if az containerapp env show -n "$ENTORNO_TAREAS" -g "$RG" -o none 2>/dev/null; then
  echo "      Ya existe: no se vuelve a crear."
else
  # Sin registros en Log Analytics: no genera costo mientras no corre nada (tarda unos minutos).
  az containerapp env create -n "$ENTORNO_TAREAS" -g "$RG" -l "$UBICACION" --logs-destination none \
    --tags proyecto=auditnet -o none
fi

# Las apps arrancan desde cero réplicas: curl reintenta hasta que responden.
# Las variables se expanden dentro del trabajo, no acá.
# shellcheck disable=SC2016
LLAMADA='curl -fsS --retry 6 --retry-all-errors --retry-delay 30 --max-time 600 -X POST -H "X-AuditNet-Tarea: $TAREA_SECRETO" "$URL_WEB/internal/review/run"'

echo "[4/4] Trabajo programado $JOB_REVISION ($CRON_REVISION UTC)..."
# Se crea con la API de Azure (PUT: crea o reemplaza): la CLI toma el "-c" de sh como una opción propia y
# rechaza el comando.
SUSCRIPCION=$(az account show --query id -o tsv)
ENTORNO_ID=$(az containerapp env show -n "$ENTORNO_TAREAS" -g "$RG" --query id -o tsv)
CUERPO=$(jq -n \
  --arg ubicacion "$UBICACION" --arg entorno "$ENTORNO_ID" --arg cron "$CRON_REVISION" \
  --arg secreto "$SECRETO" --arg imagen "$IMAGEN_CURL" --arg llamada "$LLAMADA" --arg url "https://$URL_WEB" \
  '{location: $ubicacion, tags: {proyecto: "auditnet"},
    properties: {
      environmentId: $entorno,
      configuration: {
        triggerType: "Schedule", replicaTimeout: 900, replicaRetryLimit: 1,
        scheduleTriggerConfig: {cronExpression: $cron, parallelism: 1, replicaCompletionCount: 1},
        secrets: [{name: "tarea-secreto", value: $secreto}]},
      template: {containers: [{
        name: "revision", image: $imagen, command: ["/bin/sh", "-c"], args: [$llamada],
        env: [{name: "TAREA_SECRETO", secretRef: "tarea-secreto"}, {name: "URL_WEB", value: $url}],
        resources: {cpu: 0.25, memory: "0.5Gi"}}]}}}')
az rest --method put -o none --body "$CUERPO" \
  --url "https://management.azure.com/subscriptions/$SUSCRIPCION/resourceGroups/$RG/providers/Microsoft.App/jobs/$JOB_REVISION?api-version=2024-03-01"

ESTADO=""
for _ in $(seq 1 30); do
  ESTADO=$(az containerapp job show -n "$JOB_REVISION" -g "$RG" --query properties.provisioningState -o tsv 2>/dev/null || true)
  [[ "$ESTADO" == "Succeeded" ]] && break
  sleep 10
done
echo "      Estado del trabajo: $ESTADO"

echo
echo "Listo. La revisión corre todos los días a las 6:00 (hora de Argentina)."
echo "Para probarla ya: az containerapp job start -n $JOB_REVISION -g $RG"
echo "y mirá el resultado en el portal (Trabajo > Historial de ejecuciones) o con el botón del panel ADM."
