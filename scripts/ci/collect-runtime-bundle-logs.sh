#!/usr/bin/env bash
# Collect runtime-bundle pod logs from the preview namespace for post-mortem diagnosis.
# Runs on Playwright failure, before the preview namespace is torn down by `make delete`.
# Output is sanitized: an extract limited to identity/exception loggers plus a bounded
# tail for stacktrace context. No attempt is made to print secrets; avoid widening scope.

set -uo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
# shellcheck source=../../activiti-cloud-acceptance-tests-playwright/scripts/lib/cluster-discovery.sh
source "${ROOT_DIR}/activiti-cloud-acceptance-tests-playwright/scripts/lib/cluster-discovery.sh"

: "${PREVIEW_NAME:?PREVIEW_NAME is required}"

OUT_DIR="${1:-${ROOT_DIR}/activiti-cloud-acceptance-tests-playwright/rb-logs}"
mkdir -p "${OUT_DIR}"

EXTRACT_PATTERN='KeycloakUserGroupManager|RuntimeBundleExceptionHandler|UserGroupManager|identity\.keycloak|SocketTimeout|ReadTimeout|feign\.|FeignException|Retryable'
TAIL_LINES="${RB_LOG_TAIL_LINES:-3000}"

echo "Resolving runtime-bundle workload in ${PREVIEW_NAME}..."
RB_DEP="$(find_workload_in_namespace "${PREVIEW_NAME}" "runtime-bundle")"
[[ -z "${RB_DEP}" ]] && RB_DEP="$(find_workload_in_namespace "${PREVIEW_NAME}" "rb")"

{
  echo "# Runtime bundle log capture"
  echo "# namespace: ${PREVIEW_NAME}"
  echo "# workload:  ${RB_DEP:-<not found>}"
  echo "# captured:  $(date -u +'%Y-%m-%dT%H:%M:%SZ')"
} > "${OUT_DIR}/README.txt"

kubectl get pods -n "${PREVIEW_NAME}" -o wide > "${OUT_DIR}/pods.txt" 2>&1 || true

if [[ -z "${RB_DEP}" ]]; then
  echo "ERROR: runtime-bundle workload not found in ${PREVIEW_NAME}; see pods.txt" >&2
  exit 0
fi

mapfile -t RB_PODS < <(kubectl get pods -n "${PREVIEW_NAME}" -o name 2>/dev/null | grep -F "${RB_DEP}" || true)

if [[ ${#RB_PODS[@]} -eq 0 ]]; then
  echo "No pods matched workload ${RB_DEP}; see pods.txt" >&2
  exit 0
fi

for pod in "${RB_PODS[@]}"; do
  safe_name="${pod#pod/}"
  full_log="${OUT_DIR}/${safe_name}.full.log"
  extract_log="${OUT_DIR}/${safe_name}.identity-and-errors.log"

  kubectl logs -n "${PREVIEW_NAME}" "${pod}" --all-containers --prefix --tail="${TAIL_LINES}" \
    > "${full_log}" 2>&1 || true
  kubectl logs -n "${PREVIEW_NAME}" "${pod}" --all-containers --prefix --previous --tail="${TAIL_LINES}" \
    >> "${full_log}" 2>/dev/null || true

  {
    echo "# Extract for ${safe_name} (patterns: ${EXTRACT_PATTERN})"
    echo "# An empty extract below means KeycloakUserGroupManager never logged for the request —"
    echo "# consistent with the identity lookup hanging before any branch was reached."
    echo "# ---"
  } > "${extract_log}"
  grep -E "${EXTRACT_PATTERN}" "${full_log}" >> "${extract_log}" 2>/dev/null || true
done

echo "✓ Runtime bundle logs collected into ${OUT_DIR}"
