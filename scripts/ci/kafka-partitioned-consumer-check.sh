#!/usr/bin/env bash
# Kafka partitioned profile: 1 query consumer pod, 4 engineEvents topic partitions.

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
# shellcheck source=../lib/preview-env.sh
source "${ROOT_DIR}/scripts/lib/preview-env.sh"

: "${PREVIEW_NAME:?PREVIEW_NAME is required}"

export_preview_gateway_env
export QUERY_CONSUMER_NUMBER=2

check_result_is() {
  if [[ "${RESULT}" == "$1" ]]; then
    echo "correct"
  else
    echo "number expected is $1 but result is ${RESULT}"
    exit 1
  fi
}

count_query_consumer_pods() {
  RESULT="$(kubectl -n "${PREVIEW_NAME}" get pods -o \
    'custom-columns=POD:metadata.name,READY-true:status.containerStatuses[*].ready' | \
    grep -e 'activiti-cloud-query.*true' | wc -l | xargs)"
  export RESULT
}

count_query_topic_partitions() {
  RESULT="$(kubectl exec -t -n "${PREVIEW_NAME}" kafka-0 \
    -c kafka "--" sh -c $'/opt/bitnami/kafka/bin/kafka-topics.sh \
    --bootstrap-server=localhost:9092 --describe \
    --topic engineEvents | grep PartitionCount |\
    awk \'{ printf "%s",$6 }\'')"
  export RESULT
}

echo "It checks the deployment has 1 query pod consumer"
count_query_consumer_pods
check_result_is 1

echo "It checks the deployment has 4 query topic partitions"
count_query_topic_partitions
check_result_is 4
