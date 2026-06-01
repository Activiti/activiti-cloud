#!/usr/bin/env bash
# Resolve Kubernetes deployment names (Helm release names may be truncated).

# Longest workload suffix: {release}-activiti-cloud-identity-adapter (33 chars) → release max 29.
PREVIEW_RELEASE_MAX_LENGTH=29

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

find_statefulset_in_namespace() {
  local namespace=$1
  local pattern=$2
  local candidate

  for candidate in "${namespace}-${pattern}" "${pattern}"; do
    if kubectl get statefulset "${candidate}" -n "${namespace}" &>/dev/null; then
      echo "${candidate}"
      return 0
    fi
  done

  kubectl get statefulset -n "${namespace}" -o jsonpath='{range .items[*]}{.metadata.name}{"\n"}{end}' 2>/dev/null \
    | grep -i "${pattern}" \
    | head -1
}

# Helm chart uses Deployment or StatefulSet depending on broker/partitioning profile.
find_workload_in_namespace() {
  local namespace=$1
  local pattern=$2
  local name

  name="$(find_deployment_in_namespace "${namespace}" "${pattern}")"
  if [[ -n "${name}" ]]; then
    echo "${name}"
    return 0
  fi

  find_statefulset_in_namespace "${namespace}" "${pattern}"
}

workload_kind_in_namespace() {
  local namespace=$1
  local name=$2

  if kubectl get deployment "${name}" -n "${namespace}" &>/dev/null; then
    echo "deployment"
    return 0
  fi
  if kubectl get statefulset "${name}" -n "${namespace}" &>/dev/null; then
    echo "statefulset"
    return 0
  fi
  return 1
}

validate_preview_release_name_length() {
  local preview_name=$1
  if [[ ${#preview_name} -gt ${PREVIEW_RELEASE_MAX_LENGTH} ]]; then
    echo "Preview namespace '${preview_name}' is ${#preview_name} characters; maximum is ${PREVIEW_RELEASE_MAX_LENGTH}." >&2
    echo "Kubernetes limits deployment names to 63 chars; identity-adapter needs a shorter env name (~13 chars with default rabbitmq/non-partitioned)." >&2
    echo "Example: npm run test:setup -- --install --name jane-a3f2b1" >&2
    return 1
  fi
  return 0
}

patch_deployment_keycloak_env() {
  local deployment=$1
  local namespace=$2
  local keycloak_url=$3
  local keycloak_realm=$4
  local container

  container="$(kubectl get deployment "${deployment}" -n "${namespace}" \
    -o jsonpath='{.spec.template.spec.containers[0].name}' 2>/dev/null || true)"
  [[ -z "${container}" ]] && return 1

  kubectl patch deployment "${deployment}" -n "${namespace}" --type=strategic -p \
    "{\"spec\":{\"template\":{\"spec\":{\"containers\":[{\"name\":\"${container}\",\"env\":[{\"name\":\"ACT_KEYCLOAK_URL\",\"value\":\"${keycloak_url}\"},{\"name\":\"ACT_KEYCLOAK_REALM\",\"value\":\"${keycloak_realm}\"}]}]}}}}"
}
