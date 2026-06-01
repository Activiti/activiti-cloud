#!/usr/bin/env bash
# Wait for workloads, apply acceptance overlay, load Keycloak secret, smoke auth.
# Playwright global-setup skips overlay when ACCEPTANCE_CI_OVERLAY_APPLIED=true.

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
# shellcheck source=../lib/keycloak-preview.sh
source "${ROOT_DIR}/scripts/lib/keycloak-preview.sh"
# shellcheck source=../lib/preview-env.sh
source "${ROOT_DIR}/scripts/lib/preview-env.sh"

: "${PREVIEW_NAME:?PREVIEW_NAME is required}"

export CI=true
export GITHUB_ACTIONS=true
export ACCEPTANCE_ROLLOUT_TIMEOUT_SEC="${ACCEPTANCE_ROLLOUT_TIMEOUT_SEC:-300}"
export ACCEPTANCE_STATEFULSET_ROLLOUT_TIMEOUT_SEC="${ACCEPTANCE_STATEFULSET_ROLLOUT_TIMEOUT_SEC:-420}"

# Images: either chart defaults (fast path) or VERSION from build job (Serenity / PR flow).
if [[ "${ACCEPTANCE_CI_USE_CHART_IMAGE_TAGS:-}" == "true" ]]; then
  export ACCEPTANCE_SKIP_IMAGE_RESOLVE=true
elif [[ -n "${VERSION:-}" ]]; then
  export ACCEPTANCE_SKIP_IMAGE_RESOLVE=true
  export ACCEPTANCE_RUNTIME_BUNDLE_IMAGE="activiti/example-runtime-bundle:${VERSION}"
fi

export_preview_gateway_env

echo "Waiting for Activiti workloads in ${PREVIEW_NAME}..."
# shellcheck source=../../activiti-cloud-acceptance-tests-playwright/scripts/lib/cluster-discovery.sh
source "${ROOT_DIR}/activiti-cloud-acceptance-tests-playwright/scripts/lib/cluster-discovery.sh"
wait_for_acceptance_deployments "${PREVIEW_NAME}"

echo "Applying acceptance overlay (security policies + hostAliases; RB image only if drift from ${ACCEPTANCE_RUNTIME_BUNDLE_IMAGE:-helm deploy})..."
bash "${ROOT_DIR}/activiti-cloud-acceptance-tests-playwright/scripts/apply-cluster-prereqs.sh" "${PREVIEW_NAME}"

echo "Loading activiti client secret from ${PREVIEW_NAME}/activiti-keycloak-client..."
KEYCLOAK_CLIENT_SECRET="$(wait_for_preview_keycloak_client_secret "${PREVIEW_NAME}")"
echo "::add-mask::${KEYCLOAK_CLIENT_SECRET}"
export KEYCLOAK_CLIENT_SECRET
if [[ "${GITHUB_ACTIONS:-}" == "true" ]]; then
  echo "KEYCLOAK_CLIENT_SECRET=${KEYCLOAK_CLIENT_SECRET}" >> "${GITHUB_ENV}"
fi

bash "${ROOT_DIR}/scripts/ci/smoke-preview-auth.sh"

if [[ "${GITHUB_ACTIONS:-}" == "true" ]]; then
  echo "ACCEPTANCE_CI_OVERLAY_APPLIED=true" >> "${GITHUB_ENV}"
fi
echo "✓ Preview ${PREVIEW_NAME} ready for Playwright"
