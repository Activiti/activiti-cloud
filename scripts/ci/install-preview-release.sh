#!/usr/bin/env bash
# Helm install for one preview namespace (matrix cell). Images come from chart values only.
#
# Requires: PREVIEW_NAME, MESSAGING_BROKER, MESSAGING_PARTITIONED, MESSAGING_DESTINATIONS
# Optional: ACCEPTANCE_CI_USE_CHART_IMAGE_TAGS=true (default in CI — no update-chart / no PR-SNAPSHOT)

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
# shellcheck source=../lib/preview-env.sh
source "${ROOT_DIR}/scripts/lib/preview-env.sh"

: "${PREVIEW_NAME:?PREVIEW_NAME is required}"
: "${MESSAGING_BROKER:?MESSAGING_BROKER is required}"
: "${MESSAGING_PARTITIONED:?MESSAGING_PARTITIONED is required}"
: "${MESSAGING_DESTINATIONS:?MESSAGING_DESTINATIONS is required}"

export_preview_gateway_env
export ACCEPTANCE_CI_USE_CHART_IMAGE_TAGS="${ACCEPTANCE_CI_USE_CHART_IMAGE_TAGS:-true}"

cd "${ROOT_DIR}"
echo "Installing Helm release ${PREVIEW_NAME} (broker=${MESSAGING_BROKER}, partitioning=${MESSAGING_PARTITIONED}, destinations=${MESSAGING_DESTINATIONS})"
echo "Image tags: chart defaults (ACCEPTANCE_CI_USE_CHART_IMAGE_TAGS=${ACCEPTANCE_CI_USE_CHART_IMAGE_TAGS})"
make install
