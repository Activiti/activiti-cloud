#!/usr/bin/env bash
# Shared preview naming and host URLs for CI and local tooling.

preview_name_from_ci_matrix() {
  local pr_number="${1:-}"
  local run_number="${2:-}"
  local broker="${3:?broker required}"
  local partitioned="${4:?partitioning required}"
  local destinations="${5:?destinations required}"

  local base
  if [[ -n "${pr_number}" ]]; then
    base="pr-${pr_number}"
  else
    base="gh-${run_number}"
  fi
  echo "${base}-${broker:0:6}-${partitioned:0:1}-${destinations:0:1}"
}

preview_global_gateway_domain() {
  local cluster_name="${1:-${CLUSTER_NAME:-activiti}}"
  local cluster_domain="${2:-${CLUSTER_DOMAIN:-envalfresco.com}}"
  echo "${cluster_name}.${cluster_domain}"
}

preview_gateway_host() {
  local preview_name=$1
  local global_domain
  global_domain="$(preview_global_gateway_domain)"
  echo "gateway-${preview_name}.${global_domain}"
}

preview_identity_host() {
  local preview_name=$1
  local global_domain
  global_domain="$(preview_global_gateway_domain)"
  echo "identity-${preview_name}.${global_domain}"
}

export_preview_gateway_env() {
  export GLOBAL_GATEWAY_DOMAIN="${GLOBAL_GATEWAY_DOMAIN:-$(preview_global_gateway_domain)}"
  export GATEWAY_PROTOCOL="${GATEWAY_PROTOCOL:-https}"
  export SSO_PROTOCOL="${SSO_PROTOCOL:-https}"
  export GATEWAY_HOST="${GATEWAY_HOST:-$(preview_gateway_host "${PREVIEW_NAME}")}"
  export SSO_HOST="${SSO_HOST:-$(preview_identity_host "${PREVIEW_NAME}")}"
}

ci_append_github_env() {
  local key=$1
  local value=$2
  if [[ "${GITHUB_ACTIONS:-}" == "true" && -n "${GITHUB_ENV:-}" ]]; then
    echo "${key}=${value}" >> "${GITHUB_ENV}"
  fi
}
