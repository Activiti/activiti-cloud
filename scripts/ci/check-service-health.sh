#!/usr/bin/env bash
# Poll an HTTPS health endpoint until it responds or timeout.

set -euo pipefail

HEALTH_URL="${HEALTH_URL:-${1:-}}"
MAX_ATTEMPTS="${MAX_ATTEMPTS:-${2:-50}}"
POLL_INTERVAL="${POLL_INTERVAL:-${3:-5}}"

if [[ -z "${HEALTH_URL}" ]]; then
  echo "Usage: HEALTH_URL=https://host/path $0" >&2
  echo "   or: $0 <health-url> [max-attempts] [poll-interval-sec]" >&2
  exit 2
fi

wait_until_true() {
  local attempt_counter=0
  until "$@"; do
    if [[ ${attempt_counter} -eq ${MAX_ATTEMPTS} ]]; then
      echo "Max attempts reached, cannot connect to ${HEALTH_URL}"
      exit 1
    fi
    printf '.'
    attempt_counter=$((attempt_counter + 1))
    sleep "${POLL_INTERVAL}"
  done
}

check_services_up() {
  curl --silent --head --fail "${HEALTH_URL}" > /dev/null 2>&1
}

echo "Waiting for service to be up: ${HEALTH_URL}"
wait_until_true check_services_up
echo ""
echo "Service is up and running!"
