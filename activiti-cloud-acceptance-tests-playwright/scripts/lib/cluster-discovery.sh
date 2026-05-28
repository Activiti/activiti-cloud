#!/usr/bin/env bash
# Discover preview namespace and Activiti deployment names (Helm release prefix varies).

_LIB_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
_ROOT_DIR="$(cd "${_LIB_DIR}/../../.." && pwd)"
# shellcheck source=../../../scripts/lib/k8s-deployments.sh
source "${_ROOT_DIR}/scripts/lib/k8s-deployments.sh"

preview_name_from_env_name() {
  local env_name=$1
  local broker="${MESSAGING_BROKER:-rabbitmq}"
  local partitioned="${MESSAGING_PARTITIONED:-non-partitioned}"
  local destinations="${MESSAGING_DESTINATIONS:-default}"

  local partitioned_suffix="n"
  if [[ "${partitioned}" == "true" || "${partitioned}" == "partitioned" ]]; then
    partitioned_suffix="p"
  fi

  local destinations_suffix="d"
  if [[ "${destinations}" == "override" || "${destinations}" == "override-destinations" ]]; then
    destinations_suffix="o"
  elif [[ "${destinations}" == "pdb" ]]; then
    destinations_suffix="p"
  fi

  if [[ "${partitioned}" == "prefix" ]]; then
    partitioned_suffix="p"
  fi

  echo "pr-${env_name}-${broker:0:6}-${partitioned_suffix}-${destinations_suffix}"
}

# Short random suffix for unique namespaces on shared clusters (e.g. a3f2b1).
random_env_suffix() {
  if command -v openssl &>/dev/null; then
    openssl rand -hex 3
  else
    printf '%06x' $((RANDOM * 65536 + RANDOM))
  fi
}

# Default local env name: short user + random (fits K8s 63-char deployment limit).
default_acceptance_env_name() {
  local user="${USER:-${USERNAME:-dev}}"
  user="$(echo "${user}" | tr '[:upper:]' '[:lower:]' | tr -cd '[:alnum:]-')"
  [[ -z "${user}" ]] && user="dev"
  user="${user:0:6}"
  echo "${user}-$(random_env_suffix)"
}

# Skip shared/legacy names so an old .env does not keep pr-activiti-tests-rabbit-n-d.
is_legacy_acceptance_env_name() {
  local name=$1
  [[ "${name}" == "activiti-tests" || "${name}" == "your-user-local" ]] && return 0
  [[ "${name}" =~ -local$ ]] && return 0
  return 1
}

read_acceptance_env_name_from_dotenv() {
  local env_file=$1
  local existing
  if [[ ! -f "${env_file}" ]]; then
    return 0
  fi
  existing="$(grep -E '^ACCEPTANCE_ENV_NAME=' "${env_file}" 2>/dev/null | head -1 | cut -d= -f2- | tr -d '"' | tr -d "'" || true)"
  if [[ -z "${existing}" ]] || is_legacy_acceptance_env_name "${existing}"; then
    return 0
  fi
  echo "${existing}"
}

namespace_has_runtime_bundle() {
  local namespace=$1
  [[ -n "$(find_deployment_in_namespace "${namespace}" "runtime-bundle")" ]]
}

discover_preview_namespace() {
  local preferred="${1:-}"

  # Never scan the cluster for "any" pr-* namespace — that steals CI previews (e.g. pr-2303-rabbit-n-d).
  if [[ -n "${preferred}" ]] && kubectl get namespace "${preferred}" &>/dev/null; then
    if namespace_has_runtime_bundle "${preferred}"; then
      echo "${preferred}"
      return 0
    fi
    echo "${preferred}"
    return 0
  fi

  if [[ -n "${ACCEPTANCE_ENV_NAME:-}" ]]; then
    echo "$(preview_name_from_env_name "${ACCEPTANCE_ENV_NAME}")"
    return 0
  fi

  return 1
}

discover_acceptance_deployments() {
  local namespace=$1

  RB_DEP="$(find_deployment_in_namespace "${namespace}" "runtime-bundle")"
  [[ -z "${RB_DEP}" ]] && RB_DEP="$(find_deployment_in_namespace "${namespace}" "rb")"

  QUERY_DEP="$(find_deployment_in_namespace "${namespace}" "activiti-cloud-query")"
  [[ -z "${QUERY_DEP}" ]] && QUERY_DEP="$(find_deployment_in_namespace "${namespace}" "query")"

  AUDIT_DEP="$(find_deployment_in_namespace "${namespace}" "activiti-cloud-audit")"
  CONNECTOR_DEP="$(find_deployment_in_namespace "${namespace}" "activiti-cloud-connector")"
  IDENTITY_DEP="$(find_deployment_in_namespace "${namespace}" "activiti-cloud-identity-adapter")"

  export RB_DEP QUERY_DEP AUDIT_DEP CONNECTOR_DEP IDENTITY_DEP

  [[ -n "${RB_DEP}" && -n "${QUERY_DEP}" && -n "${CONNECTOR_DEP}" ]]
}

# CI: Helm --wait can finish before Deployment objects are visible to kubectl.
wait_for_acceptance_deployments() {
  local namespace=$1
  local attempt

  for attempt in $(seq 1 60); do
    if discover_acceptance_deployments "${namespace}"; then
      return 0
    fi
    echo "  Waiting for runtime-bundle, query, and connector in ${namespace} (${attempt}/60)..."
    sleep 10
  done

  echo "ERROR: Activiti workloads not found in ${namespace} after 10 minutes" >&2
  kubectl get deployments -n "${namespace}" 2>/dev/null || true
  return 1
}
