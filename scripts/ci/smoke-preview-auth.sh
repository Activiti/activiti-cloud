#!/usr/bin/env bash
# Post-overlay smoke: Keycloak password grant + identity-adapter group search.

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
# shellcheck source=../lib/keycloak-preview.sh
source "${ROOT_DIR}/scripts/lib/keycloak-preview.sh"
# shellcheck source=../lib/preview-env.sh
source "${ROOT_DIR}/scripts/lib/preview-env.sh"
# shellcheck source=../lib/acceptance-test-users.sh
source "${ROOT_DIR}/scripts/lib/acceptance-test-users.sh"

: "${PREVIEW_NAME:?PREVIEW_NAME is required}"
: "${KEYCLOAK_CLIENT_SECRET:?KEYCLOAK_CLIENT_SECRET is required}"

export_preview_gateway_env
export_acceptance_test_user_env

TOKEN_URL="$(preview_sso_token_url "${PREVIEW_NAME}" activiti)"
TOKEN_HTTP="$(curl -sS -o /tmp/acceptance-token-check.json -w '%{http_code}' \
  -X POST "${TOKEN_URL}" \
  -d "grant_type=password" \
  -d "client_id=activiti" \
  -d "client_secret=${KEYCLOAK_CLIENT_SECRET}" \
  -d "username=${TESTUSER_USERNAME}" \
  -d "password=${TESTUSER_PASSWORD}" || echo "000")"

if [[ "${TOKEN_HTTP}" != "200" ]]; then
  echo "::error::Keycloak token check failed (HTTP ${TOKEN_HTTP}) at ${TOKEN_URL}"
  cat /tmp/acceptance-token-check.json 2>/dev/null || true
  exit 1
fi
echo "✓ Keycloak token OK for ${TESTUSER_USERNAME} (client activiti, secret from namespace)"

ACCESS_TOKEN="$(python3 -c "import json; print(json.load(open('/tmp/acceptance-token-check.json'))['access_token'])" 2>/dev/null || true)"
if [[ -n "${ACCESS_TOKEN}" && -n "${GATEWAY_HOST:-}" ]]; then
  IDENTITY_SEARCH_HTTP="$(curl -sS -o /tmp/acceptance-identity-groups.json -w '%{http_code}' \
    --max-time 30 \
    -H "Authorization: Bearer ${ACCESS_TOKEN}" \
    "https://${GATEWAY_HOST}/identity-adapter-service/v1/groups?search=sa" || echo "000")"
  if [[ "${IDENTITY_SEARCH_HTTP}" != "200" ]]; then
    echo "::error::identity-adapter group search check failed (HTTP ${IDENTITY_SEARCH_HTTP})"
    cat /tmp/acceptance-identity-groups.json 2>/dev/null || true
    exit 1
  fi
  echo "✓ identity-adapter group search OK (testuser bearer)"
fi
