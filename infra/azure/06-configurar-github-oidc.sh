#!/usr/bin/env bash
# Configura OIDC para que GitHub Actions despliegue sin contraseñas: identidad
# administrada, credencial federada para el entorno $ENTORNO_GITHUB, Reader del grupo
# y Contributor solo sobre las 3 apps (puede leer sus secretos: el entorno exige aprobación).
# Uso: bash 06-configurar-github-oidc.sh   (requiere 01, 02 y ser Owner; es reejecutable)
set -euo pipefail
cd "$(dirname "$0")"
source ./variables.sh
az config set extension.use_dynamic_install=yes_without_prompt --only-show-errors >/dev/null

registrar_proveedores Microsoft.ManagedIdentity

echo "[1/3] Identidad administrada $IDENTIDAD_GITHUB..."
if ! az identity show -g "$RG" -n "$IDENTIDAD_GITHUB" -o none 2>/dev/null; then
  az identity create -g "$RG" -n "$IDENTIDAD_GITHUB" -l "$UBICACION" --tags proyecto=auditnet -o none
fi
CLIENT_ID=$(az identity show -g "$RG" -n "$IDENTIDAD_GITHUB" --query clientId -o tsv)
PRINCIPAL_ID=$(az identity show -g "$RG" -n "$IDENTIDAD_GITHUB" --query principalId -o tsv)

echo "[2/3] Credencial federada para repo:$REPO_GITHUB:environment:$ENTORNO_GITHUB..."
CREDENCIAL="github-${ENTORNO_GITHUB}"
if ! az identity federated-credential show -g "$RG" --identity-name "$IDENTIDAD_GITHUB" -n "$CREDENCIAL" -o none 2>/dev/null; then
  az identity federated-credential create -g "$RG" --identity-name "$IDENTIDAD_GITHUB" -n "$CREDENCIAL" \
    --issuer "https://token.actions.githubusercontent.com" \
    --subject "repo:${REPO_GITHUB}:environment:${ENTORNO_GITHUB}" \
    --audiences "api://AzureADTokenExchange" -o none
fi

echo "[3/3] Permisos acotados..."
asignar() {
  local rol="$1" alcance="$2"
  local existentes
  # Un alcance vacío haría que Azure CLI asigne el rol sobre TODA la suscripción: se corta antes.
  if [[ ! "$alcance" =~ ^/subscriptions/[^/]+/resourceGroups/[^/]+ ]]; then
    echo "ERROR: alcance inválido para $rol: '$alcance'. ¿Se ejecutó 02-crear-apps.sh?"; exit 1
  fi
  existentes=$(az role assignment list --assignee "$PRINCIPAL_ID" --role "$rol" --scope "$alcance" --query "length(@)" -o tsv)
  if [[ "$existentes" == "0" ]]; then
    # --assignee-principal-type evita esperar a que la identidad recién creada se replique en Entra ID.
    az role assignment create --assignee-object-id "$PRINCIPAL_ID" --assignee-principal-type ServicePrincipal \
      --role "$rol" --scope "$alcance" -o none
  fi
  echo "      $rol en ${alcance##*/}"
}
# Cada alcance se calcula en su propia línea: así, si az falla, set -e detiene el script.
ALCANCE_RG=$(az group show -n "$RG" --query id -o tsv)
asignar "Reader" "$ALCANCE_RG"
for APP in "$APP_MANAGEMENT" "$APP_CORE" "$APP_WEB"; do
  ALCANCE_APP=$(az containerapp show -n "$APP" -g "$RG" --query id -o tsv)
  asignar "Contributor" "$ALCANCE_APP"
done

echo
echo "Listo. En GitHub: Settings > Secrets and variables > Actions > pestaña Variables > New repository variable"
echo "  AZURE_CLIENT_ID        = $CLIENT_ID"
echo "  AZURE_TENANT_ID        = $(az account show --query tenantId -o tsv)"
echo "  AZURE_SUBSCRIPTION_ID  = $(az account show --query id -o tsv)"
echo "Son identificadores, no contraseñas: sin la aprobación del entorno '$ENTORNO_GITHUB' no permiten desplegar."
