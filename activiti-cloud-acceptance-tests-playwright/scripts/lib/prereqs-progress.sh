#!/usr/bin/env bash
# Live progress helpers for apply-cluster-prereqs.sh — colored service personas + vibes.

PREREQS_START_TS=$(date +%s)
ACCEPTANCE_PREREQS_HEARTBEAT_SEC="${ACCEPTANCE_PREREQS_HEARTBEAT_SEC:-10}"
PREREQS_ACTOR="${PREREQS_ACTOR:-coordinator}"
PREREQS_VIBE_TICK=0

# ANSI (disabled when not a TTY or NO_COLOR is set).
if [[ -t 1 && -z "${NO_COLOR:-}" ]]; then
  PREREQS_USE_COLOR=1
else
  PREREQS_USE_COLOR=0
fi
readonly PREREQS_RESET=$'\033[0m'
readonly PREREQS_DIM=$'\033[2m'
readonly PREREQS_BOLD=$'\033[1m'

prereqs_elapsed() {
  echo "$(( $(date +%s) - PREREQS_START_TS ))s"
}

prereqs_color_for_actor() {
  case "$1" in
    coordinator) echo $'\033[36m' ;;      # cyan
    registry) echo $'\033[33m' ;;         # yellow
    runtime-bundle) echo $'\033[32m' ;;   # green
    traefik) echo $'\033[34m' ;;          # blue
    identity) echo $'\033[35m' ;;         # magenta
    query) echo $'\033[96m' ;;            # bright cyan
    connector) echo $'\033[92m' ;;        # bright green
    audit) echo $'\033[37m' ;;            # white
    keycloak) echo $'\033[91m' ;;         # bright red
    policies) echo $'\033[95m' ;;         # bright magenta
    *) echo $'\033[90m' ;;                # gray
  esac
}

# Two spaces after emoji so the label reads clearly in all terminals.
prereqs_actor_label() {
  case "$1" in
    coordinator) echo "🧭  Discovery Squad" ;;
    registry) echo "📦  Registry Gremlin" ;;
    runtime-bundle) echo "⚙️  Runtime-Bundle" ;;
    traefik) echo "🌐  Traefik DJ" ;;
    identity) echo "🔐  Identity-Adapter" ;;
    query) echo "🔍  Query-Service" ;;
    connector) echo "🔌  Cloud-Connector" ;;
    audit) echo "📋  Audit-Service" ;;
    keycloak) echo "👑  Keycloak Realm" ;;
    policies) echo "📜  Policy Goblins" ;;
    *) echo "☕  Prereqs Coordinator" ;;
  esac
}

prereqs_set_actor() {
  PREREQS_ACTOR="$1"
}

prereqs_actor_for_dep() {
  local dep=$1
  case "$dep" in
    *runtime-bundle*) echo "runtime-bundle" ;;
    *query*) echo "query" ;;
    *identity*) echo "identity" ;;
    *connector*) echo "connector" ;;
    *audit*) echo "audit" ;;
    *) echo "coordinator" ;;
  esac
}

prereqs_vibe_line() {
  local actor=$1
  local lines=()
  case "$actor" in
    coordinator)
      lines=(
        "Scanning the cluster like it's a Where's Wally? poster…"
        "If the namespace isn't here, it didn't leave a forwarding address."
        "kubectl and I are having a moment. A long moment."
      )
      ;;
    registry)
      lines=(
        "Negotiating with Docker Hub. They drive a hard bargain."
        "Downloading tags… spiritually, if not literally yet."
        "The gremlin promises this image exists. Probably."
      )
      ;;
    runtime-bundle)
      lines=(
        "Runtime-Bundle is rehearsing its BPMN solo."
        "Still rolling out — pods need their beauty sleep."
        "HeadersConnectorProcess sends regards. Eventually."
      )
      ;;
    traefik)
      lines=(
        "Traefik is mixing hostnames into the perfect cocktail."
        "Routing vibes only — no bad Gateway 502s on my watch."
        "hostAliases incoming. DNS, but make it fashion."
      )
      ;;
    identity)
      lines=(
        "Identity-Adapter is syncing with Keycloak. Very professional. Very slow."
        "Almost there — tokens don't mint themselves."
        "SSO patience is a virtue. So is coffee."
      )
      ;;
    query)
      lines=(
        "Query-Service is indexing tasks you haven't created yet."
        "Read models take time. Blame eventual consistency."
        "Still warming up — LIKE queries demand respect."
      )
      ;;
    connector)
      lines=(
        "Cloud-Connector is stretching before the acceptance marathon."
        "Connectors gonna connect. Rollouts gonna roll."
        "If this takes long, it's building character."
      )
      ;;
    audit)
      lines=(
        "Audit-Service is filing paperwork in triplicate."
        "Every event will be judged. Fairly. Slowly."
        "The audit trail cometh."
      )
      ;;
    keycloak)
      lines=(
        "Keycloak is debating hradmin's admin privileges. Drama!"
        "Realm activiti is having a committee meeting."
        "kcadm.sh walks into a bar… still running."
      )
      ;;
    policies)
      lines=(
        "Policy Goblins are stapling security rules to ConfigMaps."
        "Mounting acceptance-security-policies.properties with great ceremony."
        "Serenity parity doesn't happen by accident. Or quickly."
      )
      ;;
    *)
      lines=("Still working. The cluster is thinking very hard.")
      ;;
  esac
  local count=${#lines[@]}
  [[ "${count}" -eq 0 ]] && return 0
  local idx=$(( (PREREQS_VIBE_TICK + RANDOM) % count ))
  PREREQS_VIBE_TICK=$((PREREQS_VIBE_TICK + 1))
  echo "${lines[$idx]}"
}

prereqs_vibe() {
  local actor="${1:-${PREREQS_ACTOR}}"
  local line
  line="$(prereqs_vibe_line "${actor}")"
  [[ -n "${line}" ]] && prereqs_log "${line}"
}

prereqs_log() {
  local tag time elapsed
  tag="$(prereqs_actor_label "${PREREQS_ACTOR}")"
  time="$(date '+%H:%M:%S')"
  elapsed="$(prereqs_elapsed)"

  if [[ "${PREREQS_USE_COLOR}" -eq 1 ]]; then
    local color
    color="$(prereqs_color_for_actor "${PREREQS_ACTOR}")"
    printf '%s[%s +%s]%s %s[%s]%s %s\n' \
      "${PREREQS_DIM}" "${time}" "${elapsed}" "${PREREQS_RESET}" \
      "${color}${PREREQS_BOLD}" "${tag}" "${PREREQS_RESET}" \
      "$*"
  else
    printf '[%s +%s] [%s] %s\n' "${time}" "${elapsed}" "${tag}" "$*"
  fi
}

prereqs_phase_actor() {
  prereqs_set_actor "$1"
  shift
  echo ""
  prereqs_log "━━ $* ━━"
  prereqs_vibe "${PREREQS_ACTOR}"
}

prereqs_phase() {
  prereqs_phase_actor "${PREREQS_ACTOR}" "$@"
}

prereqs_step() {
  prereqs_log "→ $*"
}

run_with_heartbeat() {
  local actor="${PREREQS_ACTOR}"
  if [[ $# -ge 2 && "$1" != *" "* && "$2" != -* ]]; then
    case "$1" in
      coordinator|registry|runtime-bundle|traefik|identity|query|connector|audit|keycloak|policies)
        actor="$1"
        shift
        prereqs_set_actor "${actor}"
        ;;
    esac
  fi
  local label=$1
  shift
  "$@" &
  local cmd_pid=$!
  local waited=0
  while kill -0 "${cmd_pid}" 2>/dev/null; do
    sleep "${ACCEPTANCE_PREREQS_HEARTBEAT_SEC}"
    waited=$((waited + ACCEPTANCE_PREREQS_HEARTBEAT_SEC))
    prereqs_log "… ${label} (${waited}s — still running)"
    prereqs_vibe "${actor}"
  done
  wait "${cmd_pid}"
}

wait_rollout_one() {
  local dep=$1
  local timeout=${2:-${ACCEPTANCE_ROLLOUT_TIMEOUT_SEC:-180}}
  local kind="deployment"
  if declare -f workload_kind &>/dev/null; then
    kind="$(workload_kind "${dep}")"
  elif kubectl get statefulset "${dep}" -n "${NAMESPACE}" &>/dev/null; then
    kind="statefulset"
  fi
  [[ -z "${kind}" ]] && kind="deployment"
  prereqs_set_actor "$(prereqs_actor_for_dep "${dep}")"
  prereqs_step "rollout ${kind}/${dep} (timeout ${timeout}s)"
  kubectl rollout status "${kind}/${dep}" -n "${NAMESPACE}" --timeout="${timeout}s" 2>&1 | while IFS= read -r line; do
    prereqs_log "│ ${line}"
  done
  return "${PIPESTATUS[0]}"
}

wait_rollouts_parallel() {
  local deps=("$@")
  [[ ${#deps[@]} -eq 0 ]] && return 0
  prereqs_step "waiting for ${#deps[@]} rollout(s) in parallel: ${deps[*]}"
  prereqs_vibe "${PREREQS_ACTOR}"
  local pids=() pid failed=0
  for dep in "${deps[@]}"; do
    local rollout_timeout="${ACCEPTANCE_ROLLOUT_TIMEOUT_SEC:-180}"
    if declare -f workload_kind &>/dev/null; then
      local kind
      kind="$(workload_kind "${dep}")"
      if [[ "${kind}" == "statefulset" ]]; then
        rollout_timeout="${ACCEPTANCE_STATEFULSET_ROLLOUT_TIMEOUT_SEC:-${ACCEPTANCE_ROLLOUT_TIMEOUT_SEC:-420}}"
      fi
    fi
    wait_rollout_one "${dep}" "${rollout_timeout}" &
    pids+=($!)
  done
  for pid in "${pids[@]}"; do
    wait "${pid}" || failed=1
  done
  return "${failed}"
}

restart_deployments_parallel() {
  local deps=("$@")
  local restart_deps=() dep
  local pids=() pid failed=0

  for dep in "${deps[@]}"; do
    if declare -f workload_exists &>/dev/null && workload_exists "${dep}"; then
      local kind
      kind="$(workload_kind "${dep}")"
      prereqs_set_actor "$(prereqs_actor_for_dep "${dep}")"
      prereqs_step "restarting ${kind}/${dep} — pods go brrr"
      kubectl rollout restart "${kind}/${dep}" -n "${NAMESPACE}" &
      pids+=($!)
      restart_deps+=("${dep}")
    elif declare -f deployment_exists &>/dev/null && deployment_exists "${dep}"; then
      prereqs_set_actor "$(prereqs_actor_for_dep "${dep}")"
      prereqs_step "restarting deployment/${dep} — pods go brrr"
      kubectl rollout restart deployment/"${dep}" -n "${NAMESPACE}" &
      pids+=($!)
      restart_deps+=("${dep}")
    fi
  done
  for pid in "${pids[@]}"; do
    wait "${pid}" || failed=1
  done
  if [[ ${#restart_deps[@]} -gt 0 ]]; then
    wait_rollouts_parallel "${restart_deps[@]}" || failed=1
  fi
  return "${failed}"
}
