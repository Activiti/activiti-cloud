#!/usr/bin/env bash
# Export env for one acceptance-test matrix cell (broker × partitioning × destinations).
#
# Required env:
#   MESSAGING_BROKER, MESSAGING_PARTITIONED, MESSAGING_DESTINATIONS, VERSION
# Optional:
#   GITHUB_PR_NUMBER, GITHUB_RUN_NUMBER, CLUSTER_NAME, CLUSTER_DOMAIN

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
# shellcheck source=../lib/preview-env.sh
source "${ROOT_DIR}/scripts/lib/preview-env.sh"

: "${MESSAGING_BROKER:?MESSAGING_BROKER is required}"
: "${MESSAGING_PARTITIONED:?MESSAGING_PARTITIONED is required}"
: "${MESSAGING_DESTINATIONS:?MESSAGING_DESTINATIONS is required}"
: "${VERSION:?VERSION is required}"

export MESSAGING_BROKER
export MESSAGING_PARTITIONED
export MESSAGING_DESTINATIONS
export VERSION

PREVIEW_NAME="$(preview_name_from_ci_matrix \
  "${GITHUB_PR_NUMBER:-}" \
  "${GITHUB_RUN_NUMBER:-}" \
  "${MESSAGING_BROKER}" \
  "${MESSAGING_PARTITIONED}" \
  "${MESSAGING_DESTINATIONS}")"
export PREVIEW_NAME

export_preview_gateway_env

echo "${VERSION}" > "${ROOT_DIR}/VERSION"

if [[ "${GITHUB_ACTIONS:-}" == "true" ]]; then
  ci_append_github_env MESSAGING_BROKER "${MESSAGING_BROKER}"
  ci_append_github_env MESSAGING_PARTITIONED "${MESSAGING_PARTITIONED}"
  ci_append_github_env MESSAGING_DESTINATIONS "${MESSAGING_DESTINATIONS}"
  ci_append_github_env VERSION "${VERSION}"
  ci_append_github_env PREVIEW_NAME "${PREVIEW_NAME}"
  ci_append_github_env SSO_PROTOCOL "${SSO_PROTOCOL}"
  ci_append_github_env GATEWAY_PROTOCOL "${GATEWAY_PROTOCOL}"
  ci_append_github_env GLOBAL_GATEWAY_DOMAIN "${GLOBAL_GATEWAY_DOMAIN}"
  ci_append_github_env GATEWAY_HOST "${GATEWAY_HOST}"
  ci_append_github_env SSO_HOST "${SSO_HOST}"
fi

echo "MESSAGING_BROKER=${MESSAGING_BROKER}"
echo "MESSAGING_PARTITIONED=${MESSAGING_PARTITIONED}"
echo "MESSAGING_DESTINATIONS=${MESSAGING_DESTINATIONS}"
echo "PREVIEW_NAME=${PREVIEW_NAME}"
echo "VERSION=${VERSION}"
echo "GATEWAY_HOST=${GATEWAY_HOST}"
echo "SSO_HOST=${SSO_HOST}"
