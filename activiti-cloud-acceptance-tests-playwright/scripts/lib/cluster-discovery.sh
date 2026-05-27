#!/usr/bin/env bash
# Discover preview namespace and Activiti deployment names (Helm release prefix varies).

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

# Default local env name: per-user + random (e.g. jane-a3f2b1 → pr-jane-a3f2b1-rabbit-n-d).
default_acceptance_env_name() {
  local user="${USER:-${USERNAME:-dev}}"
  user="$(echo "${user}" | tr '[:upper:]' '[:lower:]' | tr -cd '[:alnum:]-')"
  [[ -z "${user}" ]] && user="dev"
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
  QUERY_DEP="$(find_deployment_in_namespace "${namespace}" "activiti-cloud-query")"
  AUDIT_DEP="$(find_deployment_in_namespace "${namespace}" "activiti-cloud-audit")"
  CONNECTOR_DEP="$(find_deployment_in_namespace "${namespace}" "activiti-cloud-connector")"
  IDENTITY_DEP="$(find_deployment_in_namespace "${namespace}" "activiti-cloud-identity-adapter")"

  export RB_DEP QUERY_DEP AUDIT_DEP CONNECTOR_DEP IDENTITY_DEP

  [[ -n "${RB_DEP}" && -n "${QUERY_DEP}" && -n "${CONNECTOR_DEP}" ]]
}
