#!/usr/bin/env bash
# Ensure workflow matrix id lists stay in sync with .github/ci/*.json keys.
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "${ROOT_DIR}"

json_keys() {
  local file="$1"
  jq -r 'keys[]' "${file}" | sort
}

yaml_shard_ids() {
  local file="$1"
  grep -E '^[[:space:]]+shard-id:' "${file}" | head -1 \
    | sed -E 's/.*\[(.*)\].*/\1/' \
    | tr ',' '\n' \
    | sed 's/^[[:space:]]*//;s/[[:space:]]*$//' \
    | sort
}

yaml_profile_ids() {
  local file="$1"
  awk '
    /^[[:space:]]+matrix:/ { in_matrix=1; next }
    in_matrix && /^[[:space:]]+profile:[[:space:]]*$/ { in_profiles=1; next }
    in_matrix && /^[[:space:]]+include:[[:space:]]*$/ { in_include=1; next }
    in_profiles && /^[[:space:]]+- (rabbitmq|kafka)-[a-z0-9-]+$/ {
      sub(/^[[:space:]]+- /, "")
      print
      next
    }
    in_include && /^[[:space:]]+-[[:space:]]+profile:[[:space:]]*(rabbitmq|kafka)-[a-z0-9-]+$/ {
      sub(/^[[:space:]]+-[[:space:]]+profile:[[:space:]]*/, "")
      print
      next
    }
    in_profiles && /^[[:space:]]+steps:/ { exit }
    in_include && /^[[:space:]]+steps:/ { exit }
    in_profiles && /^[^[:space:]]/ { exit }
    in_include && /^[^[:space:]]/ { exit }
  ' "${file}" | sort
}

assert_same_lists() {
  local label="$1"
  local expected_file="$2"
  local actual_file="$3"
  local actual_extractor="$4"

  local expected_tmp actual_tmp
  expected_tmp="$(mktemp)"
  actual_tmp="$(mktemp)"

  json_keys "${expected_file}" > "${expected_tmp}"
  "${actual_extractor}" "${actual_file}" > "${actual_tmp}"

  if ! diff -u "${expected_tmp}" "${actual_tmp}" >/dev/null; then
    echo "::error::${label}: matrix list in ${actual_file} does not match keys in ${expected_file}" >&2
    echo "Expected ($(basename "${expected_file}")):" >&2
    sed 's/^/  /' "${expected_tmp}" >&2
    echo "Found in YAML:" >&2
    sed 's/^/  /' "${actual_tmp}" >&2
    rm -f "${expected_tmp}" "${actual_tmp}"
    return 1
  fi

  rm -f "${expected_tmp}" "${actual_tmp}"
}

errors=0

assert_same_lists \
  "Maven build shards" \
  ".github/ci/maven-shards.json" \
  ".github/workflows/_reusable-maven-build.yml" \
  yaml_shard_ids || errors=$((errors + 1))

assert_same_lists \
  "Maven test shards" \
  ".github/ci/maven-shards.json" \
  ".github/workflows/_reusable-maven-test.yml" \
  yaml_shard_ids || errors=$((errors + 1))

assert_same_lists \
  "Playwright profiles (reusable)" \
  ".github/ci/playwright-profiles.json" \
  ".github/workflows/_reusable-playwright-tests.yml" \
  yaml_profile_ids || errors=$((errors + 1))

assert_same_lists \
  "Playwright profiles (pr-closed)" \
  ".github/ci/playwright-profiles.json" \
  ".github/workflows/pr-closed.yaml" \
  yaml_profile_ids || errors=$((errors + 1))

if (( errors > 0 )); then
  exit 1
fi

echo "CI matrix lists match JSON config."
