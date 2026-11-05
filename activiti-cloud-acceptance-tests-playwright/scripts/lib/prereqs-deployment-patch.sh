#!/usr/bin/env bash
# Combined deployment patches: hostAliases, Keycloak env, policy mounts.

resolve_keycloak_patch_for_deployment() {
  local dep=$1
  PATCH_KEYCLOAK_URL="${ACT_KEYCLOAK_URL}"
  PATCH_JWT_ISSUER_URI=""
  PATCH_JWT_JWK_SET_URI="${JWT_JWK_SET_URI}"
  PATCH_KEYCLOAK_CLIENT_TOKEN_URI="${KEYCLOAK_CLIENT_TOKEN_URI}"

  if [[ -n "${JWT_JWK_SET_URI}" && "${dep}" == "${IDENTITY_DEP}" ]]; then
    PATCH_KEYCLOAK_URL="http://${IDENTITY_HOST}/auth"
    PATCH_JWT_ISSUER_URI="https://${IDENTITY_HOST}/auth/realms/${KEYCLOAK_REALM:-activiti}"
  fi
  export PATCH_KEYCLOAK_URL PATCH_JWT_ISSUER_URI PATCH_JWT_JWK_SET_URI PATCH_KEYCLOAK_CLIENT_TOKEN_URI
}

deployment_needs_host_patch() {
  local dep=$1
  if ! deployment_has_host_alias "${dep}"; then
    return 0
  fi
  resolve_keycloak_patch_for_deployment "${dep}"
  local current_kc_url current_kc_realm current_issuer current_jwks current_client_token
  current_kc_url="$(deployment_env_value "${dep}" "ACT_KEYCLOAK_URL")"
  current_kc_realm="$(deployment_env_value "${dep}" "ACT_KEYCLOAK_REALM")"
  [[ "${current_kc_url}" != "${PATCH_KEYCLOAK_URL}" ]] && return 0
  [[ "${current_kc_realm}" != "${KEYCLOAK_REALM:-activiti}" ]] && return 0
  if [[ -n "${PATCH_JWT_JWK_SET_URI}" ]]; then
    current_jwks="$(deployment_env_value "${dep}" "SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWK_SET_URI")"
    current_client_token="$(deployment_env_value "${dep}" "SPRING_SECURITY_OAUTH2_CLIENT_PROVIDER_KEYCLOAK_TOKEN_URI")"
    [[ "${current_jwks}" != "${PATCH_JWT_JWK_SET_URI}" ]] && return 0
    [[ "${current_client_token}" != "${PATCH_KEYCLOAK_CLIENT_TOKEN_URI}" ]] && return 0
  fi
  if [[ -n "${PATCH_JWT_ISSUER_URI}" ]]; then
    current_issuer="$(deployment_env_value "${dep}" "SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI")"
    [[ "${current_issuer}" != "${PATCH_JWT_ISSUER_URI}" ]] && return 0
  fi
  return 1
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

apply_acceptance_deployment_patch() {
  local dep=$1
  local include_policy=$2
  local include_host=$3
  local patch_json result
  resolve_keycloak_patch_for_deployment "${dep}"
  export NAMESPACE RB_DEP ACT_KEYCLOAK_URL POLICY_CONFIG NEEDS_SUPPLEMENTAL_PROCESSES
  export PROCESS_LOCATION_CLASSPATH PROCESS_LOCATION_SUPPLEMENTAL
  export DEP_NAME="${dep}" KEYCLOAK_REALM="${KEYCLOAK_REALM:-activiti}"
  export DEP_KIND="$(workload_kind "${dep}")"
  export INCLUDE_POLICY="${include_policy}" INCLUDE_HOST="${include_host}"
  export HOST_ALIASES="[{\"ip\":\"${TRAEFIK_IP}\",\"hostnames\":[\"${IDENTITY_HOST}\",\"${GATEWAY_HOST}\"]}]"
  export PATCH_KEYCLOAK_URL PATCH_JWT_ISSUER_URI PATCH_JWT_JWK_SET_URI PATCH_KEYCLOAK_CLIENT_TOKEN_URI
  patch_json="$(
    python3 -c "
import json, os
patch = {
    'namespace': os.environ['NAMESPACE'],
    'deployment': os.environ['DEP_NAME'],
    'workloadKind': os.environ.get('DEP_KIND', 'deployment'),
}
if os.environ.get('INCLUDE_HOST') == '1':
    patch['hostAliases'] = json.loads(os.environ['HOST_ALIASES'])
    patch['keycloakUrl'] = os.environ['PATCH_KEYCLOAK_URL']
    patch['keycloakRealm'] = os.environ['KEYCLOAK_REALM']
    if os.environ.get('PATCH_JWT_ISSUER_URI'):
        patch['jwtIssuerUri'] = os.environ['PATCH_JWT_ISSUER_URI']
    if os.environ.get('PATCH_JWT_JWK_SET_URI'):
        patch['jwtJwkSetUri'] = os.environ['PATCH_JWT_JWK_SET_URI']
    if os.environ.get('PATCH_KEYCLOAK_CLIENT_TOKEN_URI'):
        patch['keycloakClientTokenUri'] = os.environ['PATCH_KEYCLOAK_CLIENT_TOKEN_URI']
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

prereqs_patch_workloads() {
  local workload_rollouts=() workload_pids=() workload_dep_names=() workload_deps=()
  local dep include_host include_policy needs_patch patch_status pid i

  prereqs_phase_actor coordinator "Acceptance workload configuration"
  for dep in "${HOST_ALIAS_DEPLOYMENTS[@]}" "${POLICY_DEPLOYMENTS[@]}"; do
    [[ -n "${dep}" ]] || continue
    if ((${#workload_deps[@]} > 0)) && dep_in_list "${dep}" "${workload_deps[@]}"; then
      continue
    fi
    workload_deps+=("${dep}")
  done

  for dep in "${workload_deps[@]}"; do
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
    workload_pids+=($!)
    workload_dep_names+=("${dep}")
  done

  if ((${#workload_pids[@]} > 0)); then
    for i in "${!workload_pids[@]}"; do
      pid="${workload_pids[$i]}"
      dep="${workload_dep_names[$i]}"
      patch_status=0
      wait "${pid}" || patch_status=$?
      if [[ "${patch_status}" -eq 0 ]]; then
        workload_rollouts+=("${dep}")
      elif [[ "${patch_status}" -ne 2 ]]; then
        echo "ERROR: failed to patch ${dep}" >&2
        return 1
      fi
    done
  fi

  if [[ ${#workload_rollouts[@]} -gt 0 ]]; then
    wait_rollouts_parallel "${workload_rollouts[@]}"
    PREREQS_CHANGED=1
  fi

  if [[ "${POLICY_CM_CHANGED:-0}" -eq 1 || "${SUPPLEMENTAL_CM_CHANGED:-0}" -eq 1 ]]; then
    if [[ ${#workload_rollouts[@]} -eq 0 ]]; then
      prereqs_phase_actor policies "Reload policy consumers"
      restart_deployments_parallel "${POLICY_DEPLOYMENTS[@]}"
      PREREQS_CHANGED=1
    else
      prereqs_log "skip extra policy restart — rollout already completed after combined patch"
    fi
  fi
}
