#!/usr/bin/env bash
# Collect runtime-bundle pod logs from the preview namespace for post-mortem diagnosis.
# Runs on Playwright completion, before the preview namespace is torn down by `make delete`.
# Captures full RB logs (so SIGQUIT thread dumps emitted by sample-rb-thread-dumps.sh are
# included) and isolates any thread stanza touching the task assign/reassign/release path.

set -uo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
# shellcheck source=../../activiti-cloud-acceptance-tests-playwright/scripts/lib/cluster-discovery.sh
source "${ROOT_DIR}/activiti-cloud-acceptance-tests-playwright/scripts/lib/cluster-discovery.sh"

: "${PREVIEW_NAME:?PREVIEW_NAME is required}"

OUT_DIR="${1:-${ROOT_DIR}/activiti-cloud-acceptance-tests-playwright/rb-logs}"
mkdir -p "${OUT_DIR}"

SUSPECT_PATTERN='TaskRuntimeImpl|assertCanReleaseTask|releaseTask|reassignTask|TaskControllerImpl|CandidateGroup|KeycloakUserGroupManager|RuntimeBundleExceptionHandler|AmqpTemplate|RabbitTemplate|IntegrationContext|FeignException|SocketTimeout|ReadTimeout|deadlock|waiting to lock'

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
  suspect_log="${OUT_DIR}/${safe_name}.suspect-threads.txt"

  kubectl logs -n "${PREVIEW_NAME}" "${pod}" --all-containers --prefix \
    > "${full_log}" 2>&1 || true
  kubectl logs -n "${PREVIEW_NAME}" "${pod}" --all-containers --prefix --previous \
    >> "${full_log}" 2>/dev/null || true

  {
    echo "# Thread stanzas touching the assign/reassign/release path for ${safe_name}"
    echo "# (SIGQUIT thread dumps from sample-rb-thread-dumps.sh land in ${safe_name}.full.log)"
    echo "# An empty list below means no sampled dump caught a thread on that path."
    echo "# ---"
  } > "${suspect_log}"
  awk -v RS='' -v ORS='\n\n' "/${SUSPECT_PATTERN}/" "${full_log}" >> "${suspect_log}" 2>/dev/null || true
done

echo "✓ Runtime bundle logs collected into ${OUT_DIR}"
