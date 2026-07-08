#!/usr/bin/env bash
# Point kubectl at your cluster kubeconfig (activiti, develop, etc.).
#
# Usage:
#   export ACTIVITI_KUBECONFIG=~/Downloads/activiti.yaml
#   source activiti-cloud-acceptance-tests-playwright/scripts/use-kubeconfig.sh
#
# Or:
#   source .../use-kubeconfig.sh ~/Downloads/activiti.yaml
#
# Picks the first kubeconfig that can reach the cluster (cluster-info).
# A stale ~/Downloads/activiti.yaml is skipped when ~/.kube/config works.

kubeconfig_connects() {
  KUBECONFIG="$1" kubectl cluster-info &>/dev/null
}

collect_kubeconfig_candidates() {
  local explicit="${1:-}"
  local -a candidates=()

  if [[ -n "${explicit}" ]]; then
    candidates+=("${explicit}")
  else
    [[ -n "${ACTIVITI_KUBECONFIG:-}" ]] && candidates+=("${ACTIVITI_KUBECONFIG}")
    [[ -n "${KUBECONFIG:-}" ]] && candidates+=("${KUBECONFIG}")
    candidates+=("${HOME}/Downloads/activiti.yaml")
    candidates+=("${HOME}/.kube/config")
  fi

  local c seen=""
  for c in "${candidates[@]}"; do
    [[ -z "${c}" ]] && continue
    case ",${seen}," in
      *",${c},"*) continue ;;
    esac
    seen="${seen},${c}"
    echo "${c}"
  done
}

resolve_working_kubeconfig() {
  local explicit="${1:-}"
  local candidate chosen="" skipped=""

  while IFS= read -r candidate; do
    [[ -z "${candidate}" ]] && continue
    if [[ ! -f "${candidate}" ]]; then
      continue
    fi
    if kubeconfig_connects "${candidate}"; then
      chosen="${candidate}"
      break
    fi
    skipped="${skipped} ${candidate}"
  done < <(collect_kubeconfig_candidates "${explicit}")

  if [[ -n "${chosen}" ]]; then
  if [[ -n "${skipped}" ]]; then
      echo "⚠ Skipping kubeconfig(s) that cannot reach the cluster:${skipped}" >&2
      echo "  Using: ${chosen}" >&2
    fi
    echo "${chosen}"
    return 0
  fi

  # Explicit path requested but none connect — surface the first existing file for diagnostics.
  while IFS= read -r candidate; do
    [[ -z "${candidate}" ]] && continue
    if [[ -f "${candidate}" ]]; then
      echo "${candidate}"
      return 1
    fi
  done < <(collect_kubeconfig_candidates "${explicit}")

  return 1
}

EXPLICIT_KUBECONFIG="${1:-}"
if ! KUBECONFIG_CANDIDATE="$(resolve_working_kubeconfig "${EXPLICIT_KUBECONFIG}")"; then
  echo "❌ No working kubeconfig found." >&2
  if [[ -n "${KUBECONFIG_CANDIDATE:-}" ]]; then
    echo "   Tried: ${KUBECONFIG_CANDIDATE} (file exists but kubectl cannot connect)" >&2
  else
    echo "   Set ACTIVITI_KUBECONFIG or run: npm run kube:use" >&2
    echo "   Or refresh: ./scripts/fix-kubectl-config.sh activiti" >&2
  fi
  return 1 2>/dev/null || exit 1
fi

export KUBECONFIG="${KUBECONFIG_CANDIDATE}"
export ACTIVITI_KUBECONFIG="${KUBECONFIG_CANDIDATE}"
echo "✓ KUBECONFIG=${KUBECONFIG}"
echo "  context: $(kubectl config current-context 2>/dev/null || echo 'unknown')"
