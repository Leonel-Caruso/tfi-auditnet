#!/usr/bin/env sh
set -eu

if [ -z "${JWT_SECRET:-}" ]; then
  echo "JWT_SECRET no esta configurado." >&2
  exit 1
fi

# La misma clave HS256 se representa como JWK Base64URL
# para verificar los tokens emitidos por management-service.
SECRET_B64URL="$(printf '%s' "$JWT_SECRET" | base64 | tr '+/' '-_' | tr -d '=\r\n')"

JWK_JSON="{\"kty\":\"oct\",\"k\":\"${SECRET_B64URL}\",\"alg\":\"HS256\"}"

JWT_VERIFY_JWK="$(printf '%s' "$JWK_JSON" | base64 | tr '+/' '-_' | tr -d '=\r\n')"

# Inyeccion directa de la propiedad smallrye.jwt.verify.secretkey.
export SMALLRYE_JWT_VERIFY_SECRETKEY="$JWT_VERIFY_JWK"

exec java -jar quarkus-run.jar
