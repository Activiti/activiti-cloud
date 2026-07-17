#!/usr/bin/env bash
# Fail when a PR merge-base with develop is older than the allowed age.
# Ensures acceptance-tested PRs are rebased onto recent develop before merge.
set -euo pipefail

MAX_AGE_SECONDS="${PR_DEVELOP_MAX_AGE_SECONDS:-86400}"
BASE_REF="${PR_DEVELOP_BASE_REF:-develop}"

git fetch --no-tags origin "${BASE_REF}"

MERGE_BASE="$(git merge-base HEAD "origin/${BASE_REF}")"
MERGE_BASE_EPOCH="$(git show -s --format=%ct "${MERGE_BASE}")"
NOW_EPOCH="$(date -u +%s)"
AGE=$((NOW_EPOCH - MERGE_BASE_EPOCH))

if [[ "${AGE}" -gt "${MAX_AGE_SECONDS}" ]]; then
  MAX_HOURS=$((MAX_AGE_SECONDS / 3600))
  AGE_HOURS=$((AGE / 3600))
  echo "::error::This pull request must be rebased onto ${BASE_REF}. Merge-base with ${BASE_REF} is ${AGE_HOURS}h old (max ${MAX_HOURS}h). Run: git fetch origin ${BASE_REF} && git rebase origin/${BASE_REF} && git push --force-with-lease"
  exit 1
fi

echo "PR merge-base with ${BASE_REF} is ${AGE}s old (within ${MAX_AGE_SECONDS}s limit)."
