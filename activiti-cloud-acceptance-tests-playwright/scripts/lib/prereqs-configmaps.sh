#!/usr/bin/env bash
# Acceptance security policy and supplemental BPMN ConfigMaps.

prereqs_apply_configmaps() {
  POLICY_CM_CHANGED=0
  SUPPLEMENTAL_CM_CHANGED=0

  prereqs_phase_actor policies "Security policies ConfigMaps"
  prereqs_step "checking ${CONFIGMAP_NAME} ConfigMap"
  local new_policy_hash old_policy_hash
  new_policy_hash="$(file_sha256 "${POLICY_FILE}")"
  old_policy_hash="$(kubectl get configmap "${CONFIGMAP_NAME}" -n "${NAMESPACE}" -o jsonpath='{.metadata.annotations.acceptance-policy-sha256}' 2>/dev/null || true)"

  if [[ "${new_policy_hash}" != "${old_policy_hash}" ]]; then
    echo "Applying acceptance security policies ConfigMap (content changed)..."
    kubectl create configmap "${CONFIGMAP_NAME}" \
      --from-file=acceptance-security-policies.properties="${POLICY_FILE}" \
      -n "${NAMESPACE}" \
      --dry-run=client -o yaml | kubectl apply -f -
    kubectl annotate configmap "${CONFIGMAP_NAME}" -n "${NAMESPACE}" \
      "acceptance-policy-sha256=${new_policy_hash}" --overwrite
    POLICY_CM_CHANGED=1
    PREREQS_CHANGED=1
  else
    echo "✓ ${CONFIGMAP_NAME} ConfigMap already up to date"
  fi

  if [[ -d "${SUPPLEMENTAL_PROCESSES_DIR}" ]]; then
    local new_supplemental_hash old_supplemental_hash
    new_supplemental_hash="$(dir_sha256 "${SUPPLEMENTAL_PROCESSES_DIR}")"
    old_supplemental_hash="$(kubectl get configmap "${SUPPLEMENTAL_CONFIGMAP}" -n "${NAMESPACE}" -o jsonpath='{.metadata.annotations.acceptance-supplemental-sha256}' 2>/dev/null || true)"

    if [[ "${new_supplemental_hash}" != "${old_supplemental_hash}" ]]; then
      echo "Applying supplemental acceptance processes ConfigMap..."
      kubectl create configmap "${SUPPLEMENTAL_CONFIGMAP}" \
        --from-file="${SUPPLEMENTAL_PROCESSES_DIR}" \
        -n "${NAMESPACE}" \
        --dry-run=client -o yaml | kubectl apply -f -
      kubectl annotate configmap "${SUPPLEMENTAL_CONFIGMAP}" -n "${NAMESPACE}" \
        "acceptance-supplemental-sha256=${new_supplemental_hash}" --overwrite
      SUPPLEMENTAL_CM_CHANGED=1
      PREREQS_CHANGED=1
    else
      echo "✓ ${SUPPLEMENTAL_CONFIGMAP} ConfigMap already up to date"
    fi
  fi
}
