#!/bin/bash

# 🚀 Activiti Cloud Complete Environment Setup
# This script consolidates all environment setup functionality into one comprehensive tool
#
# Usage: ./scripts/setup-environment.sh [options]
#
# This script replaces and consolidates:
# - generate-preview-name.sh
# - quick-preview-env.sh
# - setup-playwright-local.sh
# - setup-local-access.sh
# - health-check-enhanced.sh
# - test-health-checks-local.sh

set -e

# Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
MAGENTA='\033[0;35m'
NC='\033[0m' # No Color

# Default values
ENVIRONMENT_NAME=""
CLUSTER_NAME=""
MESSAGING_BROKER="rabbitmq"
MESSAGING_PARTITIONED="false"
MESSAGING_DESTINATIONS="default"
MODE="full"
SKIP_INSTALL=false
SKIP_HEALTH_CHECK=false
SKIP_PORT_FORWARD=false
DRY_RUN=false

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(dirname "$SCRIPT_DIR")"

# Help function
show_help() {
    cat << EOF
🚀 Activiti Cloud Complete Environment Setup

Consolidates all environment setup into one comprehensive script.

USAGE:
    $0 [OPTIONS]

OPTIONS:
    -n, --name <name>           Environment name (e.g., test-123, local-dev)
    -c, --cluster <cluster>     Cluster name (e.g., activiti-hackathon, activiti-community)
    -b, --broker <broker>       Messaging broker: rabbitmq|kafka (default: rabbitmq)
    -pt, --partitioned <bool>   Partitioned: true|false (default: false)
    -d, --destinations <type>   Destinations: default|override (default: default)

    --mode <mode>               Setup mode: full|env-only|test-only|playwright
                               full: Complete setup with installation (default)
                               env-only: Only generate environment variables
                               test-only: Only run health checks and setup access
                               playwright: Setup specifically for Playwright tests

    --skip-install              Skip the installation phase
    --skip-health               Skip health checks
    --skip-port-forward         Skip automatic port forwarding setup
    --dry-run                   Show what would be executed without running
    -h, --help                  Show this help message

EXAMPLES:
    # Complete setup for environment "test-123"
    $0 -n test-123 -c activiti-hackathon

    # Just generate environment variables
    $0 -n local-dev -c activiti-community --mode env-only

    # Setup for Playwright testing
    $0 -n playwright-test -c activiti-hackathon --mode playwright

    # Advanced configuration with installation
    $0 -n kafka-test -c activiti-hackathon -b kafka -pt true -d override

    # Test existing deployment
    $0 -n test-123 -c activiti-hackathon --mode test-only

    # See what would happen
    $0 --dry-run -n test-123 -c activiti-hackathon

MODES:
    full        - Generate env → Install → Setup access → Health checks
    env-only    - Only generate and export environment variables
    test-only   - Setup access and run health checks on existing deployment
    playwright  - Complete setup optimized for Playwright tests
EOF
}

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -n|--name)
            ENVIRONMENT_NAME="$2"
            shift 2
            ;;
        -c|--cluster)
            CLUSTER_NAME="$2"
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
        --mode)
            MODE="$2"
            shift 2
            ;;
        --skip-install)
            SKIP_INSTALL=true
            shift
            ;;
        --skip-health)
            SKIP_HEALTH_CHECK=true
            shift
            ;;
        --skip-port-forward)
            SKIP_PORT_FORWARD=true
            shift
            ;;
        --dry-run)
            DRY_RUN=true
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

# Validation
if [[ -z "$ENVIRONMENT_NAME" ]]; then
    echo -e "${RED}Error: Environment name (--name) must be specified${NC}" >&2
    show_help
    exit 1
fi

if [[ -z "$CLUSTER_NAME" ]]; then
    echo -e "${RED}Error: Cluster name (--cluster) must be specified${NC}" >&2
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

if [[ "$MODE" != "full" && "$MODE" != "env-only" && "$MODE" != "test-only" && "$MODE" != "playwright" ]]; then
    echo -e "${RED}Error: Mode must be 'full', 'env-only', 'test-only', or 'playwright'${NC}" >&2
    exit 1
fi

# Set mode-specific flags
case "$MODE" in
    "env-only")
        SKIP_INSTALL=true
        SKIP_HEALTH_CHECK=true
        SKIP_PORT_FORWARD=true
        ;;
    "test-only")
        SKIP_INSTALL=true
        ;;
    "playwright")
        # Playwright mode - all logic handled in main execution
        ;;
esac

# Function to run commands with dry-run support
execute_command() {
    local cmd="$1"
    local description="$2"

    if [[ "$DRY_RUN" == "true" ]]; then
        echo -e "${CYAN}[DRY-RUN] ${description}${NC}"
        echo -e "${YELLOW}Command: ${cmd}${NC}"
    else
        echo -e "${BLUE}${description}${NC}"
        eval "$cmd"
    fi
}

# Generate environment variables (consolidates generate-preview-name.sh and quick-preview-env.sh)
generate_environment() {
    echo -e "${MAGENTA}=== 🔧 Generating Environment Variables ===${NC}"

    # Use the provided cluster name
    CLUSTER_DOMAIN="envalfresco.com"
    GLOBAL_GATEWAY_DOMAIN="$CLUSTER_NAME.$CLUSTER_DOMAIN"

    # Generate base PREVIEW_NAME from environment name
    PREVIEW_NAME="$ENVIRONMENT_NAME"

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

    # Generate version
    VERSION="0.0.1-${ENVIRONMENT_NAME}-SNAPSHOT"

    # Export all variables
    export PREVIEW_NAME
    export GLOBAL_GATEWAY_DOMAIN
    export GATEWAY_HOST
    export SSO_HOST
    # Keep original MESSAGING_PARTITIONED for local-install.sh compatibility
    export MESSAGING_PARTITIONED
    # Keep original MESSAGING_DESTINATIONS for local-install.sh compatibility
    export MESSAGING_DESTINATIONS
    export VERSION
    export CLUSTER_NAME
    export CLUSTER_DOMAIN

    # Set additional protocol variables
    export SSO_PROTOCOL=https
    export GATEWAY_PROTOCOL=https

    echo -e "${GREEN}✅ Environment variables generated:${NC}"
    echo -e "  ${YELLOW}PREVIEW_NAME:${NC} $PREVIEW_NAME"
    echo -e "  ${YELLOW}CLUSTER_NAME:${NC} $CLUSTER_NAME"
    echo -e "  ${YELLOW}VERSION:${NC} $VERSION"
    echo -e "  ${YELLOW}MESSAGING_BROKER:${NC} $MESSAGING_BROKER"
    echo -e "  ${YELLOW}MESSAGING_PARTITIONED:${NC} $MESSAGING_PARTITIONED_MAKE"
    echo -e "  ${YELLOW}MESSAGING_DESTINATIONS:${NC} $MESSAGING_DESTINATIONS_MAKE"
    echo -e "  ${YELLOW}GATEWAY_HOST:${NC} $GATEWAY_HOST"
    echo -e "  ${YELLOW}SSO_HOST:${NC} $SSO_HOST"
    echo ""

    # For env-only mode, also show export commands
    if [[ "$MODE" == "env-only" ]]; then
        echo -e "${CYAN}📋 Export commands for manual use:${NC}"
        echo "export PREVIEW_NAME='$PREVIEW_NAME'"
        echo "export GATEWAY_HOST='$GATEWAY_HOST'"
        echo "export SSO_HOST='$SSO_HOST'"
        echo "export MESSAGING_BROKER='$MESSAGING_BROKER'"
        echo "export MESSAGING_PARTITIONED='$MESSAGING_PARTITIONED'"
        echo "export MESSAGING_DESTINATIONS='$MESSAGING_DESTINATIONS'"
        echo "export VERSION='$VERSION'"
        echo ""
        echo -e "${CYAN}💡 To use these variables in your current shell:${NC}"
        echo "source <($0 -n $ENVIRONMENT_NAME -c $CLUSTER_NAME --mode env-only 2>/dev/null | grep '^export')"
        echo ""
    fi
}

# Setup /etc/hosts entries (from setup-local-access.sh)
setup_hosts() {
    echo -e "${MAGENTA}=== 🌐 Setting up /etc/hosts entries ===${NC}"

    # Function to add host entry if it doesn't exist
    add_host_entry() {
        local host=$1
        if grep -q "$host" /etc/hosts 2>/dev/null; then
            echo -e "${GREEN}✓ $host already in /etc/hosts${NC}"
        else
            if [[ "$DRY_RUN" == "true" ]]; then
                echo -e "${CYAN}[DRY-RUN] Would add: 127.0.0.1 $host${NC}"
            else
                echo -e "${YELLOW}Adding $host to /etc/hosts...${NC}"
                echo "127.0.0.1 $host" | sudo tee -a /etc/hosts > /dev/null
                echo -e "${GREEN}✓ Added $host to /etc/hosts${NC}"
            fi
        fi
    }

    add_host_entry "$GATEWAY_HOST"
    add_host_entry "$SSO_HOST"
    echo ""
}

# Setup Playwright-specific configuration
setup_playwright() {
    echo -e "${MAGENTA}=== 🎭 Setting up Playwright Test Environment ===${NC}"

    local playwright_dir="$ROOT_DIR/activiti-cloud-acceptance-tests-playwright"
    local env_file="$playwright_dir/.env"
    local backup_file="$playwright_dir/.env.backup"
    local root_env_file="$ROOT_DIR/.env"

    if [[ ! -d "$playwright_dir" ]]; then
        echo -e "${RED}❌ Playwright directory not found: $playwright_dir${NC}"
        return 1
    fi

    cd "$playwright_dir"

    # Backup existing .env file
    if [ -f "$env_file" ]; then
        execute_command "cp '$env_file' '$backup_file'" "Backing up existing .env file"
        echo -e "${GREEN}✓ Backup created at .env.backup${NC}"
    fi

    # Create new .env file
    if [[ "$DRY_RUN" == "false" ]]; then
        echo "🔧 Creating Playwright environment configuration..."

        cat > "$env_file" << EOF
# Environment configuration for Playwright tests
PREVIEW_NAME=$PREVIEW_NAME
CLUSTER_NAME=$CLUSTER_NAME
CLUSTER_DOMAIN=$CLUSTER_DOMAIN
LOCAL_PORT=8080

# For CI detection
CI=false
GITHUB_ACTIONS=false

# Application Configuration (environment-specific)
GATEWAY_PROTOCOL=https
SSO_PROTOCOL=https
GATEWAY_HOST=$GATEWAY_HOST
IDENTITY_HOST=$SSO_HOST
SSO_HOST=https://$CLUSTER_NAME.$CLUSTER_DOMAIN/auth/realms/activiti/protocol/openid-connect/token
REALM=activiti

DEBUG=pw:api
EOF

        # Append user credentials from root .env file if it exists
        if [ -f "$root_env_file" ]; then
            echo "" >> "$env_file"
            echo "# User Credentials (from root .env)" >> "$env_file"
            grep -E "^[A-Z_]*USER_USERNAME=" "$root_env_file" >> "$env_file" 2>/dev/null || true
            grep -E "^[A-Z_]*USER_PASSWORD=" "$root_env_file" >> "$env_file" 2>/dev/null || true
            echo -e "${GREEN}✓ Merged user credentials from root .env${NC}"
        fi

        echo -e "${GREEN}✓ Created Playwright environment configuration${NC}"
    else
        echo -e "${CYAN}[DRY-RUN] Would create Playwright .env file${NC}"
    fi

    # Install dependencies
    if [ ! -d "node_modules" ]; then
        execute_command "npm install" "Installing npm dependencies"
    else
        echo -e "${GREEN}✓ Dependencies already installed${NC}"
    fi

    # Install Playwright browsers if needed
    if [ ! -d "node_modules/@playwright/test" ]; then
        execute_command "npx playwright install" "Installing Playwright browsers"
    fi

    cd "$ROOT_DIR"
    echo ""
}

# Run installation (uses local-install.sh)
run_installation() {
    echo -e "${MAGENTA}=== 🚀 Running Installation ===${NC}"

    # Resolve Docker images and create local-values.local.yaml with working tags
    echo -e "${BLUE}Resolving Docker image versions...${NC}"
    local use_local_images=false

    # Run resolve script to ensure we have working image tags
    if command -v ./scripts/resolve-docker-images.sh >/dev/null 2>&1; then
        if [[ "$DRY_RUN" == "false" ]]; then
            echo -e "${CYAN}Running image resolution for version $VERSION...${NC}"
            ./scripts/resolve-docker-images.sh "$VERSION" >/dev/null 2>&1
        else
            echo -e "${CYAN}[DRY-RUN] Would run: ./scripts/resolve-docker-images.sh $VERSION${NC}"
        fi

        # If local-values.local.yaml exists, use it for reliable image tags
        if [[ -f "local-values.local.yaml" ]]; then
            echo -e "${GREEN}✓ Using local-values.local.yaml with verified working image tags${NC}"
            use_local_images=true
        elif [[ -f "local-values.yaml" ]]; then
            echo -e "${YELLOW}⚠ Using legacy local-values.yaml (please migrate to local-values.local.yaml)${NC}"
            use_local_images=true
        fi
    else
        # If resolve script doesn't exist, check for existing local-values.local.yaml
        if [[ -f "local-values.local.yaml" ]]; then
            echo -e "${YELLOW}⚠ Using existing local-values.local.yaml (resolve script not found)${NC}"
            use_local_images=true
        elif [[ -f "local-values.yaml" ]]; then
            echo -e "${YELLOW}⚠ Using legacy local-values.yaml (resolve script not found)${NC}"
            use_local_images=true
        else
            echo -e "${YELLOW}⚠ No image resolution available - using default tags${NC}"
        fi
    fi

    # Note: local-install.sh still uses legacy PR-based logic internally
    # We pass our environment name as a "PR" to maintain compatibility
    local pr_for_install="$ENVIRONMENT_NAME"
    local install_cmd="./scripts/local-install.sh -p $pr_for_install -b $MESSAGING_BROKER -pt $MESSAGING_PARTITIONED -d $MESSAGING_DESTINATIONS"

    # Add local images flag if needed
    if [[ "$use_local_images" == "true" ]]; then
        install_cmd="$install_cmd --use-local-images"
    fi

    if [[ "$DRY_RUN" == "true" ]]; then
        install_cmd="$install_cmd --dry-run"
    fi

    execute_command "$install_cmd" "Running complete installation"
    echo ""
}

# Setup port forwarding (from setup-local-access.sh)
setup_port_forwarding() {
    echo -e "${MAGENTA}=== 🔗 Setting up Port Forwarding ===${NC}"

    # Check if namespace exists (try pr- prefix first as it's the standard naming)
    local actual_namespace="pr-$PREVIEW_NAME"
    if ! kubectl get namespace "$actual_namespace" >/dev/null 2>&1; then
        # Fall back to non-prefixed namespace
        actual_namespace="$PREVIEW_NAME"
        if ! kubectl get namespace "$actual_namespace" >/dev/null 2>&1; then
            echo -e "${RED}❌ Namespace 'pr-$PREVIEW_NAME' or '$PREVIEW_NAME' not found${NC}"
            echo "Please run installation first or check the deployment."
            return 1
        fi
    fi

    if [[ "$DRY_RUN" == "false" ]]; then
        echo "Starting port forwarding to ingress controller..."
        echo "This will forward local port 8080 to the cluster ingress."
        echo ""
        echo -e "${YELLOW}💡 After port forwarding starts, you can test with:${NC}"
        echo "curl http://$GATEWAY_HOST:8080/identity-adapter-service/actuator/health"
        echo "curl http://$SSO_HOST:8080/auth/realms/activiti/.well-known/openid_configuration"
        echo ""
        echo -e "${BLUE}Press Ctrl+C to stop port forwarding...${NC}"

        # Start port forwarding in foreground
        kubectl port-forward svc/ingress-nginx-controller 8080:80 -n default
    else
        echo -e "${CYAN}[DRY-RUN] Would start: kubectl port-forward svc/ingress-nginx-controller 8080:80 -n default${NC}"
    fi
    echo ""
}

# Health checks (consolidates health-check-enhanced.sh and test-health-checks-local.sh)
run_health_checks() {
    echo -e "${MAGENTA}=== 🏥 Running Health Checks ===${NC}"

    # Check if namespace exists, try pr- prefix first (standard naming from local-install.sh)
    local actual_namespace="pr-$PREVIEW_NAME"
    if ! kubectl get namespace "$actual_namespace" >/dev/null 2>&1; then
        # Fall back to non-prefixed namespace (legacy naming)
        actual_namespace="$PREVIEW_NAME"
        if ! kubectl get namespace "$actual_namespace" >/dev/null 2>&1; then
            echo -e "${RED}❌ Namespace 'pr-$PREVIEW_NAME' or '$PREVIEW_NAME' not found${NC}"
            echo -e "${YELLOW}Available namespaces:${NC}"
            kubectl get namespaces | grep -E "(michal|local|test)" || echo "No matching namespaces found"
            return 1
        else
            echo -e "${GREEN}✓ Found namespace: $actual_namespace${NC}"
            local actual_gateway_host="$GATEWAY_HOST"
            local actual_sso_host="$SSO_HOST"
        fi
    else
        echo -e "${GREEN}✓ Found namespace: $actual_namespace${NC}"
        # Update hosts to match the actual namespace for health checks
        local actual_gateway_host="gateway-$actual_namespace.$GLOBAL_GATEWAY_DOMAIN"
        local actual_sso_host="identity-$actual_namespace.$GLOBAL_GATEWAY_DOMAIN"
        echo -e "${BLUE}Using hosts: $actual_gateway_host, $actual_sso_host${NC}"
    fi

    # Function to test via port forwarding with ingress
    test_via_ingress() {
        local host=$1
        local path=$2
        local service_name=$3

        echo -e "${BLUE}Testing $service_name via ingress...${NC}"

        if [[ "$DRY_RUN" == "true" ]]; then
            echo -e "${CYAN}[DRY-RUN] Would test: curl -H 'Host: $host' http://localhost:8080$path${NC}"
            return
        fi

        # Start port forwarding to ingress controller
        kubectl port-forward svc/ingress-nginx-controller 8080:80 -n default &
        local pf_pid=$!

        # Wait for port forwarding to establish
        sleep 3

        # Test the endpoint
        local success=false
        for i in $(seq 1 3); do
            response=$(curl -s -H "Host: $host" -w "HTTP %{http_code}" -o /dev/null "http://localhost:8080$path" 2>/dev/null || echo "HTTP 000")
            if [[ $response == "HTTP 200" ]]; then
                echo -e "${GREEN}✅ $service_name: SUCCESS${NC}"
                success=true
                break
            fi
            sleep 2
        done

        if [ "$success" = false ]; then
            echo -e "${YELLOW}⚠ $service_name: $response${NC}"
        fi

        # Clean up port forwarding
        kill $pf_pid 2>/dev/null || true
        sleep 1
    }

    echo -e "${BLUE}Testing health endpoints via ingress...${NC}"
    test_via_ingress "$actual_gateway_host" "/identity-adapter-service/actuator/health" "Identity Adapter"
    test_via_ingress "$actual_gateway_host" "/rb/actuator/health" "Runtime Bundle"
    test_via_ingress "$actual_gateway_host" "/query/actuator/health" "Query Service"
    test_via_ingress "$actual_sso_host" "/auth/realms/activiti/.well-known/openid_configuration" "Keycloak"

    echo ""
    echo -e "${GREEN}🎉 Health checks completed!${NC}"
    echo ""
    echo -e "${YELLOW}Your Activiti Cloud deployment is accessible at:${NC}"
    echo "• Gateway: http://$actual_gateway_host:8080 (with port forwarding)"
    echo "• Identity: http://$actual_sso_host:8080/auth (with port forwarding)"
    echo "• Namespace: $actual_namespace"
    echo ""
}

# Main execution flow
main() {
    echo -e "${GREEN}🚀 Activiti Cloud Complete Environment Setup${NC}"
    echo -e "${BLUE}Mode: $MODE${NC}"
    echo ""

    # Always generate environment
    generate_environment

    # Mode-specific execution
    case "$MODE" in
        "env-only")
            echo -e "${GREEN}✅ Environment variables generated (env-only mode)${NC}"
            ;;
        "full")
            if [[ "$SKIP_INSTALL" == "false" ]]; then
                run_installation
            fi
            setup_hosts
            if [[ "$SKIP_HEALTH_CHECK" == "false" ]]; then
                run_health_checks
            fi
            if [[ "$SKIP_PORT_FORWARD" == "false" ]]; then
                echo -e "${CYAN}💡 To start port forwarding:${NC}"
                echo "$0 -n $ENVIRONMENT_NAME -c $CLUSTER_NAME --mode test-only --skip-health"
                echo "or manually: kubectl port-forward svc/ingress-nginx-controller 8080:80 -n default"
            fi
            ;;
        "test-only")
            setup_hosts
            if [[ "$SKIP_HEALTH_CHECK" == "false" ]]; then
                run_health_checks
            fi
            if [[ "$SKIP_PORT_FORWARD" == "false" ]]; then
                setup_port_forwarding
            fi
            ;;
        "playwright")
            if [[ "$SKIP_INSTALL" == "false" ]]; then
                run_installation
            fi
            setup_hosts
            setup_playwright
            if [[ "$SKIP_HEALTH_CHECK" == "false" ]]; then
                run_health_checks
            fi
            echo -e "${GREEN}✅ Playwright environment ready!${NC}"
            echo ""
            echo "You can now run Playwright tests with:"
            echo -e "${BLUE}npm test${NC}"
            ;;
    esac

    echo -e "${GREEN}🎉 Setup completed successfully!${NC}"
}

# Run main function
main
