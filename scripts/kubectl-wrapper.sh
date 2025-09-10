#!/bin/bash

# kubectl wrapper that uses rancher kubectl for activiti-hackathon cluster
# This ensures we have proper authentication to the cluster

# Check if rancher CLI is available
if ! command -v rancher >/dev/null 2>&1; then
    echo "Error: rancher CLI not found. Please install it first:" >&2
    echo "  brew install rancher-cli" >&2
    exit 1
fi

# Check if rancher is connected to activiti-hackathon
RANCHER_CONTEXT=$(rancher context current 2>/dev/null)
if [[ ! "$RANCHER_CONTEXT" =~ "activiti-hackathon" ]]; then
    echo "Error: rancher not connected to activiti-hackathon cluster" >&2
    echo "Current context: $RANCHER_CONTEXT" >&2
    echo "Please run: rancher context switch" >&2
    exit 1
fi

# Execute kubectl command through rancher
exec rancher kubectl "$@"
