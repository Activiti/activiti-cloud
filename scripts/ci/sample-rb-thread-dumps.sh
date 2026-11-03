#!/usr/bin/env bash
# Periodically trigger a JVM thread dump (SIGQUIT -> stdout) on the runtime-bundle
# pods while the Playwright suite runs, so a hanging request (e.g. a stuck
# POST /assign) is captured in the pod logs for post-mortem analysis.
# Intended to run in the background; stopped via the stop-file or by killing the PID.

set -uo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
# shellcheck source=../../activiti-cloud-acceptance-tests-playwright/scripts/lib/cluster-discovery.sh
source "${ROOT_DIR}/activiti-cloud-acceptance-tests-playwright/scripts/lib/cluster-discovery.sh"

: "${PREVIEW_NAME:?PREVIEW_NAME is required}"

INTERVAL_SECONDS="${RB_DUMP_INTERVAL_SECONDS:-5}"
MAX_SECONDS="${RB_DUMP_MAX_SECONDS:-2400}"
STOP_FILE="${RB_DUMP_STOP_FILE:-${ROOT_DIR}/.rb-dump-stop}"

rm -f "${STOP_FILE}"

RB_DEP="$(find_workload_in_namespace "${PREVIEW_NAME}" "runtime-bundle")"
[[ -z "${RB_DEP}" ]] && RB_DEP="$(find_workload_in_namespace "${PREVIEW_NAME}" "rb")"

if [[ -z "${RB_DEP}" ]]; then
  echo "sample-rb-thread-dumps: runtime-bundle workload not found in ${PREVIEW_NAME}; nothing to sample" >&2
  exit 0
fi

echo "sample-rb-thread-dumps: sampling ${RB_DEP} every ${INTERVAL_SECONDS}s (max ${MAX_SECONDS}s)"

elapsed=0
while [[ ! -f "${STOP_FILE}" && "${elapsed}" -lt "${MAX_SECONDS}" ]]; do
  while IFS= read -r pod; do
    [[ -z "${pod}" ]] && continue
    kubectl exec -n "${PREVIEW_NAME}" "${pod}" -- sh -c 'kill -3 1' 2>/dev/null || true
  done < <(kubectl get pods -n "${PREVIEW_NAME}" -o name 2>/dev/null | grep -F "${RB_DEP}" || true)

  sleep "${INTERVAL_SECONDS}"
  elapsed=$((elapsed + INTERVAL_SECONDS))
done

echo "sample-rb-thread-dumps: stopped after ${elapsed}s"
