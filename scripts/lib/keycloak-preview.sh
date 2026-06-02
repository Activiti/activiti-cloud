#!/usr/bin/env bash
# Keycloak bundled with activiti-cloud-full-chart (per preview namespace).

preview_global_gateway_domain() {
  local cluster_name="${1:-${CLUSTER_NAME:-activiti}}"
  local cluster_domain="${2:-${CLUSTER_DOMAIN:-envalfresco.com}}"
  echo "${cluster_name}.${cluster_domain}"
}

preview_identity_auth_url() {
  local preview_name=$1
  local global_domain
  global_domain="$(preview_global_gateway_domain)"
  echo "http://identity-${preview_name}.${global_domain}/auth"
}

preview_sso_token_url() {
  local preview_name=$1
  local global_domain
  global_domain="$(preview_global_gateway_domain)"
  local realm="${2:-activiti}"
  echo "https://identity-${preview_name}.${global_domain}/auth/realms/${realm}/protocol/openid-connect/token"
}

read_preview_keycloak_client_secret() {
  local namespace=$1
  kubectl get secret activiti-keycloak-client -n "${namespace}" \
    -o jsonpath='{.data.clientSecret}' 2>/dev/null | base64 -d
}

wait_for_preview_keycloak_client_secret() {
  local namespace=$1
  local attempt

  for attempt in $(seq 1 60); do
    local secret
    secret="$(read_preview_keycloak_client_secret "${namespace}")"
    if [[ -n "${secret}" ]]; then
      echo "${secret}"
      return 0
    fi
    echo "  Waiting for activiti-keycloak-client secret in ${namespace} (${attempt}/60)..."
    sleep 5
  done

  return 1
}

configure_preview_keycloak_post_install() {
  local preview_name=$1
  local global_domain
  global_domain="$(preview_global_gateway_domain)"

  KEYCLOAK_REALM="${KEYCLOAK_REALM:-activiti}"
  KEYCLOAK_CLIENT_ID="${KEYCLOAK_CLIENT_ID:-activiti}"
  KEYCLOAK_URL="$(preview_identity_auth_url "${preview_name}" "${global_domain}")"

  echo -e "${YELLOW}Reading Keycloak client secret from preview namespace ${preview_name}...${NC}"
  KEYCLOAK_CLIENT_SECRET="$(wait_for_preview_keycloak_client_secret "${preview_name}")" || {
    echo -e "${RED}activiti-keycloak-client secret not found in ${preview_name}${NC}" >&2
    echo "Check: kubectl get pods -n ${preview_name} | grep -i keycloak" >&2
    return 1
  }

  echo -e "${GREEN}✓ Using bundled preview Keycloak (realm ${KEYCLOAK_REALM}, client ${KEYCLOAK_CLIENT_ID})${NC}"
  export KEYCLOAK_URL KEYCLOAK_REALM KEYCLOAK_CLIENT_ID KEYCLOAK_CLIENT_SECRET
}
