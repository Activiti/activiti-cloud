#!/usr/bin/env bash
# Runtime bundle image resolution and rollout.

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

read_runtime_bundle_image_from_values() {
  local values_file=$1
  local repo tag
  if [[ ! -f "${values_file}" ]]; then
    return 1
  fi
  if command -v yq &>/dev/null; then
    repo="$(yq e '.runtime-bundle.image.repository // "activiti/example-runtime-bundle"' "${values_file}" 2>/dev/null || true)"
    tag="$(yq e '.runtime-bundle.image.tag' "${values_file}" 2>/dev/null || true)"
  else
    repo="activiti/example-runtime-bundle"
    tag="$(read_runtime_bundle_tag_from_values "${values_file}")"
  fi
  if [[ -n "${tag}" && "${tag}" != "null" ]]; then
    ACCEPTANCE_RUNTIME_BUNDLE_IMAGE="${repo}:${tag}"
  fi
}

resolve_runtime_bundle_image() {
  prereqs_set_actor registry
  if [[ -n "${ACCEPTANCE_RUNTIME_BUNDLE_IMAGE:-}" ]]; then
    prereqs_log "using ACCEPTANCE_RUNTIME_BUNDLE_IMAGE from env — no registry drama today"
    return
  fi
  local resolved_tag=""
  local chart_values="${ROOT_DIR}/.git/activiti-cloud-full-chart/charts/activiti-cloud-full-example/values.yaml"

  if [[ "${ACCEPTANCE_CI_USE_CHART_IMAGE_TAGS:-}" == "true" && -f "${chart_values}" ]]; then
    prereqs_log "CI chart tags — runtime-bundle image from Helm chart values (no registry resolve)"
    read_runtime_bundle_image_from_values "${chart_values}"
    if [[ -n "${ACCEPTANCE_RUNTIME_BUNDLE_IMAGE:-}" ]]; then
      return
    fi
  fi

  if [[ -n "${VERSION:-}" && ( "${GITHUB_ACTIONS:-}" == "true" || "${CI:-}" == "true" ) ]]; then
    ACCEPTANCE_RUNTIME_BUNDLE_IMAGE="activiti/example-runtime-bundle:${VERSION}"
    prereqs_log "CI build VERSION — runtime-bundle image ${ACCEPTANCE_RUNTIME_BUNDLE_IMAGE} (no registry resolve)"
    return
  fi

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
  export ACCEPTANCE_RUNTIME_BUNDLE_IMAGE
}

prereqs_apply_runtime_bundle_image() {
  prereqs_phase_actor registry "Resolve runtime bundle image"
  resolve_runtime_bundle_image
}

prereqs_rollout_runtime_bundle_image() {
  prereqs_phase_actor runtime-bundle "Runtime bundle image"
  if ! workload_exists "${RB_DEP}"; then
    return 0
  fi

  prereqs_step "checking ${RB_DEP} container image"
  local rb_kind current_image container
  rb_kind="$(workload_kind "${RB_DEP}")"
  current_image="$(kubectl get "${rb_kind}" "${RB_DEP}" -n "${NAMESPACE}" -o jsonpath='{.spec.template.spec.containers[0].image}')"
  if [[ "${current_image}" != "${ACCEPTANCE_RUNTIME_BUNDLE_IMAGE}" ]]; then
    prereqs_log "updating ${RB_DEP} image to ${ACCEPTANCE_RUNTIME_BUNDLE_IMAGE} (was: ${current_image})"
    container="$(deployment_container_name "${RB_DEP}")"
    kubectl set image "${rb_kind}/${RB_DEP}" -n "${NAMESPACE}" "${container}=${ACCEPTANCE_RUNTIME_BUNDLE_IMAGE}"
    wait_rollout_one "${RB_DEP}" 300 || return 1
    PREREQS_CHANGED=1
  else
    prereqs_log "✓ ${RB_DEP} image already ${current_image}"
  fi
}

prereqs_verify_connector_present() {
  if ! workload_exists "${CONNECTOR_DEP}"; then
    echo "ERROR: Missing workload ${CONNECTOR_DEP} — install chart with example-cloud-connector"
    return 1
  fi
  echo "✓ ${CONNECTOR_DEP} present"
}
