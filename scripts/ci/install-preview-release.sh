#!/usr/bin/env bash
# Helm install for one preview namespace (matrix cell).
#
# Requires: PREVIEW_NAME, VERSION, MESSAGING_BROKER, MESSAGING_PARTITIONED, MESSAGING_DESTINATIONS
# Uses make update-chart + install (PR-SNAPSHOT from build job) unless ACCEPTANCE_CI_USE_CHART_IMAGE_TAGS=true.

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
# shellcheck source=../lib/preview-env.sh
source "${ROOT_DIR}/scripts/lib/preview-env.sh"

: "${PREVIEW_NAME:?PREVIEW_NAME is required}"
: "${VERSION:?VERSION is required — pass needs.build.outputs.version from setup-matrix-env}"
: "${MESSAGING_BROKER:?MESSAGING_BROKER is required}"
: "${MESSAGING_PARTITIONED:?MESSAGING_PARTITIONED is required}"
: "${MESSAGING_DESTINATIONS:?MESSAGING_DESTINATIONS is required}"

export_preview_gateway_env

cd "${ROOT_DIR}"
echo "Installing Helm release ${PREVIEW_NAME} (broker=${MESSAGING_BROKER}, partitioning=${MESSAGING_PARTITIONED}, destinations=${MESSAGING_DESTINATIONS})"
if [[ "${ACCEPTANCE_CI_USE_CHART_IMAGE_TAGS:-}" == "true" ]]; then
  echo "Image tags: chart defaults (ACCEPTANCE_CI_USE_CHART_IMAGE_TAGS=true)"
else
  echo "Image tags: built PR-SNAPSHOT ${VERSION} (make update-chart → helm install)"
fi
make install
