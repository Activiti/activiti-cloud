#!/usr/bin/env bash
# Merge docker-scan-image-dirs metadata with .github/ci/docker-image-services.json.
# Output: workflow matrix JSON for GitHub Actions strategy.include.
#
# Usage (in CI after docker-scan-image-dirs):
#   DIRS_AS_JSON='${{ steps.scan.outputs.image-dirs-with-metadata }}' \
#     scripts/ci/transform-docker-image-matrix.sh
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
CONFIG="${ROOT_DIR}/.github/ci/docker-image-services.json"

if [[ -z "${DIRS_AS_JSON:-}" ]]; then
  echo "::error::DIRS_AS_JSON is required (image-dirs-with-metadata from docker-scan-image-dirs)" >&2
  exit 1
fi

if [[ ! -f "${CONFIG}" ]]; then
  echo "::error::Missing ${CONFIG}" >&2
  exit 1
fi

transformed="$(jq -c --slurpfile cfg "${CONFIG}" '
  $cfg[0] as $services |
  map(
    . as $entry |
    ($services[$entry.path] // {}) as $svc |
    $entry
    | .["short-name"] = ($svc.shortName // ($entry.path | sub(".*/"; "")))
    | .["extra-modules"] = (
        if ($svc.extraModules // "") == "" then ""
        else ($svc.extraModules + ",")
        end
      )
    | .["test-maven-flags"] = ($svc.testMavenFlags // "-T 1C -DunitTests.parallel=true")
  )
' <<< "${DIRS_AS_JSON}")"

if [[ -n "${GITHUB_OUTPUT:-}" ]]; then
  echo "transformed-json=${transformed}" >> "${GITHUB_OUTPUT}"
fi

echo "${transformed}" | jq .
