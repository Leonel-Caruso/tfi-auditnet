#!/usr/bin/env bash
# shellcheck disable=SC2034  # variables usadas por los scripts que hacen source de este archivo
# =====================================================================
# AuditNet · Variables comunes de la infraestructura en Azure
# No contiene secretos: las contraseñas se piden al ejecutar los scripts.
# =====================================================================

# Grupo de recursos y región (Azure for Students solo permite algunas regiones;
# brazilsouth es la permitida más cercana a Argentina).
RG="rg-auditnet"
UBICACION="brazilsouth"

# PostgreSQL Flexible Server (el nombre del servidor debe ser único en todo Azure)
PG_SERVIDOR="psql-auditnet-lcaruso"
PG_ADMIN="auditnetadmin"
PG_BASE="tfi_auditnet"
PG_VERSION="17"

# Cosmos DB para MongoDB (bitácora de excepciones). Nombre único en todo Azure.
COSMOS_CUENTA="cosmos-auditnet-lcaruso"
MONGO_BASE="auditnet"
MONGO_VERSION="6.0"

# Container Apps
ENTORNO="cae-auditnet"
APP_MANAGEMENT="ca-auditnet-management"
APP_CORE="ca-auditnet-core"
APP_WEB="ca-auditnet-web"

# Imágenes publicadas por GitHub Actions (.github/workflows/imagenes.yml)
REGISTRO="ghcr.io/leonel-caruso"

# Datos del administrador inicial (la contraseña se pide al ejecutar)
BOOTSTRAP_ADMIN_EMAIL="admin@auditnet.local"
BOOTSTRAP_ADMIN_USERNAME="admin"
BOOTSTRAP_ORG_IDENTIFIER="TFI-DEMO"
BOOTSTRAP_ORG_NAME="Organizacion Demo TFI"

# ---------------------------------------------------------------------
# Pide una contraseña dos veces y valida que no tenga símbolos, para
# evitar problemas al pasarla a Azure CLI: 16 a 64 caracteres, con
# mayúsculas, minúsculas y números.
# Uso: pedir_clave "Mensaje" NOMBRE_VARIABLE
# ---------------------------------------------------------------------
pedir_clave() {
  local mensaje="$1" destino="$2" clave repetida
  while true; do
    read -rsp "$mensaje: " clave; echo
    read -rsp "Repetila: " repetida; echo
    if [[ "$clave" != "$repetida" ]]; then
      echo "  No coinciden. Probá de nuevo."; continue
    fi
    if [[ ! "$clave" =~ ^[A-Za-z0-9]{16,64}$ || ! "$clave" =~ [A-Z] || ! "$clave" =~ [a-z] || ! "$clave" =~ [0-9] ]]; then
      echo "  Debe tener entre 16 y 64 caracteres, solo letras y números, con mayúsculas, minúsculas y números."; continue
    fi
    printf -v "$destino" '%s' "$clave"
    return 0
  done
}
