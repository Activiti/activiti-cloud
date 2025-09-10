#!/bin/bash

# Source PREVIEW_NAME environment variables
# Usage: source ./scripts/set-preview-env.sh [options]
# Same options as generate-preview-name.sh

# Get the directory where this script is located
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Generate the environment variables
ENV_VARS=$("$SCRIPT_DIR/generate-preview-name.sh" "$@" 2>/dev/null | grep "^export" || true)

if [[ -z "$ENV_VARS" ]]; then
    echo "Error: Failed to generate environment variables" >&2
    return 1 2>/dev/null || exit 1
fi

# Export the variables
eval "$ENV_VARS"

echo "✅ Environment variables set successfully!"
echo "Current PREVIEW_NAME: $PREVIEW_NAME"
