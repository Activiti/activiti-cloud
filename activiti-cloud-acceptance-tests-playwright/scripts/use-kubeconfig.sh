#!/usr/bin/env bash
# Point kubectl at your cluster kubeconfig (activiti, develop, etc.).
#
# Usage:
#   export ACTIVITI_KUBECONFIG=~/Downloads/activiti.yaml
#   source activiti-cloud-acceptance-tests-playwright/scripts/use-kubeconfig.sh
#
# Or:
#   source .../use-kubeconfig.sh ~/Downloads/activiti.yaml

KUBECONFIG_CANDIDATE="${1:-${ACTIVITI_KUBECONFIG:-${KUBECONFIG:-$HOME/Downloads/activiti.yaml}}}"

if [[ ! -f "${KUBECONFIG_CANDIDATE}" ]]; then
  echo "❌ Kubeconfig not found: ${KUBECONFIG_CANDIDATE}" >&2
  echo "   export ACTIVITI_KUBECONFIG=/path/to/activiti.yaml" >&2
  return 1 2>/dev/null || exit 1
fi

export KUBECONFIG="${KUBECONFIG_CANDIDATE}"
export ACTIVITI_KUBECONFIG="${KUBECONFIG_CANDIDATE}"
echo "✓ KUBECONFIG=${KUBECONFIG}"
echo "  context: $(kubectl config current-context 2>/dev/null || echo 'unknown')"
