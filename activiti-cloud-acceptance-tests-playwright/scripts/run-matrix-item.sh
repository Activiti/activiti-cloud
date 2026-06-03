#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

NAME=""
CLUSTER=""
BROKER="rabbitmq"
PARTITIONING="non-partitioned"
DESTINATIONS_OPTION="default-destinations"
INSTALL_ARGS=()

show_help() {
  cat <<'EOF'
Run one CI matrix item locally (install + Playwright).

USAGE:
  ./activiti-cloud-acceptance-tests-playwright/scripts/run-matrix-item.sh [options]

OPTIONS:
  -n, --name <name>                 Environment name (required), e.g. michal-test
  -c, --cluster <cluster>           Cluster name (optional, passed to local-install.sh)
  -b, --broker <rabbitmq|kafka>     Messaging broker (default: rabbitmq)
  -p, --partitioning <mode>         partitioned|non-partitioned|prefix (default: non-partitioned)
  -d, --destinations-option <opt>   default-destinations|override-destinations|pdb (default: default-destinations)
  --                                Everything after this is forwarded to scripts/local-install.sh

EXAMPLES:
  ./activiti-cloud-acceptance-tests-playwright/scripts/run-matrix-item.sh -n local-dev \
    -b rabbitmq -p non-partitioned -d default-destinations

  ./activiti-cloud-acceptance-tests-playwright/scripts/run-matrix-item.sh -n kafka-ovr \
    -b kafka -p partitioned -d override-destinations
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    -n|--name)
      NAME="$2"
      shift 2
      ;;
    -c|--cluster)
      CLUSTER="$2"
      shift 2
      ;;
    -b|--broker)
      BROKER="$2"
      shift 2
      ;;
    -p|--partitioning)
      PARTITIONING="$2"
      shift 2
      ;;
    -d|--destinations-option)
      DESTINATIONS_OPTION="$2"
      shift 2
      ;;
    --help|-h)
      show_help
      exit 0
      ;;
    --)
      shift
      INSTALL_ARGS+=("$@")
      break
      ;;
    *)
      echo "Unknown argument: $1" >&2
      echo "Run with --help to see supported options." >&2
      exit 2
      ;;
  esac
done

if [[ -z "${NAME}" ]]; then
  echo "Error: --name is required." >&2
  exit 2
fi

cd "${ROOT_DIR}"

install_cmd=( "./scripts/local-install.sh"
  --name "${NAME}"
  --broker "${BROKER}"
  --partitioning "${PARTITIONING}"
  --destinations-option "${DESTINATIONS_OPTION}"
)

if [[ -n "${CLUSTER}" ]]; then
  install_cmd+=( --cluster "${CLUSTER}" )
fi

if [[ ${#INSTALL_ARGS[@]} -gt 0 ]]; then
  install_cmd+=( "${INSTALL_ARGS[@]}" )
fi

echo "==> Installing preview: broker=${BROKER} partitioning=${PARTITIONING} destinations=${DESTINATIONS_OPTION}"
"${install_cmd[@]}"

echo "==> Running Playwright"
npm run test

ENV_FILE="${ROOT_DIR}/activiti-cloud-acceptance-tests-playwright/.env"
if [[ -f "${ENV_FILE}" ]]; then
  # shellcheck disable=SC1090
  set -a
  source "${ENV_FILE}"
  set +a
fi

# global-teardown also prints this; repeat here after matrix install+test
if [[ -n "${PREVIEW_NAME:-}" ]]; then
  echo ""
  echo "Preview still installed: PREVIEW_NAME=${PREVIEW_NAME}"
  echo "  cd ${ROOT_DIR} && PREVIEW_NAME=${PREVIEW_NAME} make delete"
  echo "  npm run preview:delete"
fi
