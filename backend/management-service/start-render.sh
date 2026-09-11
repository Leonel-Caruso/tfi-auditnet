#!/usr/bin/env sh
set -eu

# Render entrega DATABASE_URL como postgresql://...
# Quarkus/JDBC necesita jdbc:postgresql://...
if [ -z "${DB_JDBC_URL:-}" ] && [ -n "${DATABASE_URL:-}" ]; then
  export DB_JDBC_URL="jdbc:${DATABASE_URL}"
fi

exec java -jar quarkus-run.jar
