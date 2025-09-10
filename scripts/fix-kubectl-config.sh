#!/bin/bash

# Rancher kubectl config generator
# Creates a proper kubectl config that both kubectl and helm can use

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}=== Generating kubectl config from Rancher ===${NC}"

# Check if rancher CLI is available
if ! command -v rancher >/dev/null 2>&1; then
    echo -e "${RED}Error: rancher CLI not found${NC}" >&2
    echo -e "${YELLOW}Please install it: brew install rancher-cli${NC}" >&2
    exit 1
fi

# Check current rancher context
RANCHER_CONTEXT=$(rancher context current 2>/dev/null || echo "none")
if [[ "$RANCHER_CONTEXT" == "none" ]] || [[ ! "$RANCHER_CONTEXT" =~ "activiti-hackathon" ]]; then
    echo -e "${RED}Error: rancher not connected to activiti-hackathon cluster${NC}" >&2
    echo -e "${YELLOW}Please run: rancher context switch${NC}" >&2
    echo -e "${YELLOW}And select the activiti-hackathon cluster${NC}" >&2
    exit 1
fi

echo -e "${GREEN}✓ Rancher connected to: $RANCHER_CONTEXT${NC}"

# Backup existing kubectl config
if [ -f ~/.kube/config ]; then
    BACKUP_FILE="$HOME/.kube/config.backup.$(date +%Y%m%d_%H%M%S)"
    echo -e "${YELLOW}Backing up existing kubectl config to: $BACKUP_FILE${NC}"
    cp ~/.kube/config "$BACKUP_FILE"
fi

# Create .kube directory if it doesn't exist
mkdir -p ~/.kube

# Generate kubectl config using rancher clusters kubeconfig command
echo -e "${BLUE}Generating kubectl config from Rancher...${NC}"

# Get the activiti-hackathon cluster ID
CLUSTER_ID=$(rancher clusters ls --format '{{.Cluster.ID}},{{.Cluster.Name}}' | grep activiti-hackathon | cut -d',' -f1)

if [ -z "$CLUSTER_ID" ]; then
    echo -e "${RED}✗ Could not find activiti-hackathon cluster${NC}" >&2
    exit 1
fi

echo -e "${YELLOW}Using cluster ID: $CLUSTER_ID${NC}"

# Generate the kubeconfig
if rancher clusters kubeconfig "$CLUSTER_ID" > ~/.kube/config; then
    echo -e "${GREEN}✓ kubectl config generated successfully${NC}"

    # Test the configuration
    if kubectl cluster-info >/dev/null 2>&1; then
        echo -e "${GREEN}✓ kubectl can now connect to cluster${NC}"

        # Test namespace access
        if kubectl get namespaces >/dev/null 2>&1; then
            echo -e "${GREEN}✓ kubectl can access cluster resources${NC}"
        else
            echo -e "${YELLOW}⚠️  kubectl connected but cannot access namespaces${NC}"
            echo -e "${YELLOW}This might be a permissions issue${NC}"
        fi

        # Test helm connectivity
        if helm version --short >/dev/null 2>&1; then
            echo -e "${GREEN}✓ helm can now connect to cluster${NC}"
            echo -e "${GREEN}=== Configuration Complete! ===${NC}"
            echo -e "${YELLOW}You can now run: ./scripts/local-install.sh -p 123${NC}"
        else
            echo -e "${YELLOW}⚠️  helm connectivity test failed, but kubectl is working${NC}"
            echo -e "${YELLOW}This might be normal - helm will work during installation${NC}"
        fi
    else
        echo -e "${RED}✗ kubectl still cannot connect${NC}" >&2
        exit 1
    fi
else
    echo -e "${RED}✗ Failed to generate kubectl config from Rancher${NC}" >&2
    exit 1
fi

echo ""
echo -e "${BLUE}Current cluster info:${NC}"
kubectl cluster-info || true
