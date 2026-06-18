#!/usr/bin/env bash
# Keycloak hradmin role adjustment for security policy tests.

prereqs_fix_hradmin_roles() {
  prereqs_phase_actor keycloak "Keycloak hradmin roles"
  local kc_sts
  kc_sts="$(kubectl get statefulset -n "${NAMESPACE}" -o jsonpath='{range .items[*]}{.metadata.name}{"\n"}{end}' 2>/dev/null \
    | grep -E 'keycloak|platform-.*-ra$|-ra$' | head -1 || true)"

  if [[ -n "${kc_sts}" ]] && kubectl get statefulset "${kc_sts}" -n "${NAMESPACE}" &>/dev/null; then
    prereqs_step "checking hradmin ACTIVITI_ADMIN on Keycloak (${kc_sts})"
    run_with_heartbeat keycloak "keycloak hradmin role check" kubectl exec -n "${NAMESPACE}" "${kc_sts}-0" -c keycloak -- bash -c '
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
}

prereqs_finish() {
  prereqs_set_actor coordinator
  if [[ "${PREREQS_CHANGED:-0}" -eq 0 ]]; then
    prereqs_log "✅ Cluster prerequisites already satisfied — no rollout required (speed run!)"
  else
    prereqs_log "✅ Cluster prerequisites updated — mic drop"
  fi
  prereqs_vibe coordinator
  echo ""
  echo "Next: npm run port-forward && npm run verify:process-catalog && npm run test:runtime"
}
