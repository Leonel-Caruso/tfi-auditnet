#!/usr/bin/env bash
# Deja las apps con 1 réplica mínima para una presentación (evita el arranque en
# frío) y arranca PostgreSQL si está detenido. "on" consume la cuota gratuita
# aunque no haya tráfico: volver a "off" al terminar.
# Uso: bash 07-modo-demo.sh on|off|estado
set -euo pipefail
cd "$(dirname "$0")"
source ./variables.sh
az config set extension.use_dynamic_install=yes_without_prompt --only-show-errors >/dev/null

ACCION="${1:-estado}"
APPS=("$APP_MANAGEMENT" "$APP_CORE" "$APP_WEB")

case "$ACCION" in
  on)
    ESTADO_PG=$(az postgres flexible-server show -g "$RG" -n "$PG_SERVIDOR" --query state -o tsv)
    if [[ "$ESTADO_PG" == "Stopped" ]]; then
      echo "Arrancando PostgreSQL (tarda unos minutos)..."
      az postgres flexible-server start -g "$RG" -n "$PG_SERVIDOR" -o none
    fi
    for APP in "${APPS[@]}"; do
      az containerapp update -n "$APP" -g "$RG" --min-replicas 1 -o none
      echo "  $APP: 1 réplica mínima"
    done
    echo "Modo demo ENCENDIDO. Acordate de ejecutar 'bash 07-modo-demo.sh off' al terminar."
    ;;
  off)
    for APP in "${APPS[@]}"; do
      az containerapp update -n "$APP" -g "$RG" --min-replicas 0 -o none
      echo "  $APP: 0 réplicas mínimas"
    done
    echo "Modo demo APAGADO: las apps se apagan solas a los pocos minutos sin tráfico."
    ;;
  estado)
    for APP in "${APPS[@]}"; do
      echo "  $APP: mínimo $(az containerapp show -n "$APP" -g "$RG" --query properties.template.scale.minReplicas -o tsv) réplica(s)"
    done
    echo "  PostgreSQL: $(az postgres flexible-server show -g "$RG" -n "$PG_SERVIDOR" --query state -o tsv)"
    echo "  Sitio: https://$(az containerapp show -n "$APP_WEB" -g "$RG" --query properties.configuration.ingress.fqdn -o tsv)"
    ;;
  *)
    echo "Uso: bash 07-modo-demo.sh on|off|estado"; exit 1
    ;;
esac
