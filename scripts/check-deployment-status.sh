#!/bin/bash

# Deployment status checker for Activiti Cloud
# Checks pod status and service availability

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Default values
PREVIEW_NAME=""

print_usage() {
    echo "Usage: $0 -p <preview_name>"
    echo ""
    echo "Options:"
    echo "  -p, --preview-name    Preview name (e.g., 123 for 123-rabbit-n-d)"
    echo "  -h, --help           Show this help message"
}

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -p|--preview-name)
            PREVIEW_NAME="$2"
            shift 2
            ;;
        -h|--help)
            print_usage
            exit 0
            ;;
        *)
            echo "Unknown option: $1" >&2
            print_usage
            exit 1
            ;;
    esac
done

if [ -z "$PREVIEW_NAME" ]; then
    echo -e "${RED}Error: Preview name is required${NC}" >&2
    print_usage
    exit 1
fi

# Generate namespace
NAMESPACE="pr-${PREVIEW_NAME}-rabbit-n-d"

# Auto-detect cluster information from kubectl context
if command -v kubectl >/dev/null 2>&1 && kubectl config current-context >/dev/null 2>&1; then
    CURRENT_CONTEXT=$(kubectl config current-context)

    # Map context names to proper cluster names
    case "$CURRENT_CONTEXT" in
        "activiti-hackathon")
            CLUSTER_NAME="activiti-hackathon"
            ;;
        "activiti-community")
            CLUSTER_NAME="activiti-community"
            ;;
        *rancher*)
            CLUSTER_NAME="activiti"
            ;;
        *)
            CLUSTER_NAME="${CURRENT_CONTEXT}"
            ;;
    esac
else
    # Default cluster name when kubectl is not available
    CLUSTER_NAME="activiti"
fi

CLUSTER_DOMAIN="envalfresco.com"
GATEWAY_HOST="gateway-${NAMESPACE}.${CLUSTER_NAME}.${CLUSTER_DOMAIN}"
SSO_HOST="identity-${NAMESPACE}.${CLUSTER_NAME}.${CLUSTER_DOMAIN}"

echo -e "${BLUE}=== Activiti Cloud Deployment Status Check ===${NC}"
echo "Namespace: $NAMESPACE"
echo "Gateway: https://$GATEWAY_HOST"
echo "SSO: https://$SSO_HOST"
echo ""

# Check if namespace exists
if ! kubectl get namespace "$NAMESPACE" >/dev/null 2>&1; then
    echo -e "${RED}Error: Namespace '$NAMESPACE' not found${NC}" >&2
    exit 1
fi

echo -e "${BLUE}=== Pod Status ===${NC}"
kubectl get pods -n "$NAMESPACE" --no-headers | while read -r line; do
    pod_name=$(echo "$line" | awk '{print $1}')
    ready=$(echo "$line" | awk '{print $2}')
    status=$(echo "$line" | awk '{print $3}')

    if [ "$status" = "Running" ] && [[ "$ready" == *"/"* ]]; then
        ready_count=$(echo "$ready" | cut -d'/' -f1)
        total_count=$(echo "$ready" | cut -d'/' -f2)
        if [ "$ready_count" = "$total_count" ]; then
            echo -e "  ${GREEN}✓${NC} $pod_name ($ready)"
        else
            echo -e "  ${YELLOW}⚠${NC} $pod_name ($ready) - Not all containers ready"
        fi
    elif [ "$status" = "Running" ]; then
        echo -e "  ${GREEN}✓${NC} $pod_name"
    else
        echo -e "  ${RED}✗${NC} $pod_name - Status: $status"
    fi
done

echo ""
echo -e "${BLUE}=== Image Information ===${NC}"
kubectl get pods -n "$NAMESPACE" -o jsonpath='{range .items[*]}{.metadata.name}{"\n"}{range .spec.containers[*]}{.name}: {.image}{"\n"}{end}{"\n"}{end}' | while read -r line; do
    if [[ "$line" =~ ^pr-.*-.*$ ]]; then
        echo -e "${YELLOW}Pod: $line${NC}"
    elif [[ "$line" =~ activiti ]]; then
        echo -e "  ${GREEN}$line${NC}"
    elif [ -n "$line" ]; then
        echo "  $line"
    fi
done

echo ""
echo -e "${BLUE}=== Useful Commands ===${NC}"
echo "View logs:"
echo "  kubectl logs -f deployment/pr-${PREVIEW_NAME}-rabbit-n-d-runtime-bundle -n $NAMESPACE"
echo "  kubectl logs -f deployment/pr-${PREVIEW_NAME}-rabbit-n-d-activiti-cloud-query -n $NAMESPACE"
echo ""
echo "Scale services:"
echo "  kubectl scale deployment pr-${PREVIEW_NAME}-rabbit-n-d-runtime-bundle --replicas=2 -n $NAMESPACE"
echo ""
echo "Delete deployment:"
echo "  kubectl delete namespace $NAMESPACE"
echo "  # or use: make delete PREVIEW_NAME=$NAMESPACE"
echo ""
echo -e "${GREEN}=== Status Check Complete ===${NC}"
