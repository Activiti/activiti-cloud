#!/usr/bin/env bash
# Discover preview namespace and Activiti deployment names (Helm release prefix varies).

preview_name_from_env_name() {
  local env_name=$1
  local broker="${MESSAGING_BROKER:-rabbitmq}"
  local partitioned="${MESSAGING_PARTITIONED:-false}"
  local destinations="${MESSAGING_DESTINATIONS:-default}"

  local partitioned_suffix="n"
  if [[ "${partitioned}" == "true" || "${partitioned}" == "partitioned" ]]; then
    partitioned_suffix="p"
  fi

  local destinations_suffix="d"
  if [[ "${destinations}" == "override" || "${destinations}" == "override-destinations" ]]; then
    destinations_suffix="o"
  fi

  echo "pr-${env_name}-${broker:0:6}-${partitioned_suffix}-${destinations_suffix}"
}

find_deployment_in_namespace() {
  local namespace=$1
  local pattern=$2
  local candidate

  for candidate in "${namespace}-${pattern}" "${pattern}"; do
    if kubectl get deployment "${candidate}" -n "${namespace}" &>/dev/null; then
      echo "${candidate}"
      return 0
    fi
  done

  kubectl get deployment -n "${namespace}" -o jsonpath='{range .items[*]}{.metadata.name}{"\n"}{end}' 2>/dev/null \
    | grep -i "${pattern}" \
    | head -1
}

namespace_has_runtime_bundle() {
  local namespace=$1
  [[ -n "$(find_deployment_in_namespace "${namespace}" "runtime-bundle")" ]]
}

discover_preview_namespace() {
  local preferred="${1:-}"

  if [[ -n "${preferred}" ]] && kubectl get namespace "${preferred}" &>/dev/null; then
    if namespace_has_runtime_bundle "${preferred}"; then
      echo "${preferred}"
      return 0
    fi
  fi

  if [[ -n "${ACCEPTANCE_ENV_NAME:-}" ]]; then
    local generated
    generated="$(preview_name_from_env_name "${ACCEPTANCE_ENV_NAME}")"
    if kubectl get namespace "${generated}" &>/dev/null && namespace_has_runtime_bundle "${generated}"; then
      echo "${generated}"
      return 0
    fi
  fi

  local match
  match="$(kubectl get ns -o jsonpath='{range .items[*]}{.metadata.name}{"\n"}{end}' 2>/dev/null \
    | grep -E '^pr-.*-rabbit-n-d$' || true)"

  while IFS= read -r ns; do
    [[ -z "${ns}" ]] && continue
    if namespace_has_runtime_bundle "${ns}"; then
      echo "${ns}"
      return 0
    fi
  done <<< "${match}"

  if [[ -n "${preferred}" ]] && kubectl get namespace "${preferred}" &>/dev/null; then
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
  QUERY_DEP="$(find_deployment_in_namespace "${namespace}" "activiti-cloud-query")"
  AUDIT_DEP="$(find_deployment_in_namespace "${namespace}" "activiti-cloud-audit")"
  CONNECTOR_DEP="$(find_deployment_in_namespace "${namespace}" "activiti-cloud-connector")"
  IDENTITY_DEP="$(find_deployment_in_namespace "${namespace}" "activiti-cloud-identity-adapter")"

  export RB_DEP QUERY_DEP AUDIT_DEP CONNECTOR_DEP IDENTITY_DEP

  [[ -n "${RB_DEP}" && -n "${QUERY_DEP}" && -n "${CONNECTOR_DEP}" ]]
}
