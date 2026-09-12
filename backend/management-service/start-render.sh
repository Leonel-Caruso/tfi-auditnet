#!/usr/bin/env sh
set -eu

if [ -z "${JWT_SECRET:-}" ]; then
  echo "JWT_SECRET no esta configurado." >&2
  exit 1
fi

# SmallRye JWT verifica HS256 usando la misma representacion JWK Base64URL
# que utilizamos en el entorno local.
SECRET_B64URL="$(printf '%s' "$JWT_SECRET" | base64 | tr '+/' '-_' | tr -d '=\r\n')"

JWK_JSON="{\"kty\":\"oct\",\"k\":\"${SECRET_B64URL}\",\"alg\":\"HS256\"}"

JWT_VERIFY_JWK="$(printf '%s' "$JWK_JSON" | base64 | tr '+/' '-_' | tr -d '=\r\n')"
export JWT_VERIFY_JWK

exec java -jar quarkus-run.jar
