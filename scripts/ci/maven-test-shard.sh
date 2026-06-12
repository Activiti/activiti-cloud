#!/usr/bin/env bash
# Run Maven verify for one CI test shard (see .github/ci/maven-shards.json).
#
# Required env:
#   MAVEN_TEST_MODULES — comma-separated -pl list when MAVEN_TEST_ROOTS is empty
#   MAVEN_TEST_ROOTS   — comma-separated reactor roots for verify -f (may be empty)
#   MAVEN_TEST_ROOT_OPTIONS — JSON map of per-root overrides (threads, unitTestsParallel)
# Optional env:
#   MAVEN_CLI_OPTS — passed through to mvn

set -euo pipefail

: "${MAVEN_TEST_MODULES:=}"
: "${MAVEN_TEST_ROOTS:=}"
: "${MAVEN_TEST_ROOT_OPTIONS:={}}"
: "${MAVEN_CLI_OPTS:=}"

resolve_mvn_threads() {
  local root="$1"
  jq -r --arg root "${root}" '.[$root].threads // "1C"' <<< "${MAVEN_TEST_ROOT_OPTIONS}"
}

resolve_unit_tests_parallel() {
  local root="$1"
  if jq -e --arg root "${root}" '.[$root].unitTestsParallel != null' <<< "${MAVEN_TEST_ROOT_OPTIONS}" >/dev/null; then
    jq -r --arg root "${root}" '.[$root].unitTestsParallel' <<< "${MAVEN_TEST_ROOT_OPTIONS}"
  else
    echo "true"
  fi
}

if [[ -n "${MAVEN_TEST_ROOTS}" ]]; then
  IFS=',' read -ra ROOTS <<< "${MAVEN_TEST_ROOTS}"
  for root in "${ROOTS[@]}"; do
    mvn_threads="$(resolve_mvn_threads "${root}")"
    unit_tests_parallel="$(resolve_unit_tests_parallel "${root}")"
    MVN_TEST_FLAGS=(-U "-DunitTests.parallel=${unit_tests_parallel}")
    echo "::group::verify -f ${root}/pom.xml (-T ${mvn_threads}, unitTests.parallel=${unit_tests_parallel})"
    mvn -f "${root}/pom.xml" verify -T "${mvn_threads}" \
      "${MVN_TEST_FLAGS[@]}" ${MAVEN_CLI_OPTS}
    echo "::endgroup::"
  done
else
  MVN_TEST_FLAGS=(-U -DunitTests.parallel=true)
  mvn verify -pl "${MAVEN_TEST_MODULES}" -am -T 1C \
    "${MVN_TEST_FLAGS[@]}" ${MAVEN_CLI_OPTS}
fi
