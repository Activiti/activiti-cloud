#!/usr/bin/env bash
# Start ingress port-forward for local Playwright / API tests.
# Loads .env from this module directory.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENV_FILE="${SCRIPT_DIR}/../.env"

if [[ -f "$ENV_FILE" ]]; then
  set -a
  # shellcheck disable=SC1090
  source "$ENV_FILE"
  set +a
fi

LOCAL_PORT="${LOCAL_PORT:-8080}"
NAMESPACE="${PORT_FORWARD_NAMESPACE:-default}"
SERVICE="${PORT_FORWARD_SERVICE:-ingress-nginx-controller}"

echo "Port-forward: svc/${SERVICE} ${LOCAL_PORT}:80 -n ${NAMESPACE}"

if lsof -i ":${LOCAL_PORT}" -sTCP:LISTEN -t >/dev/null 2>&1; then
  echo "✓ Port ${LOCAL_PORT} is already in use (likely an existing kubectl port-forward)."
  echo "  If tests fail, stop it with: kill \$(lsof -i :${LOCAL_PORT} -sTCP:LISTEN -t)"
  exit 0
fi

exec kubectl port-forward "svc/${SERVICE}" "${LOCAL_PORT}:80" -n "${NAMESPACE}"
