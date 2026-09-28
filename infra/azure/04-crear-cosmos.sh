#!/usr/bin/env bash
# =====================================================================
# AuditNet · Paso 4 de infraestructura en Azure (Bloque 2.3c)
# Crea Cosmos DB para MongoDB (plan gratuito: 1000 RU/s y 25 GB) para la
# bitácora de excepciones, y conecta ambos servicios mediante un secreto.
# Requiere haber ejecutado 01 y 02. Tarda entre 5 y 10 minutos.
# Se puede volver a ejecutar: no recrea la cuenta ni la base si ya existen.
# Uso: bash 04-crear-cosmos.sh
# =====================================================================
set -euo pipefail
cd "$(dirname "$0")"
source ./variables.sh
az config set extension.use_dynamic_install=yes_without_prompt --only-show-errors >/dev/null

echo "[1/5] Cuenta de Cosmos DB para MongoDB (plan gratuito)..."
if az cosmosdb show -n "$COSMOS_CUENTA" -g "$RG" -o none 2>/dev/null; then
  echo "      La cuenta ya existe: no se vuelve a crear."
else
  # --ip-range-filter 0.0.0.0: solo acepta conexiones desde servicios de Azure (Container Apps).
  az cosmosdb create -n "$COSMOS_CUENTA" -g "$RG" \
    --kind MongoDB --server-version "$MONGO_VERSION" \
    --enable-free-tier true \
    --default-consistency-level Session \
    --locations regionName="$UBICACION" failoverPriority=0 isZoneRedundant=False \
    --ip-range-filter 0.0.0.0 \
    --tags proyecto=auditnet -o none
fi

echo "[2/5] Verificando la versión de la API de MongoDB..."
VERSION_REAL=$(az cosmosdb show -n "$COSMOS_CUENTA" -g "$RG" --query apiProperties.serverVersion -o tsv)
if [ "$VERSION_REAL" != "$MONGO_VERSION" ]; then
  echo
  echo "ERROR: la cuenta quedó con la API $VERSION_REAL y se necesita $MONGO_VERSION."
  echo "El driver de los servicios exige 4.2 o superior; con una versión menor la bitácora no se guarda."
  echo "Azure CLI no permite subir la versión. Hacerlo desde el Portal:"
  echo "  $COSMOS_CUENTA > Configuración > Características > Upgrade MongoDB server version > $MONGO_VERSION"
  echo "Cuando termine, volver a ejecutar este script (no recrea la cuenta)."
  exit 1
fi
echo "      API $VERSION_REAL"

echo "[3/5] Base $MONGO_BASE con 400 RU/s compartidas (dentro del plan gratuito)..."
if [ "$(az cosmosdb mongodb database exists -a "$COSMOS_CUENTA" -g "$RG" -n "$MONGO_BASE")" = "true" ]; then
  echo "      La base ya existe."
else
  az cosmosdb mongodb database create -a "$COSMOS_CUENTA" -g "$RG" -n "$MONGO_BASE" --throughput 400 -o none
fi

echo "[4/5] Cadena de conexión (se guarda solo como secreto de Container Apps)..."
URI=$(az cosmosdb keys list -n "$COSMOS_CUENTA" -g "$RG" --type connection-strings \
  --query "connectionStrings[0].connectionString" -o tsv)

echo "[5/5] Conectando management-service y audit-core-service..."
for APP in "$APP_MANAGEMENT" "$APP_CORE"; do
  az containerapp secret set -n "$APP" -g "$RG" --secrets "mongo-uri=$URI" -o none
  az containerapp update -n "$APP" -g "$RG" \
    --set-env-vars "MONGODB_URI=secretref:mongo-uri" "MONGODB_BASE=$MONGO_BASE" -o none
done

echo
echo "Listo. La bitácora de excepciones usa Cosmos DB: $COSMOS_CUENTA / $MONGO_BASE"
