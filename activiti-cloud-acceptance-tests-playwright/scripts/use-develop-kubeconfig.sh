#!/usr/bin/env bash
# Point kubectl at the develop cluster (Rancher).
# Usage: source activiti-cloud-acceptance-tests-playwright/scripts/use-develop-kubeconfig.sh
#
# Set DEVELOP_KUBECONFIG to your kubeconfig file path (default: ~/Downloads/develop.yaml).
# Do NOT commit kubeconfig files with tokens to git.

DEVELOP_KUBECONFIG="${DEVELOP_KUBECONFIG:-$HOME/Downloads/develop.yaml}"

if [[ ! -f "$DEVELOP_KUBECONFIG" ]]; then
  echo "❌ Kubeconfig not found: $DEVELOP_KUBECONFIG" >&2
  echo "   Export DEVELOP_KUBECONFIG=/path/to/develop.yaml" >&2
  return 1 2>/dev/null || exit 1
fi

export KUBECONFIG="$DEVELOP_KUBECONFIG"
echo "✓ KUBECONFIG=$KUBECONFIG"
echo "  context: $(kubectl config current-context 2>/dev/null || echo 'unknown')"
