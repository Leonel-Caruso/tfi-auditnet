#!/usr/bin/env bash
# shellcheck disable=SC2034  # variables usadas por los scripts que hacen source de este archivo
# Variables comunes de los scripts de Azure. Sin secretos: las contraseñas se
# piden al ejecutar.

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
# El driver MongoDB Java 5.x exige API 4.2 o superior. Azure puede crear la cuenta en 3.6,
# que solo sube hasta 4.2: por eso 04-crear-cosmos.sh verifica la versión real.
MONGO_VERSION="4.2"

# Blob Storage. Nombre único en todo Azure: 3 a 24 minúsculas y números.
ST_CUENTA="stauditnetlcaruso"
ST_CONTENEDOR="auditnet-archivos"

# Container Apps
ENTORNO="cae-auditnet"
APP_MANAGEMENT="ca-auditnet-management"
APP_CORE="ca-auditnet-core"
APP_WEB="ca-auditnet-web"
# Trabajo programado de la revisión periódica (ver 09-crear-revision.sh). La hora va en UTC: 9:00 = 6:00 en Argentina.
# Va en un entorno propio: Azure pasó cae-auditnet al modo express, que no admite trabajos.
ENTORNO_TAREAS="cae-auditnet-tareas"
JOB_REVISION="caj-auditnet-revision"
CRON_REVISION="0 9 * * *"
IMAGEN_CURL="docker.io/curlimages/curl:8.11.1"

# Correo (Azure Communication Services). Los nombres son únicos en todo Azure.
ACS_RECURSO="acs-auditnet-lcaruso"
EMAIL_SERVICIO="ecs-auditnet-lcaruso"
# Dónde se guardan los datos del servicio de correo (no es la región de envío).
ACS_DATOS="United States"

# Imágenes publicadas por GitHub Actions (.github/workflows/imagenes.yml)
REGISTRO="ghcr.io/leonel-caruso"

# Despliegue desde GitHub Actions con OIDC (ver 06-configurar-github-oidc.sh).
IDENTIDAD_GITHUB="id-auditnet-github"
REPO_GITHUB="Leonel-Caruso/tfi-auditnet"
# GitHub también firma el token con los ids numéricos del dueño y del repo; Azure exige coincidencia exacta.
REPO_GITHUB_IDS="Leonel-Caruso@111381248/tfi-auditnet@1366741898"
ENTORNO_GITHUB="azure"

# Datos del administrador inicial (la contraseña se pide al ejecutar)
BOOTSTRAP_ADMIN_EMAIL="admin@auditnet.local"
BOOTSTRAP_ADMIN_USERNAME="admin"
BOOTSTRAP_ORG_IDENTIFIER="TFI-DEMO"
BOOTSTRAP_ORG_NAME="Organizacion Demo TFI"

# Pide la contraseña dos veces. Sin símbolos para no tener problemas de escape en Azure CLI.
# Uso: pedir_clave "Mensaje" NOMBRE_VARIABLE
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

# Una suscripción nueva trae pocos proveedores registrados y Azure falla con
# "MissingSubscriptionRegistration" al crear el primer recurso de cada tipo.
# Uso: registrar_proveedores Microsoft.Storage Microsoft.App ...
registrar_proveedores() {
  local proveedor estado
  for proveedor in "$@"; do
    estado=$(az provider show -n "$proveedor" --query registrationState -o tsv 2>/dev/null || echo "NotRegistered")
    if [[ "$estado" != "Registered" ]]; then
      echo "      Registrando el proveedor $proveedor (puede tardar unos minutos)..."
      az provider register --namespace "$proveedor" --wait -o none
    fi
  done
}
