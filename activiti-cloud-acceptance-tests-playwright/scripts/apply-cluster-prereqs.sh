#!/usr/bin/env bash
# Applies cluster settings required for full Playwright / Serenity acceptance parity.
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

# shellcheck source=lib/cluster-discovery.sh
source "${SCRIPT_DIR}/lib/cluster-discovery.sh"
# shellcheck source=lib/prereqs-progress.sh
source "${SCRIPT_DIR}/lib/prereqs-progress.sh"
# shellcheck source=lib/prereqs-workload.sh
source "${SCRIPT_DIR}/lib/prereqs-workload.sh"
# shellcheck source=lib/prereqs-init.sh
source "${SCRIPT_DIR}/lib/prereqs-init.sh"
# shellcheck source=lib/prereqs-runtime-bundle.sh
source "${SCRIPT_DIR}/lib/prereqs-runtime-bundle.sh"
# shellcheck source=lib/prereqs-configmaps.sh
source "${SCRIPT_DIR}/lib/prereqs-configmaps.sh"
# shellcheck source=lib/prereqs-deployment-patch.sh
source "${SCRIPT_DIR}/lib/prereqs-deployment-patch.sh"
# shellcheck source=lib/prereqs-keycloak-hradmin.sh
source "${SCRIPT_DIR}/lib/prereqs-keycloak-hradmin.sh"

prereqs_bootstrap "${REQUESTED_NAMESPACE}" "${PKG_DIR}" || exit 1
prereqs_apply_runtime_bundle_image
prereqs_setup_paths_and_flags "${PKG_DIR}"
prereqs_log_configuration
prereqs_rollout_runtime_bundle_image || exit 1
prereqs_verify_connector_present || exit 1
prereqs_apply_configmaps
prereqs_patch_workloads || exit 1
prereqs_fix_hradmin_roles
prereqs_finish
