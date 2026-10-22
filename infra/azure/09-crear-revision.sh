#!/usr/bin/env bash
# Programa la revisión periódica: un trabajo de Container Apps que todos los días
# llama a audit-core-service (POST /internal/review/run) con un secreto compartido.
# También le indica a audit-core-service la dirección interna de management-service
# para pedirle que envíe los avisos apenas se guardan.
# Cada ejecución genera un secreto nuevo y lo carga en los tres lugares.
# Requiere 01 y 02; es reejecutable.
# Uso: bash 09-crear-revision.sh
set -euo pipefail
cd "$(dirname "$0")"
source ./variables.sh
az config set extension.use_dynamic_install=yes_without_prompt --only-show-errors >/dev/null

URL_MANAGEMENT=$(az containerapp show -n "$APP_MANAGEMENT" -g "$RG" --query properties.configuration.ingress.fqdn -o tsv)
URL_CORE=$(az containerapp show -n "$APP_CORE" -g "$RG" --query properties.configuration.ingress.fqdn -o tsv)
# Solo hex: sin símbolos que haya que escapar.
SECRETO=$(openssl rand -hex 32)

echo "[1/3] Secreto de tareas en management-service..."
az containerapp secret set -n "$APP_MANAGEMENT" -g "$RG" --secrets "tarea-secreto=$SECRETO" -o none
az containerapp update -n "$APP_MANAGEMENT" -g "$RG" --set-env-vars "TAREA_SECRETO=secretref:tarea-secreto" -o none

echo "[2/3] Secreto de tareas y dirección de management-service en audit-core-service..."
az containerapp secret set -n "$APP_CORE" -g "$RG" --secrets "tarea-secreto=$SECRETO" -o none
az containerapp update -n "$APP_CORE" -g "$RG" --set-env-vars \
  "TAREA_SECRETO=secretref:tarea-secreto" "MANAGEMENT_URL=https://$URL_MANAGEMENT" -o none

# Los servicios arrancan desde cero réplicas: curl reintenta hasta que audit-core responde.
# Las variables se expanden dentro del trabajo, no acá.
# shellcheck disable=SC2016
LLAMADA='curl -fsS --retry 6 --retry-all-errors --retry-delay 30 --max-time 600 -X POST -H "X-AuditNet-Tarea: $TAREA_SECRETO" "$URL_CORE/internal/review/run"'

echo "[3/3] Trabajo programado $JOB_REVISION ($CRON_REVISION UTC)..."
if az containerapp job show -n "$JOB_REVISION" -g "$RG" -o none 2>/dev/null; then
  az containerapp job secret set -n "$JOB_REVISION" -g "$RG" --secrets "tarea-secreto=$SECRETO" -o none
  az containerapp job update -n "$JOB_REVISION" -g "$RG" --cron-expression "$CRON_REVISION" \
    --image "$IMAGEN_CURL" --set-env-vars "TAREA_SECRETO=secretref:tarea-secreto" "URL_CORE=https://$URL_CORE" \
    --command "/bin/sh" --args "-c" "$LLAMADA" -o none
else
  az containerapp job create -n "$JOB_REVISION" -g "$RG" --environment "$ENTORNO" \
    --trigger-type Schedule --cron-expression "$CRON_REVISION" \
    --replica-timeout 900 --replica-retry-limit 1 --parallelism 1 --replica-completion-count 1 \
    --image "$IMAGEN_CURL" --cpu 0.25 --memory 0.5Gi \
    --secrets "tarea-secreto=$SECRETO" \
    --env-vars "TAREA_SECRETO=secretref:tarea-secreto" "URL_CORE=https://$URL_CORE" \
    --command "/bin/sh" --args "-c" "$LLAMADA" -o none
fi

echo
echo "Listo. La revisión corre todos los días a las 6:00 (hora de Argentina)."
echo "Para probarla ya: az containerapp job start -n $JOB_REVISION -g $RG"
echo "y mirá el resultado en el portal (Trabajo > Historial de ejecuciones) o con el botón del panel ADM."
