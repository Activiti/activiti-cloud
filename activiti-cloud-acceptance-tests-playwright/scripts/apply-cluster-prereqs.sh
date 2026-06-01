#!/usr/bin/env bash
# Applies cluster settings required for Playwright acceptance parity.
# Only patches or restarts workloads when something is missing or out of date.
#
# Usage (from repo root):
#   npm run cluster:prereqs
#   # or: bash activiti-cloud-acceptance-tests-playwright/scripts/apply-cluster-prereqs.sh [namespace]
#
# Implementation split across scripts/lib/prereqs-*.sh

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PKG_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
ROOT_DIR="$(cd "${PKG_DIR}/.." && pwd)"
export SCRIPT_DIR PKG_DIR ROOT_DIR

REQUESTED_NAMESPACE="${1:-${PREVIEW_NAME:-}}"
PREREQS_CHANGED=0

prereqs_load_lib() {
  local lib_path=$1
  local lib_name
  lib_name="$(basename "${lib_path}")"
  if declare -f prereqs_step &>/dev/null; then
    prereqs_set_actor coordinator
    prereqs_step "load lib/${lib_name}"
  else
    echo "[apply-cluster-prereqs] load lib/${lib_name}"
  fi
  # shellcheck disable=SC1090
  source "${lib_path}"
}

prereqs_invoke() {
  local label=$1
  shift
  prereqs_set_actor coordinator
  prereqs_step "${label}"
  "$@"
}

prereqs_load_lib "${SCRIPT_DIR}/lib/cluster-discovery.sh"
prereqs_load_lib "${SCRIPT_DIR}/lib/prereqs-progress.sh"
prereqs_load_lib "${SCRIPT_DIR}/lib/prereqs-workload.sh"
prereqs_load_lib "${SCRIPT_DIR}/lib/prereqs-init.sh"
prereqs_load_lib "${SCRIPT_DIR}/lib/prereqs-runtime-bundle.sh"
prereqs_load_lib "${SCRIPT_DIR}/lib/prereqs-configmaps.sh"
prereqs_load_lib "${SCRIPT_DIR}/lib/prereqs-deployment-patch.sh"
prereqs_load_lib "${SCRIPT_DIR}/lib/prereqs-keycloak-hradmin.sh"

prereqs_invoke "bootstrap namespace and deployments" \
  prereqs_bootstrap "${REQUESTED_NAMESPACE}" "${PKG_DIR}" || exit 1
prereqs_invoke "resolve runtime bundle image" prereqs_apply_runtime_bundle_image
prereqs_invoke "setup paths and supplemental flags" prereqs_setup_paths_and_flags "${PKG_DIR}"
prereqs_invoke "log cluster configuration" prereqs_log_configuration
prereqs_invoke "rollout runtime bundle image" \
  prereqs_rollout_runtime_bundle_image || exit 1
prereqs_invoke "verify connector workload" \
  prereqs_verify_connector_present || exit 1
prereqs_invoke "apply acceptance ConfigMaps" prereqs_apply_configmaps
prereqs_invoke "patch workloads (hostAliases, Keycloak, policies)" \
  prereqs_patch_workloads || exit 1
prereqs_invoke "adjust Keycloak hradmin roles" prereqs_fix_hradmin_roles
prereqs_invoke "finish" prereqs_finish
