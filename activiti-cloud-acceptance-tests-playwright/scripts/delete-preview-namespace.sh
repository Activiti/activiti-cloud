#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
ENV_FILE="${ROOT_DIR}/activiti-cloud-acceptance-tests-playwright/.env"

if [[ -f "${ENV_FILE}" ]]; then
  # shellcheck disable=SC1090
  set -a
  source "${ENV_FILE}"
  set +a
fi

if [[ -z "${PREVIEW_NAME:-}" ]]; then
  echo "PREVIEW_NAME is not set. Run npm run test:setup -- --install first, or export PREVIEW_NAME." >&2
  exit 1
fi

echo "Deleting preview: PREVIEW_NAME=${PREVIEW_NAME}"
cd "${ROOT_DIR}"
PREVIEW_NAME="${PREVIEW_NAME}" make delete
