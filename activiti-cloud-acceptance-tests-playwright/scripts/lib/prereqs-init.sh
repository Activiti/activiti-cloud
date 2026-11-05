#!/usr/bin/env bash
# Namespace discovery, gateway/Keycloak env, deployment lists for acceptance prereqs.

prereqs_load_local_dotenv() {
  local pkg_dir=$1
  local cli_namespace="${2:-}"

  if [[ "${GITHUB_ACTIONS:-}" != "true" && "${CI:-}" != "true" && -f "${pkg_dir}/.env" ]]; then
    # shellcheck source=/dev/null
    set -a && source "${pkg_dir}/.env" && set +a
    if [[ -n "${cli_namespace}" ]]; then
      PREVIEW_NAME="${cli_namespace}"
      export PREVIEW_NAME
    fi
  fi
}

prereqs_discover_namespace() {
  local requested_namespace=$1

  prereqs_phase_actor coordinator "Cluster prerequisites"
  prereqs_step "discovering preview namespace"
  NAMESPACE="$(discover_preview_namespace "${requested_namespace}" || true)"
  if [[ -z "${NAMESPACE}" ]]; then
    echo "Usage: apply-cluster-prereqs.sh [kubernetes-namespace]"
    echo "Or set PREVIEW_NAME / ACCEPTANCE_ENV_NAME in activiti-cloud-acceptance-tests-playwright/.env"
    echo "First-time setup: npm run test:setup -- --install"
    return 1
  fi
  export NAMESPACE
}

prereqs_discover_deployments() {
  prereqs_step "discovering Activiti deployments in ${NAMESPACE}"
  if [[ "${GITHUB_ACTIONS:-}" == "true" || "${CI:-}" == "true" ]]; then
    wait_for_acceptance_deployments "${NAMESPACE}" || return 1
  elif ! discover_acceptance_deployments "${NAMESPACE}"; then
    echo "ERROR: Activiti Cloud is not installed in namespace ${NAMESPACE}"
    echo "  (missing runtime-bundle / query / connector deployments)"
    echo ""
    echo "Install once, then re-run:"
    echo "  npm run test:setup -- --install"
    echo ""
    echo "Or manually:"
    echo "  ./scripts/local-install.sh -n \${ACCEPTANCE_ENV_NAME:-\$(whoami)-local} -c \${CLUSTER_NAME:-activiti}"
    return 1
  fi
}

prereqs_setup_deployment_lists() {
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
  if [[ -n "${AUDIT_DEP:-}" ]]; then
    POLICY_DEPLOYMENTS+=("${AUDIT_DEP}")
  fi
}

prereqs_setup_gateway_and_keycloak() {
  GATEWAY_HOST="${GATEWAY_HOST:-}"
  if [[ -z "${GATEWAY_HOST}" && -n "${PREVIEW_NAME:-}" && -n "${CLUSTER_NAME:-}" ]]; then
    DOMAIN="${CLUSTER_DOMAIN:-envalfresco.com}"
    GATEWAY_HOST="gateway-${PREVIEW_NAME}.${CLUSTER_NAME}.${DOMAIN}"
  fi

  if [[ -z "${GATEWAY_HOST}" ]]; then
    echo "Set GATEWAY_HOST or PREVIEW_NAME + CLUSTER_NAME in .env"
    return 1
  fi

  GATEWAY_HOST="${GATEWAY_HOST%%:*}"
  IDENTITY_HOST="identity-${GATEWAY_HOST#gateway-}"
  export GATEWAY_HOST IDENTITY_HOST

  local pf_ns="${PORT_FORWARD_NAMESPACE:-traefik}"
  local pf_svc="${PORT_FORWARD_SERVICE:-traefik}"
  prereqs_set_actor traefik
  prereqs_step "asking Traefik for clusterIP (${pf_svc} in ${pf_ns})"
  TRAEFIK_IP="$(kubectl get svc "${pf_svc}" -n "${pf_ns}" -o jsonpath='{.spec.clusterIP}')"
  prereqs_log "Traefik clusterIP: ${TRAEFIK_IP}"
  export TRAEFIK_IP

  ACT_KEYCLOAK_URL="http://${IDENTITY_HOST}/auth"
  JWT_JWK_SET_URI=""
  KEYCLOAK_CLIENT_TOKEN_URI=""
  if [[ "${GITHUB_ACTIONS:-}" == "true" || "${CI:-}" == "true" ]]; then
    ACT_KEYCLOAK_URL="https://${IDENTITY_HOST}/auth"
    JWT_JWK_SET_URI="http://${IDENTITY_HOST}/auth/realms/${KEYCLOAK_REALM:-activiti}/protocol/openid-connect/certs"
    KEYCLOAK_CLIENT_TOKEN_URI="http://${IDENTITY_HOST}/auth/realms/${KEYCLOAK_REALM:-activiti}/protocol/openid-connect/token"
  fi
  export ACT_KEYCLOAK_URL JWT_JWK_SET_URI KEYCLOAK_CLIENT_TOKEN_URI
}

prereqs_setup_paths_and_flags() {
  local pkg_dir=$1

  POLICY_FILE="${pkg_dir}/config/cluster/acceptance-security-policies.properties"
  POLICY_CONFIG="optional:file:/config/acceptance/acceptance-security-policies.properties"
  CONFIGMAP_NAME="acceptance-security-policies"
  SUPPLEMENTAL_PROCESSES_DIR="${pkg_dir}/resources/modeling-projects/acceptance"
  SUPPLEMENTAL_CONFIGMAP="acceptance-supplemental-processes"
  PROCESS_LOCATION_CLASSPATH="classpath*:/processes/"
  PROCESS_LOCATION_SUPPLEMENTAL="file:/config/acceptance-supplemental-processes/"

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
  export NEEDS_SUPPLEMENTAL_PROCESSES
}

prereqs_log_configuration() {
  prereqs_set_actor coordinator
  prereqs_log "Namespace:              ${NAMESPACE}"
  prereqs_set_actor runtime-bundle && prereqs_log "Runtime bundle deploy:  ${RB_DEP}"
  prereqs_set_actor query && prereqs_log "Query deploy:           ${QUERY_DEP}"
  prereqs_set_actor connector && prereqs_log "Connector deploy:       ${CONNECTOR_DEP}"
  prereqs_set_actor traefik && prereqs_log "Gateway host:           ${GATEWAY_HOST}"
  prereqs_set_actor identity && prereqs_log "Identity host:          ${IDENTITY_HOST} → ${ACT_KEYCLOAK_URL}"
  if [[ -n "${JWT_JWK_SET_URI}" ]]; then
    prereqs_log "JWT JWKS (in-cluster):  ${JWT_JWK_SET_URI}"
    prereqs_log "Client token (in-cluster): ${KEYCLOAK_CLIENT_TOKEN_URI}"
  fi
  prereqs_set_actor runtime-bundle && prereqs_log "Runtime bundle image:   ${ACCEPTANCE_RUNTIME_BUNDLE_IMAGE}"
  if [[ "${NEEDS_SUPPLEMENTAL_PROCESSES}" -eq 1 ]]; then
    prereqs_set_actor policies && prereqs_log "Supplemental BPMN:      enabled"
  else
    prereqs_set_actor runtime-bundle && prereqs_log "Supplemental BPMN:      skipped — full catalog on classpath"
  fi
}

prereqs_bootstrap() {
  local requested_namespace=$1
  local pkg_dir=$2

  prereqs_load_local_dotenv "${pkg_dir}" "${requested_namespace}"
  prereqs_discover_namespace "${requested_namespace}" || return 1
  prereqs_discover_deployments || return 1
  prereqs_setup_deployment_lists
  prereqs_setup_gateway_and_keycloak || return 1
}
