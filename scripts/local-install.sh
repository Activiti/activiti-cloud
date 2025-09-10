#!/bin/bash

# Local Activiti Cloud Installation Script
# Replicates the GitHub Actions "Install release" functionality for local development
#
# Usage: ./scripts/local-install.sh [options]
#
# Options:
#   -p, --pr <number>           PR number (e.g., 123)
#   -r, --run <number>          GitHub run number (e.g., 456789)
#   -b, --broker <broker>       Messaging broker: rabbitmq|kafka (default: rabbitmq)
#   -pt, --partitioned <bool>   Partitioned: true|false (default: false)
#   -d, --destinations <type>   Destinations: default|override (default: default)
#   -v, --version <version>     Version to use (default: auto-generated)
#   --dry-run                   Show what would be executed without running
#   -h, --help                  Show this help message
#
# Prerequisites:
#   - kubectl configured and connected to cluster
#   - helm installed
#   - yq installed
#   - Access to activiti-cloud-full-chart repository

set -e

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Default values
PR_NUMBER=""
RUN_NUMBER=""
MESSAGING_BROKER="rabbitmq"
MESSAGING_PARTITIONED="false"
MESSAGING_DESTINATIONS="default"
VERSION=""
DRY_RUN=false
USE_LOCAL_IMAGES=false
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(dirname "$SCRIPT_DIR")"

# Help function
show_help() {
    cat << EOF
Local Activiti Cloud Installation Script

Replicates the GitHub Actions "Install release" functionality for local development.

USAGE:
    $0 [OPTIONS]

OPTIONS:
    -p, --pr <number>           PR number (e.g., 123)
    -r, --run <number>          GitHub run number (e.g., 456789)
    -b, --broker <broker>       Messaging broker: rabbitmq|kafka (default: rabbitmq)
    -pt, --partitioned <bool>   Partitioned: true|false (default: false)
    -d, --destinations <type>   Destinations: default|override (default: default)
    -v, --version <version>     Version to use (default: auto-generated)
    --use-local-images          Use local-values.yaml with available image tags
    --dry-run                   Show what would be executed without running
    -h, --help                  Show this help message

EXAMPLES:
    $0 -p 123                                    # Basic PR 123 with defaults
    $0 -p 456 -b kafka -pt true -d override     # Full configuration
    $0 --dry-run -p 123                         # See what would happen
    $0 -r 789012 -v 1.0.0-SNAPSHOT             # Custom version

PREREQUISITES:
    - kubectl configured and connected to cluster
    - helm installed (version 3+)
    - yq installed (for YAML processing)
    - python3 available (for version parsing)
    - Access to activiti-cloud-full-chart repository

NOTES:
    - Either --pr or --run must be specified
    - Script checks for required tools before execution
    - Uses same logic as GitHub Actions workflow
    - Creates/updates Kubernetes namespace with PREVIEW_NAME
EOF
}

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -p|--pr)
            PR_NUMBER="$2"
            shift 2
            ;;
        -r|--run)
            RUN_NUMBER="$2"
            shift 2
            ;;
        -b|--broker)
            MESSAGING_BROKER="$2"
            shift 2
            ;;
        -pt|--partitioned)
            MESSAGING_PARTITIONED="$2"
            shift 2
            ;;
        -d|--destinations)
            MESSAGING_DESTINATIONS="$2"
            shift 2
            ;;
        -v|--version)
            VERSION="$2"
            shift 2
            ;;
        --dry-run)
            DRY_RUN=true
            shift
            ;;
        --use-local-images)
            USE_LOCAL_IMAGES=true
            shift
            ;;
        -h|--help)
            show_help
            exit 0
            ;;
        *)
            echo -e "${RED}Error: Unknown option $1${NC}" >&2
            show_help
            exit 1
            ;;
    esac
done

# Function to run commands with dry-run support
execute_command() {
    local cmd="$1"
    local description="$2"

    if [[ "$DRY_RUN" == "true" ]]; then
        echo -e "${CYAN}[DRY-RUN] ${description}${NC}"
        echo -e "${YELLOW}Command: ${cmd}${NC}"
    else
        echo -e "${BLUE}${description}${NC}"
        echo -e "${YELLOW}Running: ${cmd}${NC}"
        eval "$cmd"
    fi
}

# Function to check prerequisites
check_prerequisites() {
    echo -e "${BLUE}=== Checking Prerequisites ===${NC}"

    local missing_tools=()

    # Check kubectl
    if ! command -v kubectl &> /dev/null; then
        missing_tools+=("kubectl")
    else
        echo -e "${GREEN}✓ kubectl found${NC}"
    fi

    # Check helm
    if ! command -v helm &> /dev/null; then
        missing_tools+=("helm")
    else
        echo -e "${GREEN}✓ helm found${NC}"
    fi

    # Check yq
    if ! command -v yq &> /dev/null; then
        missing_tools+=("yq")
    else
        echo -e "${GREEN}✓ yq found${NC}"
    fi

    # Check python3
    if ! command -v python3 &> /dev/null; then
        missing_tools+=("python3")
    else
        echo -e "${GREEN}✓ python3 found${NC}"
    fi

    # Check git
    if ! command -v git &> /dev/null; then
        missing_tools+=("git")
    else
        echo -e "${GREEN}✓ git found${NC}"
    fi

    if [[ ${#missing_tools[@]} -gt 0 ]]; then
        echo -e "${RED}Error: Missing required tools: ${missing_tools[*]}${NC}" >&2
        echo -e "${YELLOW}Please install the missing tools and try again.${NC}" >&2
        exit 1
    fi

    # Check kubectl cluster connection
    KUBECTL_CMD="kubectl"

    if ! kubectl cluster-info &> /dev/null; then
        echo -e "${RED}Error: kubectl not connected to a cluster${NC}" >&2
        echo ""
        echo -e "${YELLOW}🔧 Quick Setup Options:${NC}"
        echo ""
        echo -e "${CYAN}1️⃣  Generate kubectl config from Rancher:${NC}"
        echo -e "   ./scripts/fix-kubectl-config.sh"
        echo ""
        echo -e "${CYAN}2️⃣  If you have existing kubectl access:${NC}"
        echo -e "   kubectl config get-contexts"
        echo -e "   kubectl config use-context YOUR_CONTEXT"
        echo ""
        echo -e "${CYAN}3️⃣  Create local cluster with kind:${NC}"
        echo -e "   # Start Docker Desktop first, then:"
        echo -e "   brew install kind"
        echo -e "   kind create cluster --name activiti-local"
        echo ""
        echo -e "${CYAN}4️⃣  Create local cluster with minikube:${NC}"
        echo -e "   brew install minikube"
        echo -e "   minikube start --profile activiti-local"
        echo ""
        echo -e "${CYAN}📚 For detailed setup: scripts/INSTALL_GUIDE.md${NC}"
        exit 1
    else
        CURRENT_CONTEXT=$(kubectl config current-context 2>/dev/null || echo "unknown")
        echo -e "${GREEN}✓ kubectl connected to: $CURRENT_CONTEXT${NC}"

        # Check if we can actually access the cluster
        if ! kubectl get namespaces &> /dev/null; then
            echo -e "${YELLOW}⚠️  Warning: Connected to cluster but cannot access namespaces.${NC}"
            echo -e "${YELLOW}   This might be a permissions issue.${NC}"
            echo -e "${YELLOW}   Continuing anyway...${NC}"
        fi
    fi

    # Export KUBECTL_CMD for use in the rest of the script (though now it's just regular kubectl)
    export KUBECTL_CMD

    echo ""
}

# Generate environment variables
generate_environment() {
    echo -e "${BLUE}=== Generating Environment Variables ===${NC}"

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
    GLOBAL_GATEWAY_DOMAIN="$CLUSTER_NAME.$CLUSTER_DOMAIN"

    # Generate base PREVIEW_NAME (same logic as GitHub Actions)
    if [[ -n "$PR_NUMBER" ]]; then
        GITHUB_PR_NUMBER="$PR_NUMBER"
        PREVIEW_NAME="pr-${GITHUB_PR_NUMBER}"
    else
        GITHUB_RUN_NUMBER="$RUN_NUMBER"
        PREVIEW_NAME="gh-${GITHUB_RUN_NUMBER}"
    fi

    # Convert boolean partitioned to expected format
    if [[ "$MESSAGING_PARTITIONED" == "true" ]]; then
        MESSAGING_PARTITIONED_SUFFIX="partitioned"
        MESSAGING_PARTITIONED_MAKE="partitioned"
    else
        MESSAGING_PARTITIONED_SUFFIX="non-partitioned"
        MESSAGING_PARTITIONED_MAKE="non-partitioned"
    fi

    # Convert destinations to expected format
    if [[ "$MESSAGING_DESTINATIONS" == "default" ]]; then
        MESSAGING_DESTINATIONS_SUFFIX="default-destinations"
        MESSAGING_DESTINATIONS_MAKE="default-destinations"
    else
        MESSAGING_DESTINATIONS_SUFFIX="override-destinations"
        MESSAGING_DESTINATIONS_MAKE="override-destinations"
    fi

    # Add broker and configuration suffixes (same logic as GitHub Actions)
    PREVIEW_NAME="$PREVIEW_NAME-${MESSAGING_BROKER:0:6}-${MESSAGING_PARTITIONED_SUFFIX:0:1}-${MESSAGING_DESTINATIONS_SUFFIX:0:1}"

    # Generate host URLs
    GATEWAY_HOST="gateway-$PREVIEW_NAME.$GLOBAL_GATEWAY_DOMAIN"
    SSO_HOST="identity-$PREVIEW_NAME.$GLOBAL_GATEWAY_DOMAIN"

    # Export all variables
    export PREVIEW_NAME
    export GLOBAL_GATEWAY_DOMAIN
    export GATEWAY_HOST
    export SSO_HOST
    export MESSAGING_PARTITIONED="$MESSAGING_PARTITIONED_MAKE"
    export MESSAGING_DESTINATIONS="$MESSAGING_DESTINATIONS_MAKE"

    # Set additional protocol variables (from GitHub Actions)
    export SSO_PROTOCOL=https
    export GATEWAY_PROTOCOL=https

    # Generate version if not provided
    if [[ -z "$VERSION" ]]; then
        if [[ -n "$PR_NUMBER" ]]; then
            VERSION="0.0.1-PR-${PR_NUMBER}-SNAPSHOT"
        else
            VERSION="0.0.1-gh-${RUN_NUMBER}-SNAPSHOT"
        fi
        export VERSION
    fi

    echo -e "${GREEN}✅ Environment set for PR #${PR_NUMBER:-$RUN_NUMBER}${NC}"
    echo -e "   ${YELLOW}PREVIEW_NAME:${NC} $PREVIEW_NAME"
    echo -e "   ${YELLOW}GATEWAY_HOST:${NC} $GATEWAY_HOST"
    echo -e "   ${YELLOW}SSO_HOST:${NC} $SSO_HOST"

    echo -e "${GREEN}Environment variables set:${NC}"
    echo -e "  ${YELLOW}PREVIEW_NAME:${NC} $PREVIEW_NAME"
    echo -e "  ${YELLOW}VERSION:${NC} $VERSION"
    echo -e "  ${YELLOW}MESSAGING_BROKER:${NC} $MESSAGING_BROKER"
    echo -e "  ${YELLOW}MESSAGING_PARTITIONED:${NC} $MESSAGING_PARTITIONED"
    echo -e "  ${YELLOW}MESSAGING_DESTINATIONS:${NC} $MESSAGING_DESTINATIONS"
    echo -e "  ${YELLOW}GATEWAY_HOST:${NC} $GATEWAY_HOST"
    echo -e "  ${YELLOW}SSO_HOST:${NC} $SSO_HOST"
    echo ""
}

# Main installation function
perform_installation() {
    echo -e "${BLUE}=== Starting Activiti Cloud Installation ===${NC}"

    # Navigate to root directory
    cd "$ROOT_DIR"

    # Create VERSION file for make
    execute_command "echo '$VERSION' > VERSION" "Creating VERSION file"

    # Delete existing namespace (like in GitHub Actions)
    execute_command "$KUBECTL_CMD delete ns $PREVIEW_NAME || true" "Cleaning up existing namespace"

    # Run make install with all required environment variables
    local make_cmd="KUBECTL='$KUBECTL_CMD' PREVIEW_NAME='$PREVIEW_NAME' MESSAGING_BROKER='$MESSAGING_BROKER' MESSAGING_PARTITIONED='$MESSAGING_PARTITIONED' MESSAGING_DESTINATIONS='$MESSAGING_DESTINATIONS' GLOBAL_GATEWAY_DOMAIN='$GLOBAL_GATEWAY_DOMAIN'"

    # Add local values file if requested
    if [[ "$USE_LOCAL_IMAGES" == "true" ]]; then
        if [[ -f "$ROOT_DIR/local-values.yaml" ]]; then
            # Use absolute path to avoid relative path issues
            local_values_file="$ROOT_DIR/local-values.yaml"
            make_cmd="$make_cmd LOCAL_VALUES_FILE='$local_values_file'"
            echo -e "${YELLOW}Using local image overrides from local-values.yaml${NC}"
        else
            echo -e "${YELLOW}Warning: --use-local-images specified but local-values.yaml not found${NC}"
            echo -e "${YELLOW}Run './scripts/resolve-docker-images.sh' first to create it${NC}"
        fi
    fi

    make_cmd="$make_cmd make install"

    execute_command "$make_cmd" "Running make install"

    if [[ "$DRY_RUN" == "false" ]]; then
        echo -e "${GREEN}=== Installation Completed Successfully! ===${NC}"
        echo -e "${YELLOW}Your Activiti Cloud instance is available at:${NC}"
        echo -e "  ${CYAN}Gateway: https://$GATEWAY_HOST${NC}"
        echo -e "  ${CYAN}SSO: https://$SSO_HOST${NC}"
        echo -e "  ${CYAN}Namespace: $PREVIEW_NAME${NC}"
        echo ""
        echo -e "${YELLOW}To check the deployment status:${NC}"
        echo -e "  ${CYAN}kubectl get pods -n $PREVIEW_NAME${NC}"
        echo -e "  ${CYAN}kubectl get services -n $PREVIEW_NAME${NC}"
        echo ""
        echo -e "${YELLOW}To clean up later:${NC}"
        echo -e "  ${CYAN}make delete PREVIEW_NAME=$PREVIEW_NAME${NC}"
    fi
}

# Validation
if [[ -z "$PR_NUMBER" && -z "$RUN_NUMBER" ]]; then
    echo -e "${RED}Error: Either --pr or --run must be specified${NC}" >&2
    show_help
    exit 1
fi

if [[ "$MESSAGING_BROKER" != "rabbitmq" && "$MESSAGING_BROKER" != "kafka" ]]; then
    echo -e "${RED}Error: Broker must be 'rabbitmq' or 'kafka'${NC}" >&2
    exit 1
fi

if [[ "$MESSAGING_PARTITIONED" != "true" && "$MESSAGING_PARTITIONED" != "false" ]]; then
    echo -e "${RED}Error: Partitioned must be 'true' or 'false'${NC}" >&2
    exit 1
fi

if [[ "$MESSAGING_DESTINATIONS" != "default" && "$MESSAGING_DESTINATIONS" != "override" ]]; then
    echo -e "${RED}Error: Destinations must be 'default' or 'override'${NC}" >&2
    exit 1
fi

# Main execution
echo -e "${GREEN}=== Activiti Cloud Local Installation ===${NC}"
echo ""

check_prerequisites
generate_environment
perform_installation

echo -e "${GREEN}=== Done! ===${NC}"
