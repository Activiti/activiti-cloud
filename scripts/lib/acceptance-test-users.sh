#!/usr/bin/env bash
# Preview Keycloak seeded users — CI reads GitHub vars/secrets; local uses .env or chart defaults.

export_acceptance_test_user_env() {
  export TESTUSER_USERNAME="${TESTUSER_USERNAME:-testuser}"
  export HRUSER_USERNAME="${HRUSER_USERNAME:-hruser}"
  export HRADMIN_USERNAME="${HRADMIN_USERNAME:-hradmin}"
  export PROCESSADMINUSER_USERNAME="${PROCESSADMINUSER_USERNAME:-processadminuser}"
  export TESTADMIN_USERNAME="${TESTADMIN_USERNAME:-testadmin}"

  if [[ "${GITHUB_ACTIONS:-}" == "true" ]]; then
    local missing=()
    [[ -z "${TESTUSER_PASSWORD:-}" ]] && missing+=(ACCEPTANCE_TESTUSER_PASSWORD)
    [[ -z "${HRUSER_PASSWORD:-}" ]] && missing+=(ACCEPTANCE_HRUSER_PASSWORD)
    [[ -z "${HRADMIN_PASSWORD:-}" ]] && missing+=(ACCEPTANCE_HRADMIN_PASSWORD)
    [[ -z "${PROCESSADMINUSER_PASSWORD:-}" ]] && missing+=(ACCEPTANCE_PROCESSADMINUSER_PASSWORD)
    [[ -z "${TESTADMIN_PASSWORD:-}" ]] && missing+=(ACCEPTANCE_TESTADMIN_PASSWORD)
    if [[ ${#missing[@]} -gt 0 ]]; then
      echo "::error::Missing repo secrets: ${missing[*]} (bundled preview Keycloak users)" >&2
      exit 1
    fi
  else
    export TESTUSER_PASSWORD="${TESTUSER_PASSWORD:-password}"
    export HRUSER_PASSWORD="${HRUSER_PASSWORD:-password}"
    export HRADMIN_PASSWORD="${HRADMIN_PASSWORD:-password}"
    export PROCESSADMINUSER_PASSWORD="${PROCESSADMINUSER_PASSWORD:-password}"
    export TESTADMIN_PASSWORD="${TESTADMIN_PASSWORD:-password}"
  fi
}
