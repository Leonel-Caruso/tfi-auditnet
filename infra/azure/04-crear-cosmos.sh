#!/usr/bin/env bash
# =====================================================================
# AuditNet · Paso 4 de infraestructura en Azure (Bloque 2.3c)
# Crea Cosmos DB para MongoDB (plan gratuito: 1000 RU/s y 25 GB) para la
# bitácora de excepciones, y conecta ambos servicios mediante un secreto.
# Requiere haber ejecutado 01 y 02. Tarda entre 5 y 10 minutos.
# Uso: bash 04-crear-cosmos.sh
# =====================================================================
set -euo pipefail
cd "$(dirname "$0")"
source ./variables.sh
az config set extension.use_dynamic_install=yes_without_prompt --only-show-errors >/dev/null

echo "[1/4] Cuenta de Cosmos DB para MongoDB (plan gratuito)..."
# --ip-range-filter 0.0.0.0: solo acepta conexiones desde servicios de Azure (Container Apps).
az cosmosdb create -n "$COSMOS_CUENTA" -g "$RG" \
  --kind MongoDB --server-version "$MONGO_VERSION" \
  --enable-free-tier true \
  --default-consistency-level Session \
  --locations regionName="$UBICACION" failoverPriority=0 isZoneRedundant=False \
  --ip-range-filter 0.0.0.0 \
  --tags proyecto=auditnet -o none

echo "[2/4] Base $MONGO_BASE con 400 RU/s compartidas (dentro del plan gratuito)..."
az cosmosdb mongodb database create -a "$COSMOS_CUENTA" -g "$RG" -n "$MONGO_BASE" --throughput 400 -o none

echo "[3/4] Cadena de conexión (se guarda solo como secreto de Container Apps)..."
URI=$(az cosmosdb keys list -n "$COSMOS_CUENTA" -g "$RG" --type connection-strings \
  --query "connectionStrings[0].connectionString" -o tsv)

echo "[4/4] Conectando management-service y audit-core-service..."
for APP in "$APP_MANAGEMENT" "$APP_CORE"; do
  az containerapp secret set -n "$APP" -g "$RG" --secrets "mongo-uri=$URI" -o none
  az containerapp update -n "$APP" -g "$RG" \
    --set-env-vars "MONGODB_URI=secretref:mongo-uri" "MONGODB_BASE=$MONGO_BASE" -o none
done

echo
echo "Listo. La bitácora de excepciones usa Cosmos DB: $COSMOS_CUENTA / $MONGO_BASE"
