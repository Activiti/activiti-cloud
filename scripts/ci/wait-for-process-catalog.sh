#!/usr/bin/env bash
# Wait until runtime-bundle exposes BPMN keys required by Playwright (per-key probe).
# Run after prepare-preview-for-playwright and export-playwright-ci-env.sh.

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "${ROOT_DIR}"

if [[ -z "${GATEWAY_HOST:-}" || -z "${KEYCLOAK_CLIENT_SECRET:-}" ]]; then
  echo "::error::GATEWAY_HOST and KEYCLOAK_CLIENT_SECRET must be set (run export-playwright-ci-env.sh first)"
  exit 1
fi

npm ci
npm run verify:process-catalog

if [[ "${GITHUB_ACTIONS:-}" == "true" && -n "${GITHUB_ENV:-}" ]]; then
  echo "ACCEPTANCE_PROCESS_CATALOG_VERIFIED=true" >> "${GITHUB_ENV}"
fi

echo "✓ Runtime process catalog ready for Playwright"
