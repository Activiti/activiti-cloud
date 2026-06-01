#!/usr/bin/env bash
# Applies cluster settings required for full Playwright / Serenity acceptance parity.
# Only patches or restarts workloads when something is missing or out of date.
#
# Usage (from repo root):
#   npm run cluster:prereqs
#   # or: bash activiti-cloud-acceptance-tests-playwright/scripts/apply-cluster-prereqs.sh [namespace]

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PKG_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
ROOT_DIR="$(cd "${PKG_DIR}/.." && pwd)"

# shellcheck source=lib/cluster-discovery.sh
source "${SCRIPT_DIR}/lib/cluster-discovery.sh"
# shellcheck source=lib/prereqs-progress.sh
source "${SCRIPT_DIR}/lib/prereqs-progress.sh"

REQUESTED_NAMESPACE="${1:-${PREVIEW_NAME:-}}"

# Local only: .env must not override CI workflow env (PREVIEW_NAME, GATEWAY_HOST, …).
if [[ "${GITHUB_ACTIONS:-}" != "true" && "${CI:-}" != "true" && -f "${PKG_DIR}/.env" ]]; then
  # shellcheck source=/dev/null
  set -a && source "${PKG_DIR}/.env" && set +a
  # Re-apply CLI / parent env namespace after sourcing .env
  if [[ -n "${1:-}" ]]; then
    PREVIEW_NAME="${1}"
    export PREVIEW_NAME
  fi
fi

prereqs_phase_actor coordinator "Cluster prerequisites"
prereqs_step "discovering preview namespace"
NAMESPACE="$(discover_preview_namespace "${REQUESTED_NAMESPACE}" || true)"
if [[ -z "${NAMESPACE}" ]]; then
  echo "Usage: $0 [kubernetes-namespace]"
  echo "Or set PREVIEW_NAME / ACCEPTANCE_ENV_NAME in activiti-cloud-acceptance-tests-playwright/.env"
  echo "First-time setup: npm run test:setup -- --install"
  exit 1
fi

prereqs_step "discovering Activiti deployments in ${NAMESPACE}"
if [[ "${GITHUB_ACTIONS:-}" == "true" || "${CI:-}" == "true" ]]; then
  wait_for_acceptance_deployments "${NAMESPACE}" || exit 1
elif ! discover_acceptance_deployments "${NAMESPACE}"; then
  echo "ERROR: Activiti Cloud is not installed in namespace ${NAMESPACE}"
  echo "  (missing runtime-bundle / query / connector deployments)"
  echo ""
  echo "Install once, then re-run:"
  echo "  npm run test:setup -- --install"
  echo ""
  echo "Or manually:"
  echo "  ./scripts/local-install.sh -n \${ACCEPTANCE_ENV_NAME:-\$(whoami)-local} -c \${CLUSTER_NAME:-activiti}"
  exit 1
fi

HOST_ALIAS_DEPLOYMENTS=(
  "${RB_DEP}"
  "${QUERY_DEP}"
  "${IDENTITY_DEP}"
  "${CONNECTOR_DEP}"
)

POLICY_DEPLOYMENTS=(
  "${RB_DEP}"
  "${QUERY_DEP}"
)
# Audit is optional in some chart profiles.
if [[ -n "${AUDIT_DEP:-}" ]]; then
  POLICY_DEPLOYMENTS+=("${AUDIT_DEP}")
fi

GATEWAY_HOST="${GATEWAY_HOST:-}"
if [[ -z "${GATEWAY_HOST}" && -n "${PREVIEW_NAME:-}" && -n "${CLUSTER_NAME:-}" ]]; then
  DOMAIN="${CLUSTER_DOMAIN:-envalfresco.com}"
  GATEWAY_HOST="gateway-${PREVIEW_NAME}.${CLUSTER_NAME}.${DOMAIN}"
fi

if [[ -z "${GATEWAY_HOST}" ]]; then
  echo "Set GATEWAY_HOST or PREVIEW_NAME + CLUSTER_NAME in .env"
  exit 1
fi

GATEWAY_HOST="${GATEWAY_HOST%%:*}"
IDENTITY_HOST="identity-${GATEWAY_HOST#gateway-}"

PF_NS="${PORT_FORWARD_NAMESPACE:-traefik}"
PF_SVC="${PORT_FORWARD_SERVICE:-traefik}"
prereqs_set_actor traefik
prereqs_step "asking Traefik for clusterIP (${PF_SVC} in ${PF_NS})"
TRAEFIK_IP="$(kubectl get svc "${PF_SVC}" -n "${PF_NS}" -o jsonpath='{.spec.clusterIP}')"
prereqs_log "Traefik clusterIP: ${TRAEFIK_IP}"
# CI reaches Keycloak via HTTPS ingress; JWT iss is https. RB must use the same issuer (http → 401 on /rb APIs).
if [[ "${GITHUB_ACTIONS:-}" == "true" || "${CI:-}" == "true" || "${GATEWAY_PROTOCOL:-}" == "https" ]]; then
  ACT_KEYCLOAK_URL="https://${IDENTITY_HOST}/auth"
else
  ACT_KEYCLOAK_URL="http://${IDENTITY_HOST}/auth"
fi

read_runtime_bundle_tag_from_values() {
  local values_file=$1
  if [[ ! -f "${values_file}" ]]; then
    return 1
  fi
  if command -v yq &>/dev/null; then
    yq e '.runtime-bundle.image.tag' "${values_file}" 2>/dev/null || true
    return 0
  fi
  grep -A3 '^runtime-bundle:' "${values_file}" | grep 'tag:' | head -1 | sed 's/.*tag:[[:space:]]*\"\\?\\([^\"]*\\)\"\\?.*/\\1/'
}

resolve_runtime_bundle_image() {
  prereqs_set_actor registry
  if [[ -n "${ACCEPTANCE_RUNTIME_BUNDLE_IMAGE:-}" ]]; then
    prereqs_log "using ACCEPTANCE_RUNTIME_BUNDLE_IMAGE from env — no registry drama today"
    return
  fi
  local resolved_tag=""
  if [[ "${ACCEPTANCE_SKIP_IMAGE_RESOLVE:-}" == "true" ]]; then
    local values_file=""
    if [[ -f "${ROOT_DIR}/local-values.local.yaml" ]]; then
      values_file="${ROOT_DIR}/local-values.local.yaml"
    elif [[ -f "${ROOT_DIR}/local-values.yaml" ]]; then
      values_file="${ROOT_DIR}/local-values.yaml"
    fi
    if [[ -n "${values_file}" ]]; then
      resolved_tag="$(read_runtime_bundle_tag_from_values "${values_file}")"
      prereqs_log "using tag from ${values_file} (fresh Helm install — skip registry resolve)"
    fi
  elif [[ "${ACCEPTANCE_RUNTIME_BUNDLE_USE_RESOLVED_TAG:-true}" == "true" && -f "${ROOT_DIR}/scripts/resolve-docker-images.sh" ]]; then
    prereqs_step "resolving latest activiti/example-runtime-bundle tag (registry; may take 1–2 min)"
    run_with_heartbeat registry "resolve-docker-images" bash -c "cd '${ROOT_DIR}' && bash scripts/resolve-docker-images.sh" || true
    local values_file=""
    if [[ -f "${ROOT_DIR}/local-values.local.yaml" ]]; then
      values_file="${ROOT_DIR}/local-values.local.yaml"
    elif [[ -f "${ROOT_DIR}/local-values.yaml" ]]; then
      values_file="${ROOT_DIR}/local-values.yaml"
    fi
    if [[ -n "${values_file}" ]]; then
      if command -v yq &>/dev/null; then
        resolved_tag="$(yq e '.runtime-bundle.image.tag' "${values_file}" 2>/dev/null || true)"
      else
        resolved_tag="$(grep -A3 '^runtime-bundle:' "${values_file}" | grep 'tag:' | head -1 | sed 's/.*tag:[[:space:]]*\"\\?\\([^\"]*\\)\"\\?.*/\\1/')"
      fi
    fi
  fi
  if [[ -n "${resolved_tag}" ]]; then
    ACCEPTANCE_RUNTIME_BUNDLE_IMAGE="activiti/example-runtime-bundle:${resolved_tag}"
  else
    ACCEPTANCE_RUNTIME_BUNDLE_IMAGE="activiti/example-runtime-bundle:latest"
  fi
}

prereqs_phase_actor registry "Resolve runtime bundle image"
resolve_runtime_bundle_image

# Full example-runtime-bundle images already ship HeadersConnectorProcess on the classpath.
# Supplemental ConfigMap mount is only required for slim chart images (override with ACCEPTANCE_USE_SUPPLEMENTAL_PROCESSES).
NEEDS_SUPPLEMENTAL_PROCESSES=1
if [[ "${ACCEPTANCE_RUNTIME_BUNDLE_IMAGE}" == *"example-runtime-bundle"* ]]; then
  NEEDS_SUPPLEMENTAL_PROCESSES=0
fi
if [[ -n "${ACCEPTANCE_USE_SUPPLEMENTAL_PROCESSES:-}" ]]; then
  case "${ACCEPTANCE_USE_SUPPLEMENTAL_PROCESSES,,}" in
    true|1|yes) NEEDS_SUPPLEMENTAL_PROCESSES=1 ;;
    false|0|no) NEEDS_SUPPLEMENTAL_PROCESSES=0 ;;
  esac
fi

POLICY_FILE="${PKG_DIR}/config/cluster/acceptance-security-policies.properties"
POLICY_CONFIG="optional:file:/config/acceptance/acceptance-security-policies.properties"
CONFIGMAP_NAME="acceptance-security-policies"
SUPPLEMENTAL_PROCESSES_DIR="${PKG_DIR}/resources/modeling-projects/acceptance"
SUPPLEMENTAL_CONFIGMAP="acceptance-supplemental-processes"
PROCESS_LOCATION_CLASSPATH="classpath*:/processes/"
PROCESS_LOCATION_SUPPLEMENTAL="file:/config/acceptance-supplemental-processes/"

prereqs_set_actor coordinator
prereqs_log "Namespace:              ${NAMESPACE}"
prereqs_set_actor runtime-bundle && prereqs_log "Runtime bundle deploy:  ${RB_DEP}"
prereqs_set_actor query && prereqs_log "Query deploy:           ${QUERY_DEP}"
prereqs_set_actor connector && prereqs_log "Connector deploy:       ${CONNECTOR_DEP}"
prereqs_set_actor traefik && prereqs_log "Gateway host:           ${GATEWAY_HOST}"
prereqs_set_actor identity && prereqs_log "Identity host:          ${IDENTITY_HOST} → ${ACT_KEYCLOAK_URL}"
prereqs_set_actor runtime-bundle && prereqs_log "Runtime bundle image:   ${ACCEPTANCE_RUNTIME_BUNDLE_IMAGE}"
if [[ "${NEEDS_SUPPLEMENTAL_PROCESSES}" -eq 1 ]]; then
  prereqs_set_actor policies && prereqs_log "Supplemental BPMN:      enabled"
else
  prereqs_set_actor runtime-bundle && prereqs_log "Supplemental BPMN:      skipped — full catalog on classpath"
fi

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

deployment_exists() {
  kubectl get deployment "$1" -n "${NAMESPACE}" &>/dev/null
}

deployment_container_name() {
  kubectl get deployment "$1" -n "${NAMESPACE}" -o jsonpath='{.spec.template.spec.containers[0].name}'
}

deployment_env_value() {
  local dep=$1 env_name=$2
  kubectl get deployment "${dep}" -n "${NAMESPACE}" -o json \
    | python3 -c "import json,sys; d=json.load(sys.stdin); env=d['spec']['template']['spec']['containers'][0].get('env',[]); print(next((e.get('value','') for e in env if e.get('name')=='${env_name}'), ''))" 2>/dev/null || true
}

deployment_has_host_alias() {
  local dep=$1
  kubectl get deployment "${dep}" -n "${NAMESPACE}" -o json \
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
  local dep=$1
  kubectl get deployment "${dep}" -n "${NAMESPACE}" -o json \
    | python3 -c "import json,sys; d=json.load(sys.stdin); mounts=d['spec']['template']['spec']['containers'][0].get('volumeMounts',[]); sys.exit(0 if any(m.get('name')=='acceptance-security-policies' for m in mounts) else 1)" 2>/dev/null
}

deployment_has_supplemental_process_mount() {
  local dep=$1
  kubectl get deployment "${dep}" -n "${NAMESPACE}" -o json \
    | python3 -c "import json,sys; d=json.load(sys.stdin); mounts=d['spec']['template']['spec']['containers'][0].get('volumeMounts',[]); sys.exit(0 if any(m.get('name')=='acceptance-supplemental-processes' for m in mounts) else 1)" 2>/dev/null
}

deployment_needs_host_patch() {
  local dep=$1
  if ! deployment_has_host_alias "${dep}"; then
    return 0
  fi
  local current_kc_url current_kc_realm
  current_kc_url="$(deployment_env_value "${dep}" "ACT_KEYCLOAK_URL")"
  current_kc_realm="$(deployment_env_value "${dep}" "ACT_KEYCLOAK_REALM")"
  [[ "${current_kc_url}" != "${ACT_KEYCLOAK_URL}" ]] && return 0
  [[ "${current_kc_realm}" != "${KEYCLOAK_REALM:-activiti}" ]]
}

apply_acceptance_deployment_patch() {
  local dep=$1
  local include_policy=$2
  local include_host=$3
  local patch_json result
  export NAMESPACE RB_DEP ACT_KEYCLOAK_URL POLICY_CONFIG NEEDS_SUPPLEMENTAL_PROCESSES
  export PROCESS_LOCATION_CLASSPATH PROCESS_LOCATION_SUPPLEMENTAL
  export DEP_NAME="${dep}" KEYCLOAK_REALM="${KEYCLOAK_REALM:-activiti}"
  export INCLUDE_POLICY="${include_policy}" INCLUDE_HOST="${include_host}"
  export HOST_ALIASES="[{\"ip\":\"${TRAEFIK_IP}\",\"hostnames\":[\"${IDENTITY_HOST}\",\"${GATEWAY_HOST}\"]}]"
  patch_json="$(
    python3 -c "
import json, os
patch = {'namespace': os.environ['NAMESPACE'], 'deployment': os.environ['DEP_NAME']}
if os.environ.get('INCLUDE_HOST') == '1':
    patch['hostAliases'] = json.loads(os.environ['HOST_ALIASES'])
    patch['keycloakUrl'] = os.environ['ACT_KEYCLOAK_URL']
    patch['keycloakRealm'] = os.environ['KEYCLOAK_REALM']
if os.environ.get('INCLUDE_POLICY') == '1':
    patch['policyConfig'] = os.environ['POLICY_CONFIG']
    if os.environ['DEP_NAME'] == os.environ['RB_DEP']:
        patch['runtimeBundle'] = True
        patch['processClasspath'] = os.environ['PROCESS_LOCATION_CLASSPATH']
        patch['processSupplemental'] = os.environ['PROCESS_LOCATION_SUPPLEMENTAL']
        if os.environ.get('NEEDS_SUPPLEMENTAL_PROCESSES') == '1':
            patch['supplementalProcesses'] = True
print(json.dumps(patch))
" 2>/dev/null
  )" || return 1
  result="$(python3 "${SCRIPT_DIR}/lib/patch-acceptance-deployment.py" "${patch_json}" 2>&1)" || return 1
  if [[ "${result}" == UNCHANGED:* ]]; then
    prereqs_log "✓ ${dep} — pod template already matches (no rollout)"
    return 2
  fi
  if [[ "${result}" == CHANGED:* ]]; then
    return 0
  fi
  echo "${result}" >&2
  return 1
}

dep_in_list() {
  local dep=$1
  shift
  local item
  # With set -u, "${arr[@]}" on an empty arr errors on older bash (macOS default).
  if [[ $# -eq 0 ]]; then
    return 1
  fi
  for item in "$@"; do
    [[ "${item}" == "${dep}" ]] && return 0
  done
  return 1
}

CHANGED=0

# --- example-runtime-bundle image (full BPMN catalog from Serenity parity chart) ---
prereqs_phase_actor runtime-bundle "Runtime bundle image"
if deployment_exists "${RB_DEP}"; then
  prereqs_step "checking ${RB_DEP} container image"
  CURRENT_IMAGE="$(kubectl get deployment "${RB_DEP}" -n "${NAMESPACE}" -o jsonpath='{.spec.template.spec.containers[0].image}')"
  if [[ "${CURRENT_IMAGE}" != "${ACCEPTANCE_RUNTIME_BUNDLE_IMAGE}" ]]; then
    prereqs_log "updating ${RB_DEP} image to ${ACCEPTANCE_RUNTIME_BUNDLE_IMAGE} (was: ${CURRENT_IMAGE})"
    CONTAINER="$(deployment_container_name "${RB_DEP}")"
    kubectl set image deployment/"${RB_DEP}" -n "${NAMESPACE}" "${CONTAINER}=${ACCEPTANCE_RUNTIME_BUNDLE_IMAGE}"
    wait_rollout_one "${RB_DEP}" 300 || exit 1
    CHANGED=1
  else
    prereqs_log "✓ ${RB_DEP} image already ${CURRENT_IMAGE}"
  fi
fi

if ! deployment_exists "${CONNECTOR_DEP}"; then
  echo "ERROR: Missing deployment ${CONNECTOR_DEP} — install chart with example-cloud-connector"
  exit 1
fi
echo "✓ ${CONNECTOR_DEP} present"

# --- acceptance security policies ConfigMap ---
prereqs_phase_actor policies "Security policies ConfigMaps"
prereqs_step "checking ${CONFIGMAP_NAME} ConfigMap"
NEW_POLICY_HASH="$(file_sha256 "${POLICY_FILE}")"
OLD_POLICY_HASH="$(kubectl get configmap "${CONFIGMAP_NAME}" -n "${NAMESPACE}" -o jsonpath='{.metadata.annotations.acceptance-policy-sha256}' 2>/dev/null || true)"

POLICY_CM_CHANGED=0
if [[ "${NEW_POLICY_HASH}" != "${OLD_POLICY_HASH}" ]]; then
  echo "Applying acceptance security policies ConfigMap (content changed)..."
  kubectl create configmap "${CONFIGMAP_NAME}" \
    --from-file=acceptance-security-policies.properties="${POLICY_FILE}" \
    -n "${NAMESPACE}" \
    --dry-run=client -o yaml | kubectl apply -f -
  kubectl annotate configmap "${CONFIGMAP_NAME}" -n "${NAMESPACE}" \
    "acceptance-policy-sha256=${NEW_POLICY_HASH}" --overwrite
  POLICY_CM_CHANGED=1
  CHANGED=1
else
  echo "✓ ${CONFIGMAP_NAME} ConfigMap already up to date"
fi

# --- supplemental BPMN ConfigMap (HeadersConnectorProcess — not in slim chart image catalog) ---
SUPPLEMENTAL_CM_CHANGED=0
if [[ -d "${SUPPLEMENTAL_PROCESSES_DIR}" ]]; then
  NEW_SUPPLEMENTAL_HASH="$(dir_sha256 "${SUPPLEMENTAL_PROCESSES_DIR}")"
  OLD_SUPPLEMENTAL_HASH="$(kubectl get configmap "${SUPPLEMENTAL_CONFIGMAP}" -n "${NAMESPACE}" -o jsonpath='{.metadata.annotations.acceptance-supplemental-sha256}' 2>/dev/null || true)"

  if [[ "${NEW_SUPPLEMENTAL_HASH}" != "${OLD_SUPPLEMENTAL_HASH}" ]]; then
    echo "Applying supplemental acceptance processes ConfigMap..."
    kubectl create configmap "${SUPPLEMENTAL_CONFIGMAP}" \
      --from-file="${SUPPLEMENTAL_PROCESSES_DIR}" \
      -n "${NAMESPACE}" \
      --dry-run=client -o yaml | kubectl apply -f -
    kubectl annotate configmap "${SUPPLEMENTAL_CONFIGMAP}" -n "${NAMESPACE}" \
      "acceptance-supplemental-sha256=${NEW_SUPPLEMENTAL_HASH}" --overwrite
    SUPPLEMENTAL_CM_CHANGED=1
    CHANGED=1
  else
    echo "✓ ${SUPPLEMENTAL_CONFIGMAP} ConfigMap already up to date"
  fi
fi

patch_policy_mount_strategic() {
  local dep=$1
  kubectl patch deployment "${dep}" -n "${NAMESPACE}" --type=strategic -p "
spec:
  template:
    spec:
      volumes:
        - name: acceptance-security-policies
          configMap:
            name: acceptance-security-policies
      containers:
        - name: $(deployment_container_name "${dep}")
          volumeMounts:
            - name: acceptance-security-policies
              mountPath: /config/acceptance
              readOnly: true
"
}

patch_runtime_bundle_policy_mount() {
  local container
  container="$(deployment_container_name "${RB_DEP}")"
  kubectl patch deployment "${RB_DEP}" -n "${NAMESPACE}" --type=strategic -p "
spec:
  template:
    spec:
      volumes:
        - name: acceptance-security-policies
          configMap:
            name: acceptance-security-policies
      containers:
        - name: ${container}
          volumeMounts:
            - name: acceptance-security-policies
              mountPath: /config/acceptance
              readOnly: true
"
}

patch_runtime_bundle_acceptance_mounts() {
  patch_runtime_bundle_policy_mount
  if [[ "${NEEDS_SUPPLEMENTAL_PROCESSES}" -eq 1 ]]; then
    local container
    container="$(deployment_container_name "${RB_DEP}")"
    kubectl patch deployment "${RB_DEP}" -n "${NAMESPACE}" --type=strategic -p "
spec:
  template:
    spec:
      volumes:
        - name: acceptance-supplemental-processes
          configMap:
            name: acceptance-supplemental-processes
      containers:
        - name: ${container}
          volumeMounts:
            - name: acceptance-supplemental-processes
              mountPath: /config/acceptance-supplemental-processes
              readOnly: true
"
  fi
}

strip_supplemental_mount_from_rb() {
  kubectl get deployment "${RB_DEP}" -n "${NAMESPACE}" -o json | python3 -c "
import json,sys
d=json.load(sys.stdin)
spec=d['spec']['template']['spec']
spec['volumes']=[v for v in spec.get('volumes',[]) if v.get('name')!='acceptance-supplemental-processes']
mounts=spec['containers'][0].get('volumeMounts',[])
spec['containers'][0]['volumeMounts']=[m for m in mounts if m.get('name')!='acceptance-supplemental-processes']
print(json.dumps(d))
" | kubectl apply -f -
}

apply_policy_patches_for_dep() {
  local dep=$1

  if [[ "${dep}" == "${RB_DEP}" ]]; then
    local rb_mount_ok=0
    if [[ "${NEEDS_SUPPLEMENTAL_PROCESSES}" -eq 1 ]]; then
      if deployment_has_policy_mount "${dep}" && deployment_has_supplemental_process_mount "${dep}"; then
        rb_mount_ok=1
      fi
    elif deployment_has_policy_mount "${dep}" && ! deployment_has_supplemental_process_mount "${dep}"; then
      rb_mount_ok=1
    fi
    if [[ "${rb_mount_ok}" -eq 0 ]]; then
      patch_runtime_bundle_acceptance_mounts
    fi
  elif ! deployment_has_policy_mount "${dep}"; then
    patch_policy_mount_strategic "${dep}"
  fi

  local current_import
  current_import="$(deployment_env_value "${dep}" "SPRING_CONFIG_IMPORT")"
  if [[ "${current_import}" != "${POLICY_CONFIG}" ]]; then
    kubectl set env deployment/"${dep}" -n "${NAMESPACE}" \
      "SPRING_CONFIG_IMPORT=${POLICY_CONFIG}" \
      "SPRING_CONFIG_ADDITIONAL_LOCATION-"
  fi

  if [[ "${dep}" == "${RB_DEP}" ]]; then
    local current_p0 current_p1
    current_p0="$(deployment_env_value "${dep}" "SPRING_ACTIVITI_PROCESS_DEFINITION_LOCATION_PREFIX_0")"
    current_p1="$(deployment_env_value "${dep}" "SPRING_ACTIVITI_PROCESS_DEFINITION_LOCATION_PREFIX_1")"
    if [[ "${NEEDS_SUPPLEMENTAL_PROCESSES}" -eq 1 ]]; then
      if [[ "${current_p0}" != "${PROCESS_LOCATION_CLASSPATH}" || "${current_p1}" != "${PROCESS_LOCATION_SUPPLEMENTAL}" ]]; then
        kubectl set env deployment/"${dep}" -n "${NAMESPACE}" \
          "SPRING_ACTIVITI_PROCESS_DEFINITION_LOCATION_PREFIX-" \
          "SPRING_ACTIVITI_PROCESS_DEFINITION_LOCATION_PREFIX_0=${PROCESS_LOCATION_CLASSPATH}" \
          "SPRING_ACTIVITI_PROCESS_DEFINITION_LOCATION_PREFIX_1=${PROCESS_LOCATION_SUPPLEMENTAL}" \
          "SPRING_ACTIVITI_PROCESS_EXTENSIONS_LOCATION_PREFIX-" \
          "SPRING_ACTIVITI_PROCESS_EXTENSIONS_LOCATION_PREFIX_0=${PROCESS_LOCATION_CLASSPATH}" \
          "SPRING_ACTIVITI_PROCESS_EXTENSIONS_LOCATION_PREFIX_1=${PROCESS_LOCATION_SUPPLEMENTAL}"
      fi
    elif [[ "${current_p0}" != "${PROCESS_LOCATION_CLASSPATH}" || -n "${current_p1}" ]]; then
      kubectl set env deployment/"${dep}" -n "${NAMESPACE}" \
        "SPRING_ACTIVITI_PROCESS_DEFINITION_LOCATION_PREFIX-" \
        "SPRING_ACTIVITI_PROCESS_DEFINITION_LOCATION_PREFIX_0=${PROCESS_LOCATION_CLASSPATH}" \
        "SPRING_ACTIVITI_PROCESS_DEFINITION_LOCATION_PREFIX_1-" \
        "SPRING_ACTIVITI_PROCESS_EXTENSIONS_LOCATION_PREFIX-" \
        "SPRING_ACTIVITI_PROCESS_EXTENSIONS_LOCATION_PREFIX_0=${PROCESS_LOCATION_CLASSPATH}" \
        "SPRING_ACTIVITI_PROCESS_EXTENSIONS_LOCATION_PREFIX_1-"
    fi
  fi
}

policy_dep_needs_patch() {
  local dep=$1

  if [[ "${dep}" == "${RB_DEP}" ]]; then
    if [[ "${NEEDS_SUPPLEMENTAL_PROCESSES}" -eq 1 ]]; then
      deployment_has_policy_mount "${dep}" && deployment_has_supplemental_process_mount "${dep}" || return 0
    elif deployment_has_policy_mount "${dep}" && ! deployment_has_supplemental_process_mount "${dep}"; then
      :
    else
      return 0
    fi
  elif ! deployment_has_policy_mount "${dep}"; then
    return 0
  fi

  local current_import
  current_import="$(deployment_env_value "${dep}" "SPRING_CONFIG_IMPORT")"
  [[ "${current_import}" != "${POLICY_CONFIG}" ]] && return 0

  if [[ "${dep}" == "${RB_DEP}" ]]; then
    local current_p0 current_p1
    current_p0="$(deployment_env_value "${dep}" "SPRING_ACTIVITI_PROCESS_DEFINITION_LOCATION_PREFIX_0")"
    current_p1="$(deployment_env_value "${dep}" "SPRING_ACTIVITI_PROCESS_DEFINITION_LOCATION_PREFIX_1")"
    if [[ "${NEEDS_SUPPLEMENTAL_PROCESSES}" -eq 1 ]]; then
      [[ "${current_p0}" != "${PROCESS_LOCATION_CLASSPATH}" || "${current_p1}" != "${PROCESS_LOCATION_SUPPLEMENTAL}" ]] && return 0
    elif [[ "${current_p0}" != "${PROCESS_LOCATION_CLASSPATH}" || -n "${current_p1}" ]]; then
      return 0
    fi
  fi

  return 1
}

# --- hostAliases + Keycloak + policy mounts (one kubectl apply per deployment → one rollout) ---
prereqs_phase_actor coordinator "Acceptance workload configuration"
WORKLOAD_ROLLOUTS=()
WORKLOAD_PIDS=()
WORKLOAD_DEP_NAMES=()
WORKLOAD_DEPS=()
for dep in "${HOST_ALIAS_DEPLOYMENTS[@]}" "${POLICY_DEPLOYMENTS[@]}"; do
  [[ -n "${dep}" ]] || continue
  if ((${#WORKLOAD_DEPS[@]} > 0)) && dep_in_list "${dep}" "${WORKLOAD_DEPS[@]}"; then
    continue
  fi
  WORKLOAD_DEPS+=("${dep}")
done

for dep in "${WORKLOAD_DEPS[@]}"; do
  [[ -n "${dep}" ]] || continue
  prereqs_set_actor "$(prereqs_actor_for_dep "${dep}")"
  if ! deployment_exists "${dep}"; then
    prereqs_log "skip missing deployment: ${dep}"
    continue
  fi

  include_host=0
  include_policy=0
  needs_patch=0
  dep_in_list "${dep}" "${HOST_ALIAS_DEPLOYMENTS[@]}" && include_host=1
  dep_in_list "${dep}" "${POLICY_DEPLOYMENTS[@]}" && include_policy=1

  if [[ "${include_host}" -eq 1 ]] && deployment_needs_host_patch "${dep}"; then
    needs_patch=1
  fi
  if [[ "${include_policy}" -eq 1 ]] && policy_dep_needs_patch "${dep}"; then
    needs_patch=1
  fi

  if [[ "${needs_patch}" -eq 0 ]]; then
    echo "✓ ${dep} acceptance workload config already up to date"
    continue
  fi

  prereqs_step "patching ${dep} (single apply: hostAliases + Keycloak + policies)"
  apply_acceptance_deployment_patch "${dep}" "${include_policy}" "${include_host}" &
  WORKLOAD_PIDS+=($!)
  WORKLOAD_DEP_NAMES+=("${dep}")
done
if ((${#WORKLOAD_PIDS[@]} > 0)); then
  for i in "${!WORKLOAD_PIDS[@]}"; do
    pid="${WORKLOAD_PIDS[$i]}"
    dep="${WORKLOAD_DEP_NAMES[$i]}"
    patch_status=0
    wait "${pid}" || patch_status=$?
    if [[ "${patch_status}" -eq 0 ]]; then
      WORKLOAD_ROLLOUTS+=("${dep}")
    elif [[ "${patch_status}" -ne 2 ]]; then
      echo "ERROR: failed to patch ${dep}" >&2
      exit 1
    fi
  done
fi
if [[ ${#WORKLOAD_ROLLOUTS[@]} -gt 0 ]]; then
  wait_rollouts_parallel "${WORKLOAD_ROLLOUTS[@]}"
  CHANGED=1
fi

# ConfigMap-only changes need a restart when workload spec did not change.
if [[ "${POLICY_CM_CHANGED}" -eq 1 || "${SUPPLEMENTAL_CM_CHANGED}" -eq 1 ]]; then
  if [[ ${#WORKLOAD_ROLLOUTS[@]} -eq 0 ]]; then
    prereqs_phase_actor policies "Reload policy consumers"
    restart_deployments_parallel "${POLICY_DEPLOYMENTS[@]}"
    CHANGED=1
  else
    prereqs_log "skip extra policy restart — rollout already completed after combined patch"
  fi
fi

# --- hradmin: remove ACTIVITI_ADMIN so user-level policies apply (admin tests use processadminuser) ---
prereqs_phase_actor keycloak "Keycloak hradmin roles"
KC_STS="$(kubectl get statefulset -n "${NAMESPACE}" -o jsonpath='{range .items[*]}{.metadata.name}{"\n"}{end}' 2>/dev/null \
  | grep -E 'keycloak|platform-.*-ra$|-ra$' | head -1 || true)"

if [[ -n "${KC_STS}" ]] && kubectl get statefulset "${KC_STS}" -n "${NAMESPACE}" &>/dev/null; then
  prereqs_step "checking hradmin ACTIVITI_ADMIN on Keycloak (${KC_STS})"
  run_with_heartbeat keycloak "keycloak hradmin role check" kubectl exec -n "${NAMESPACE}" "${KC_STS}-0" -c keycloak -- bash -c '
    set -e
    /opt/jboss/keycloak/bin/kcadm.sh config credentials \
      --server http://localhost:8080/auth --realm master --user "${KEYCLOAK_USER}" --password "${KEYCLOAK_PASSWORD}" 2>/dev/null || exit 0
    HRADMIN_ID=$(/opt/jboss/keycloak/bin/kcadm.sh get users -r activiti -q username=hradmin --fields id --format csv --noquotes 2>/dev/null | tail -1)
    if [[ -z "${HRADMIN_ID}" || "${HRADMIN_ID}" == "id" ]]; then exit 0; fi
    if /opt/jboss/keycloak/bin/kcadm.sh get "users/${HRADMIN_ID}/role-mappings/realm" -r activiti 2>/dev/null | grep -q ACTIVITI_ADMIN; then
      echo "Removing ACTIVITI_ADMIN from hradmin..."
      ADMIN_ROLE_ID=$(/opt/jboss/keycloak/bin/kcadm.sh get roles/ACTIVITI_ADMIN -r activiti --fields id --format csv --noquotes | tail -1)
      /opt/jboss/keycloak/bin/kcadm.sh delete "users/${HRADMIN_ID}/role-mappings/realm" -r activiti \
        -b "[{\"id\":\"${ADMIN_ROLE_ID}\",\"name\":\"ACTIVITI_ADMIN\"}]" 2>/dev/null || true
    else
      echo "hradmin already without ACTIVITI_ADMIN"
    fi
  ' || echo "⚠️  Could not adjust hradmin roles (non-fatal)"
else
  echo "Skip Keycloak hradmin check (no Keycloak statefulset in ${NAMESPACE})"
fi

prereqs_set_actor coordinator
if [[ "${CHANGED}" -eq 0 ]]; then
  prereqs_log "✅ Cluster prerequisites already satisfied — no rollout required (speed run!)"
else
  prereqs_log "✅ Cluster prerequisites updated — mic drop"
fi
  prereqs_vibe coordinator

echo ""
echo "Next: npm run port-forward && npm run verify:process-catalog && npm run test:runtime"
