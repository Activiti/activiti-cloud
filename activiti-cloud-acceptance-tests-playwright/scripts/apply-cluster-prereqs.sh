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

# shellcheck source=/dev/null
set -a && source "${PKG_DIR}/.env" 2>/dev/null || true && set +a

prereqs_phase_actor coordinator "Cluster prerequisites"
prereqs_step "discovering preview namespace"

REQUESTED_NAMESPACE="${1:-${PREVIEW_NAME:-}}"
NAMESPACE="$(discover_preview_namespace "${REQUESTED_NAMESPACE}" || true)"
if [[ -z "${NAMESPACE}" ]]; then
  echo "Usage: $0 [kubernetes-namespace]"
  echo "Or set PREVIEW_NAME / ACCEPTANCE_ENV_NAME in activiti-cloud-acceptance-tests-playwright/.env"
  echo "First-time setup: npm run test:setup -- --install"
  exit 1
fi

prereqs_step "discovering Activiti deployments in ${NAMESPACE}"
if ! discover_acceptance_deployments "${NAMESPACE}"; then
  echo "ERROR: Activiti Cloud is not installed in namespace ${NAMESPACE}"
  echo "  (missing runtime-bundle / query / connector deployments)"
  echo ""
  echo "Install once, then re-run:"
  echo "  npm run test:setup -- --install"
  echo ""
  echo "Or manually:"
  echo "  ./scripts/local-install.sh -n \${ACCEPTANCE_ENV_NAME:-activiti-tests} -c \${CLUSTER_NAME:-activiti}"
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
  "${AUDIT_DEP}"
)

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
ACT_KEYCLOAK_URL="http://${IDENTITY_HOST}/auth"

resolve_runtime_bundle_image() {
  prereqs_set_actor registry
  if [[ -n "${ACCEPTANCE_RUNTIME_BUNDLE_IMAGE:-}" ]]; then
    prereqs_log "using ACCEPTANCE_RUNTIME_BUNDLE_IMAGE from env — no registry drama today"
    return
  fi
  local resolved_tag=""
  if [[ "${ACCEPTANCE_RUNTIME_BUNDLE_USE_RESOLVED_TAG:-true}" == "true" && -f "${ROOT_DIR}/scripts/resolve-docker-images.sh" ]]; then
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
SUPPLEMENTAL_PROCESSES_DIR="${PKG_DIR}/config/cluster/supplemental-processes"
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
  local ip host1 host2
  ip=$(kubectl get deployment "${dep}" -n "${NAMESPACE}" -o jsonpath='{.spec.template.spec.hostAliases[0].ip}' 2>/dev/null || true)
  host1=$(kubectl get deployment "${dep}" -n "${NAMESPACE}" -o jsonpath='{.spec.template.spec.hostAliases[0].hostnames[0]}' 2>/dev/null || true)
  host2=$(kubectl get deployment "${dep}" -n "${NAMESPACE}" -o jsonpath='{.spec.template.spec.hostAliases[0].hostnames[1]}' 2>/dev/null || true)
  [[ "${ip}" == "${TRAEFIK_IP}" && "${host1}" == "${IDENTITY_HOST}" && "${host2}" == "${GATEWAY_HOST}" ]]
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

apply_host_alias_patches_for_dep() {
  local dep=$1
  if ! deployment_has_host_alias "${dep}"; then
    kubectl patch deployment "${dep}" -n "${NAMESPACE}" --type=json -p="${HOST_ALIASES_JSON}" 2>/dev/null || true
  fi
  local current_kc_url
  current_kc_url="$(deployment_env_value "${dep}" "ACT_KEYCLOAK_URL")"
  if [[ "${current_kc_url}" != "${ACT_KEYCLOAK_URL}" ]]; then
    kubectl set env deployment/"${dep}" -n "${NAMESPACE}" "ACT_KEYCLOAK_URL=${ACT_KEYCLOAK_URL}"
  fi
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

# --- hostAliases + ACT_KEYCLOAK_URL (parallel patch, parallel rollout wait) ---
prereqs_phase_actor traefik "Host aliases and Keycloak URL"
HOST_ALIASES_JSON="[{\"op\":\"add\",\"path\":\"/spec/template/spec/hostAliases\",\"value\":[{\"ip\":\"${TRAEFIK_IP}\",\"hostnames\":[\"${IDENTITY_HOST}\",\"${GATEWAY_HOST}\"]}]}]"

HOST_ALIAS_ROLLOUTS=()
HOST_ALIAS_PIDS=()
for dep in "${HOST_ALIAS_DEPLOYMENTS[@]}"; do
  prereqs_set_actor "$(prereqs_actor_for_dep "${dep}")"
  prereqs_step "checking hostAliases + ACT_KEYCLOAK_URL on ${dep}"
  if ! deployment_exists "${dep}"; then
    echo "Skip missing deployment: ${dep}"
    continue
  fi

  NEEDS_HOST_PATCH=0
  if ! deployment_has_host_alias "${dep}"; then
    NEEDS_HOST_PATCH=1
  fi
  CURRENT_KC_URL="$(deployment_env_value "${dep}" "ACT_KEYCLOAK_URL")"
  if [[ "${CURRENT_KC_URL}" != "${ACT_KEYCLOAK_URL}" ]]; then
    NEEDS_HOST_PATCH=1
  fi

  if [[ "${NEEDS_HOST_PATCH}" -eq 1 ]]; then
    prereqs_log "queueing ${dep} hostAliases + ACT_KEYCLOAK_URL"
    apply_host_alias_patches_for_dep "${dep}" &
    HOST_ALIAS_PIDS+=($!)
    HOST_ALIAS_ROLLOUTS+=("${dep}")
  else
    echo "✓ ${dep} hostAliases and ACT_KEYCLOAK_URL already configured"
  fi
done
for pid in "${HOST_ALIAS_PIDS[@]:-}"; do
  wait "${pid}" || true
done
if [[ ${#HOST_ALIAS_ROLLOUTS[@]} -gt 0 ]]; then
  wait_rollouts_parallel "${HOST_ALIAS_ROLLOUTS[@]}"
  CHANGED=1
fi

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

prereqs_phase_actor policies "Policy mounts and runtime env"
POLICY_RESTART_NEEDED=0
POLICY_ROLLOUTS=()
POLICY_PIDS=()
for dep in "${POLICY_DEPLOYMENTS[@]}"; do
  prereqs_set_actor "$(prereqs_actor_for_dep "${dep}")"
  if ! deployment_exists "${dep}"; then
    prereqs_log "skip missing policy deployment: ${dep} (ghost ship)"
    continue
  fi

  prereqs_step "checking policy mounts on ${dep}"
  if policy_dep_needs_patch "${dep}"; then
    if [[ "${dep}" == "${RB_DEP}" ]]; then
      if [[ "${NEEDS_SUPPLEMENTAL_PROCESSES}" -eq 1 ]]; then
        echo "Queueing ${dep} acceptance policies + supplemental processes..."
      else
        echo "Queueing ${dep} acceptance policies (classpath catalog only)..."
        if deployment_has_supplemental_process_mount "${dep}"; then
          strip_supplemental_mount_from_rb
        fi
      fi
    else
      echo "Queueing security policies on ${dep}..."
    fi
    apply_policy_patches_for_dep "${dep}" &
    POLICY_PIDS+=($!)
    POLICY_ROLLOUTS+=("${dep}")
    POLICY_RESTART_NEEDED=1
  else
    echo "✓ ${dep} acceptance mounts and env already configured"
  fi
done
for pid in "${POLICY_PIDS[@]:-}"; do
  wait "${pid}" || true
done
if [[ ${#POLICY_ROLLOUTS[@]} -gt 0 ]]; then
  wait_rollouts_parallel "${POLICY_ROLLOUTS[@]}"
  CHANGED=1
fi

if [[ "${POLICY_CM_CHANGED}" -eq 1 || "${POLICY_RESTART_NEEDED}" -eq 1 || "${SUPPLEMENTAL_CM_CHANGED}" -eq 1 ]]; then
  prereqs_phase_actor policies "Reload policy consumers"
  restart_deployments_parallel "${POLICY_DEPLOYMENTS[@]}"
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
