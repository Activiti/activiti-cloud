#!/usr/bin/env bash
# kubectl inspection helpers for acceptance prerequisite patches.

file_sha256() {
  if command -v sha256sum &>/dev/null; then
    sha256sum "$1" | awk '{print $1}'
  else
    shasum -a 256 "$1" | awk '{print $1}'
  fi
}

dir_sha256() {
  local dir=$1
  if command -v sha256sum &>/dev/null; then
    tar -cf - -C "${dir}" . 2>/dev/null | sha256sum | awk '{print $1}'
  else
    tar -cf - -C "${dir}" . 2>/dev/null | shasum -a 256 | awk '{print $1}'
  fi
}

dep_in_list() {
  local dep=$1
  shift
  local item
  if [[ $# -eq 0 ]]; then
    return 1
  fi
  for item in "$@"; do
    [[ "${item}" == "${dep}" ]] && return 0
  done
  return 1
}

deployment_exists() {
  workload_exists "$1"
}

workload_kind() {
  workload_kind_in_namespace "${NAMESPACE}" "$1"
}

workload_exists() {
  [[ -n "$(workload_kind "$1")" ]]
}

deployment_container_name() {
  local dep=$1
  local kind
  kind="$(workload_kind "${dep}")"
  [[ -z "${kind}" ]] && return 1
  kubectl get "${kind}" "$1" -n "${NAMESPACE}" -o jsonpath='{.spec.template.spec.containers[0].name}'
}

deployment_env_value() {
  local dep=$1 env_name=$2 kind
  kind="$(workload_kind "${dep}")"
  [[ -z "${kind}" ]] && return 0
  kubectl get "${kind}" "${dep}" -n "${NAMESPACE}" -o json \
    | python3 -c "import json,sys; d=json.load(sys.stdin); env=d['spec']['template']['spec']['containers'][0].get('env',[]); print(next((e.get('value','') for e in env if e.get('name')=='${env_name}'), ''))" 2>/dev/null || true
}

deployment_has_host_alias() {
  local dep=$1 kind
  kind="$(workload_kind "${dep}")"
  [[ -z "${kind}" ]] && return 1
  kubectl get "${kind}" "${dep}" -n "${NAMESPACE}" -o json \
    | TRAEFIK_IP="${TRAEFIK_IP}" IDENTITY_HOST="${IDENTITY_HOST}" GATEWAY_HOST="${GATEWAY_HOST}" \
      python3 -c "
import json, os, sys
d = json.load(sys.stdin)
aliases = d.get('spec', {}).get('template', {}).get('spec', {}).get('hostAliases') or []
if not aliases:
    sys.exit(1)
entry = aliases[0]
if entry.get('ip') != os.environ['TRAEFIK_IP']:
    sys.exit(1)
hosts = set(entry.get('hostnames') or [])
needed = {os.environ['IDENTITY_HOST'], os.environ['GATEWAY_HOST']}
sys.exit(0 if needed <= hosts else 1)
" 2>/dev/null
}

deployment_has_policy_mount() {
  local dep=$1 kind
  kind="$(workload_kind "${dep}")"
  [[ -z "${kind}" ]] && return 1
  kubectl get "${kind}" "${dep}" -n "${NAMESPACE}" -o json \
    | python3 -c "import json,sys; d=json.load(sys.stdin); mounts=d['spec']['template']['spec']['containers'][0].get('volumeMounts',[]); sys.exit(0 if any(m.get('name')=='acceptance-security-policies' for m in mounts) else 1)" 2>/dev/null
}

deployment_has_supplemental_process_mount() {
  local dep=$1 kind
  kind="$(workload_kind "${dep}")"
  [[ -z "${kind}" ]] && return 1
  kubectl get "${kind}" "${dep}" -n "${NAMESPACE}" -o json \
    | python3 -c "import json,sys; d=json.load(sys.stdin); mounts=d['spec']['template']['spec']['containers'][0].get('volumeMounts',[]); sys.exit(0 if any(m.get('name')=='acceptance-supplemental-processes' for m in mounts) else 1)" 2>/dev/null
}
