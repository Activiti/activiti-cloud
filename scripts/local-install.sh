#!/bin/bash

# Local Activiti Cloud Installation Script
# Replicates the GitHub Actions "Install release" functionality for local development
#
# Usage: ./scripts/local-install.sh [options]
#
# Options:
#   -n, --name <name>           Environment name (e.g., michal-test, feature-xyz)
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
ENVIRONMENT_NAME=""
MESSAGING_BROKER="rabbitmq"
MESSAGING_PARTITIONED="false"
MESSAGING_DESTINATIONS="default"
VERSION=""
DRY_RUN=false
USE_LOCAL_IMAGES=true  # Always use local images by default
CLUSTER_NAME=""        # Will be auto-detected or specified
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
    -n, --name <name>           Environment name (e.g., michal-test, feature-xyz)
    -c, --cluster <name>        Cluster name (auto-detected if not specified)
    -b, --broker <broker>       Messaging broker: rabbitmq|kafka (default: rabbitmq)
    -pt, --partitioned <bool>   Partitioned: true|false (default: false)
    -d, --destinations <type>   Destinations: default|override (default: default)
    -v, --version <version>     Version to use (default: auto-generated)
    --no-local-images           Don't use local-values.yaml (uses generated versions)
    --dry-run                   Show what would be executed without running
    -h, --help                  Show this help message

EXAMPLES:
    $0 -n michal-test                           # Basic environment with working image tags
    $0 -n feature-xyz -b kafka -pt true        # Kafka with partitioning
    $0 --dry-run -n test-env                    # See what would happen
    $0 -n my-env -c activiti-hackathon         # Specify cluster explicitly

PREREQUISITES:
    - kubectl configured and connected to cluster (or use rancher CLI)
    - helm installed (version 3+)
    - yq installed (for YAML processing)
    - python3 available (for version parsing)
    - Keycloak client secret for 'activiti-keycloak' client
    - Keycloak admin login from Kubernetes secret 'keycloak-admin-credentials'

ENVIRONMENT VARIABLES:
    KEYCLOAK_CLIENT_SECRET    Set to avoid interactive prompt for client secret

WORKFLOW:
    1. Checks/configures cluster connection
    2. Prompts for Keycloak client secret (if not set via env var)
    3. Ensures local-values.yaml exists with working image tags
    4. Deploys Activiti Cloud with reliable configuration
    5. Configures identity adapter with correct Keycloak settings
    6. Generates .env file for Playwright tests

NOTES:
    - Uses local-values.yaml by default for reliable deployments
    - Auto-detects cluster or helps configure it
    - Keycloak admin username/password come from the Kubernetes secret 'keycloak-admin-credentials' in the same namespace as the Keycloak pod
    - Generates complete .env configuration at the end
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
        -v|--version)
            VERSION="$2"
            shift 2
            ;;
        --dry-run)
            DRY_RUN=true
            shift
            ;;
        --no-local-images)
            USE_LOCAL_IMAGES=false
            shift
            ;;
        --use-local-images)
            USE_LOCAL_IMAGES=true  # Redundant since it's default, but keep for compatibility
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

# Function to read a secret from the terminal without echoing it.
# Supports pasting and falls back to stdin when no TTY is available.
prompt_for_secret() {
    local prompt="$1"

    if [[ -t 0 ]]; then
        printf "%b" "$prompt"
        IFS= read -r -s REPLY
        printf "\n"
    else
        printf "%b" "$prompt"
        IFS= read -r REPLY
    fi
}

# Function to configure Keycloak settings
configure_keycloak() {
    echo -e "${BLUE}=== Configuring Keycloak Settings ===${NC}"

    # Determine correct Keycloak URL - use configured cluster domain
    CLUSTER_DOMAIN="envalfresco.com"
    KEYCLOAK_URL="https://$CLUSTER_NAME.$CLUSTER_DOMAIN/auth"
    KEYCLOAK_REALM="alfresco"
    KEYCLOAK_CLIENT_ID="activiti-keycloak"

    echo -e "${YELLOW}Detected Keycloak configuration:${NC}"
    echo -e "  ${CYAN}URL: $KEYCLOAK_URL${NC}"
    echo -e "  ${CYAN}Realm: $KEYCLOAK_REALM${NC}"
    echo -e "  ${CYAN}Client ID: $KEYCLOAK_CLIENT_ID${NC}"

    # Check if client secret is provided as environment variable
    if [[ -n "$KEYCLOAK_CLIENT_SECRET" ]]; then
        echo -e "${GREEN}✓ Using Keycloak client secret from environment variable${NC}"
    else
        echo ""
        echo -e "${YELLOW}🔑 Keycloak Client Secret Required${NC}"
        echo ""
        printf "%b\n" "${CYAN}The identity adapter needs the correct client secret for '$KEYCLOAK_CLIENT_ID'${NC}"
        printf "%b\n" "${CYAN}You can find this secret in the Keycloak admin console:${NC}"
        printf "%b\n" "${CYAN}To log in, use the admin credentials stored in the Kubernetes secret 'keycloak-admin-credentials' for the Keycloak pod/namespace.${NC}"
        printf "%b\n" "  ${CYAN}Example username: kubectl get secret keycloak-admin-credentials -n <keycloak-namespace> -o jsonpath='{.data.username}' | base64 --decode && echo${NC}"
        printf "%b\n" "  ${CYAN}Example password: kubectl get secret keycloak-admin-credentials -n <keycloak-namespace> -o jsonpath='{.data.password}' | base64 --decode && echo${NC}"
        printf "%b\n" "  ${CYAN}1. Open: $KEYCLOAK_URL/admin/master/console/#/alfresco/clients${NC}"
        printf "%b\n" "  ${CYAN}2. Find client: $KEYCLOAK_CLIENT_ID${NC}"
        printf "%b\n" "  ${CYAN}3. Go to Credentials tab${NC}"
        printf "%b\n" "  ${CYAN}4. Copy the Client Secret${NC}"
        echo ""

        if [[ "$DRY_RUN" == "true" ]]; then
            echo -e "${CYAN}[DRY-RUN] Would prompt for client secret${NC}"
            KEYCLOAK_CLIENT_SECRET="<WOULD_PROMPT_FOR_SECRET>"
        else
            printf "%b\n" "${CYAN}Paste the client secret and press Enter. Input will be hidden.${NC}"
            prompt_for_secret "${YELLOW}Enter the Keycloak client secret for '$KEYCLOAK_CLIENT_ID': ${NC}"
            KEYCLOAK_CLIENT_SECRET="$REPLY"
            unset REPLY

            if [[ -z "$KEYCLOAK_CLIENT_SECRET" ]]; then
                echo -e "${RED}Error: Client secret is required${NC}" >&2
                exit 1
            fi
        fi
    fi

    # Test the client credentials if not in dry-run mode
    if [[ "$DRY_RUN" == "false" ]]; then
        echo -e "${YELLOW}Testing client credentials...${NC}"
        local test_response
        test_response=$(curl -s -X POST "$KEYCLOAK_URL/realms/$KEYCLOAK_REALM/protocol/openid-connect/token" \
            -H "Content-Type: application/x-www-form-urlencoded" \
            -d "grant_type=client_credentials&client_id=$KEYCLOAK_CLIENT_ID&client_secret=$KEYCLOAK_CLIENT_SECRET" \
            2>/dev/null || echo '{"error":"connection_failed"}')

        if echo "$test_response" | grep -q '"access_token"'; then
            echo -e "${GREEN}✓ Client credentials are valid${NC}"
        else
            echo -e "${RED}✗ Client credentials test failed${NC}" >&2
            echo -e "${YELLOW}Response: $test_response${NC}" >&2
            echo -e "${YELLOW}Please verify the client secret is correct${NC}" >&2
            exit 1
        fi
    fi

    # Export for use in deployment
    export KEYCLOAK_URL
    export KEYCLOAK_REALM
    export KEYCLOAK_CLIENT_ID
    export KEYCLOAK_CLIENT_SECRET

    echo -e "${GREEN}✓ Keycloak configuration ready${NC}"
    echo ""
}

# Function to configure /etc/hosts entries
configure_hosts_file() {
    echo -e "${BLUE}=== Configuring /etc/hosts ===${NC}"

    local gateway_host="gateway-$PREVIEW_NAME.$GLOBAL_GATEWAY_DOMAIN"
    local identity_host="identity-$PREVIEW_NAME.$GLOBAL_GATEWAY_DOMAIN"

    echo -e "${YELLOW}Checking /etc/hosts entries for local development...${NC}"

    if [[ "$DRY_RUN" == "true" ]]; then
        echo -e "${CYAN}[DRY-RUN] Would add to /etc/hosts:${NC}"
        echo -e "${CYAN}127.0.0.1 $gateway_host${NC}"
        echo -e "${CYAN}127.0.0.1 $identity_host${NC}"
        return 0
    fi

    # Check if entries already exist
    local needs_gateway=true
    local needs_identity=true

    if grep -q "127.0.0.1 $gateway_host" /etc/hosts 2>/dev/null; then
        needs_gateway=false
        echo -e "${GREEN}✓ Gateway host entry already exists${NC}"
    fi

    if grep -q "127.0.0.1 $identity_host" /etc/hosts 2>/dev/null; then
        needs_identity=false
        echo -e "${GREEN}✓ Identity host entry already exists${NC}"
    fi

    if [[ "$needs_gateway" == "true" || "$needs_identity" == "true" ]]; then
        echo -e "${YELLOW}Adding entries to /etc/hosts...${NC}"
        echo -e "${CYAN}This requires sudo access to modify /etc/hosts${NC}"

        # Create temporary file with new entries
        local temp_hosts=$(mktemp)
        if [[ "$needs_gateway" == "true" ]]; then
            echo "127.0.0.1 $gateway_host" >> "$temp_hosts"
        fi
        if [[ "$needs_identity" == "true" ]]; then
            echo "127.0.0.1 $identity_host" >> "$temp_hosts"
        fi

        # Add to /etc/hosts
        if sudo sh -c "cat '$temp_hosts' >> /etc/hosts"; then
            echo -e "${GREEN}✓ /etc/hosts updated successfully${NC}"
            [[ "$needs_gateway" == "true" ]] && echo -e "${GREEN}  Added: 127.0.0.1 $gateway_host${NC}"
            [[ "$needs_identity" == "true" ]] && echo -e "${GREEN}  Added: 127.0.0.1 $identity_host${NC}"
        else
            echo -e "${YELLOW}⚠️  Failed to update /etc/hosts automatically${NC}"
            echo -e "${YELLOW}Please add these entries manually:${NC}"
            [[ "$needs_gateway" == "true" ]] && echo -e "${CYAN}127.0.0.1 $gateway_host${NC}"
            [[ "$needs_identity" == "true" ]] && echo -e "${CYAN}127.0.0.1 $identity_host${NC}"
        fi

        rm -f "$temp_hosts"
    else
        echo -e "${GREEN}✓ All required /etc/hosts entries already exist${NC}"
    fi

    echo ""
}

# Function to configure cluster connection
configure_cluster() {
    echo -e "${BLUE}=== Configuring Cluster Connection ===${NC}"

    # Check if kubectl is working
    if kubectl cluster-info &> /dev/null; then
        CURRENT_CONTEXT=$(kubectl config current-context 2>/dev/null || echo "unknown")
        echo -e "${GREEN}✓ kubectl already connected to: $CURRENT_CONTEXT${NC}"

        # Auto-detect cluster name if not specified
        if [[ -z "$CLUSTER_NAME" ]]; then
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
                    # Use the context name directly for other clusters like aae-38098
                    CLUSTER_NAME="$CURRENT_CONTEXT"
                    ;;
            esac
            echo -e "${YELLOW}Auto-detected cluster: $CLUSTER_NAME${NC}"
        fi
        return 0
    fi

    # kubectl not working, try to configure it
    echo -e "${YELLOW}kubectl not connected to cluster. Attempting to configure...${NC}"

    # Check if rancher CLI is available
    if command -v rancher >/dev/null 2>&1; then
        echo -e "${YELLOW}Found rancher CLI, attempting to configure kubectl...${NC}"

        local target_cluster="${CLUSTER_NAME:-activiti}"

        if [[ "$DRY_RUN" == "true" ]]; then
            echo -e "${CYAN}[DRY-RUN] Would run: ./scripts/fix-kubectl-config.sh $target_cluster${NC}"
        else
            if "$SCRIPT_DIR/fix-kubectl-config.sh" "$target_cluster"; then
                echo -e "${GREEN}✓ kubectl configured successfully${NC}"
                CLUSTER_NAME="$target_cluster"
            else
                echo -e "${RED}✗ Failed to configure kubectl${NC}" >&2
                echo -e "${YELLOW}Please configure kubectl manually or run:${NC}"
                echo -e "${CYAN}  ./scripts/fix-kubectl-config.sh [cluster-name]${NC}"
                exit 1
            fi
        fi
    else
        echo -e "${RED}✗ kubectl not connected and rancher CLI not found${NC}" >&2
        echo -e "${YELLOW}Please configure kubectl connection manually${NC}"
        exit 1
    fi
}

# Function to ensure local-values.yaml exists
ensure_local_values() {
    echo -e "${BLUE}=== Ensuring Local Docker Images Configuration ===${NC}"

    if [[ "$USE_LOCAL_IMAGES" == "false" ]]; then
        echo -e "${YELLOW}Skipping local-values.yaml (--no-local-images specified)${NC}"
        return 0
    fi

    local local_values_file="$ROOT_DIR/local-values.yaml"

    if [[ -f "$local_values_file" ]]; then
        echo -e "${GREEN}✓ local-values.yaml already exists${NC}"
        echo -e "${YELLOW}Using working image tags from local-values.yaml${NC}"

        # Extract version from local-values.yaml for use as main VERSION
        if command -v yq >/dev/null 2>&1; then
            LOCAL_IMAGE_VERSION=$(yq e '.runtime-bundle.image.tag' "$local_values_file" 2>/dev/null || echo "")
            if [[ -n "$LOCAL_IMAGE_VERSION" && "$LOCAL_IMAGE_VERSION" != "null" ]]; then
                echo -e "${YELLOW}Detected working image version: $LOCAL_IMAGE_VERSION${NC}"
                export LOCAL_IMAGE_VERSION
            fi
        fi
    else
        echo -e "${YELLOW}local-values.yaml not found, creating it...${NC}"

        if [[ "$DRY_RUN" == "true" ]]; then
            echo -e "${CYAN}[DRY-RUN] Would run: ./scripts/resolve-docker-images.sh${NC}"
        else
            if "$SCRIPT_DIR/resolve-docker-images.sh"; then
                echo -e "${GREEN}✓ local-values.yaml created with working image tags${NC}"

                # Extract version from newly created local-values.yaml
                if command -v yq >/dev/null 2>&1; then
                    LOCAL_IMAGE_VERSION=$(yq e '.runtime-bundle.image.tag' "$local_values_file" 2>/dev/null || echo "")
                    if [[ -n "$LOCAL_IMAGE_VERSION" && "$LOCAL_IMAGE_VERSION" != "null" ]]; then
                        echo -e "${YELLOW}Using working image version: $LOCAL_IMAGE_VERSION${NC}"
                        export LOCAL_IMAGE_VERSION
                    fi
                fi
            else
                echo -e "${RED}✗ Failed to create local-values.yaml${NC}" >&2
                echo -e "${YELLOW}Will proceed without local image overrides${NC}"
                USE_LOCAL_IMAGES=false
            fi
        fi
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

    # CLUSTER_NAME should already be set by configure_cluster
    if [[ -z "$CLUSTER_NAME" ]]; then
        echo -e "${RED}Error: Cluster name not configured${NC}" >&2
        exit 1
    fi

    CLUSTER_DOMAIN="envalfresco.com"
    GLOBAL_GATEWAY_DOMAIN="$CLUSTER_NAME.$CLUSTER_DOMAIN"

    # Generate base PREVIEW_NAME using environment name
    if [[ -n "$ENVIRONMENT_NAME" ]]; then
        PREVIEW_NAME="pr-${ENVIRONMENT_NAME}"
    else
        echo -e "${RED}Error: Environment name is required${NC}" >&2
        exit 1
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
        if [[ "$USE_LOCAL_IMAGES" == "true" && -n "$LOCAL_IMAGE_VERSION" ]]; then
            # When using local images, use the version from local-values.yaml
            VERSION="$LOCAL_IMAGE_VERSION"
            echo -e "${YELLOW}Using version from local-values.yaml: $VERSION${NC}"
        else
            # When not using local images, generate custom version
            VERSION="0.0.1-${ENVIRONMENT_NAME}-SNAPSHOT"
            echo -e "${YELLOW}Generated custom version: $VERSION${NC}"
        fi
        export VERSION
    fi

    echo -e "${GREEN}✅ Environment set for: ${ENVIRONMENT_NAME}${NC}"
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

    # Configure Keycloak settings in the deployed identity adapter
    if [[ "$DRY_RUN" == "false" ]]; then
        echo -e "${BLUE}=== Configuring Identity Adapter ===${NC}"

        # Dynamic deployment name based on preview name
        local identity_deployment="${PREVIEW_NAME}-activiti-cloud-identity-adapter"

        # Wait for deployment to be ready
        echo -e "${YELLOW}Waiting for identity adapter deployment...${NC}"
        kubectl wait --for=condition=available --timeout=300s deployment/$identity_deployment -n $PREVIEW_NAME || true

        # Update the identity adapter with correct Keycloak configuration
        echo -e "${YELLOW}Updating Keycloak client secret...${NC}"
        kubectl patch secret activiti-keycloak-client -n $PREVIEW_NAME -p "{\"data\":{\"clientSecret\":\"$(echo -n "$KEYCLOAK_CLIENT_SECRET" | base64)\"}}"

        # Update all deployments with correct Keycloak URL and realm
        echo -e "${YELLOW}Updating Keycloak URL and realm configuration...${NC}"
        local deployments=(
            "$PREVIEW_NAME-activiti-cloud-connector"
            "$PREVIEW_NAME-activiti-cloud-identity-adapter"
            "$PREVIEW_NAME-activiti-cloud-query"
            "$PREVIEW_NAME-runtime-bundle"
        )

        for deployment in "${deployments[@]}"; do
            if kubectl get deployment "$deployment" -n $PREVIEW_NAME &>/dev/null; then
                echo -e "${CYAN}  Updating $deployment...${NC}"
                kubectl patch deployment "$deployment" -n $PREVIEW_NAME -p "{\"spec\":{\"template\":{\"spec\":{\"containers\":[{\"name\":\"${deployment#$PREVIEW_NAME-}\",\"env\":[{\"name\":\"ACT_KEYCLOAK_URL\",\"value\":\"$KEYCLOAK_URL\"},{\"name\":\"ACT_KEYCLOAK_REALM\",\"value\":\"$KEYCLOAK_REALM\"}]}]}}}}"
            fi
        done        # Restart the deployment to pick up the new secret
        echo -e "${YELLOW}Restarting identity adapter to pick up new configuration...${NC}"
        kubectl rollout restart deployment/$identity_deployment -n $PREVIEW_NAME
        kubectl rollout status deployment/$identity_deployment -n $PREVIEW_NAME

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

# Function to generate .env file for Playwright tests
generate_env_file() {
    echo -e "${BLUE}=== Generating .env Configuration ===${NC}"

    local env_file="$ROOT_DIR/activiti-cloud-acceptance-tests-playwright/.env"
    local local_port="8080"

    # Use the configured Keycloak realm
    local realm="${KEYCLOAK_REALM:-alfresco}"

    if [[ "$DRY_RUN" == "true" ]]; then
        echo -e "${CYAN}[DRY-RUN] Would create .env file at: $env_file${NC}"
        echo -e "${YELLOW}Environment variables that would be written:${NC}"
    else
        # Create the directory if it doesn't exist
        mkdir -p "$(dirname "$env_file")"

        cat > "$env_file" << EOF
# Environment configuration for Playwright tests
PREVIEW_NAME=$PREVIEW_NAME
CLUSTER_NAME=$CLUSTER_NAME
CLUSTER_DOMAIN=envalfresco.com
LOCAL_PORT=$local_port

# For CI detection
CI=false
GITHUB_ACTIONS=false

# Application Configuration (environment-specific)
GATEWAY_PROTOCOL=http
GATEWAY_HOST=gateway-$PREVIEW_NAME.$GLOBAL_GATEWAY_DOMAIN:$local_port
GATEWAY_URL=http://gateway-$PREVIEW_NAME.$GLOBAL_GATEWAY_DOMAIN:$local_port
SSO_PROTOCOL=http
IDENTITY_HOST=identity-$PREVIEW_NAME.$GLOBAL_GATEWAY_DOMAIN:$local_port
SSO_HOST=$KEYCLOAK_URL/realms/$realm/protocol/openid-connect/token
REALM=$realm

# Keycloak Configuration
KEYCLOAK_REALM=$realm
KEYCLOAK_CLIENT_ID=$KEYCLOAK_CLIENT_ID
KEYCLOAK_CLIENT_SECRET=$KEYCLOAK_CLIENT_SECRET

# Application Configuration
ACTIVITI_CLOUD_APPLICATION_NAME=default-app

# DEBUG=pw:api

# User Credentials (from realm-secret / realm.json - all use password 'password')
HRUSER_USERNAME=hruser
HRUSER_PASSWORD=password
HRADMIN_USERNAME=hradmin
HRADMIN_PASSWORD=password
PROCESSADMINUSER_USERNAME=processadminuser
PROCESSADMINUSER_PASSWORD=password
MODELER_USERNAME=hruser
MODELER_PASSWORD=password
MODELERQA_USERNAME=hruser
MODELERQA_PASSWORD=password
DEVOPSUSER_USERNAME=hruser
DEVOPSUSER_PASSWORD=password
SUPERADMINUSER_USERNAME=superadminuser
SUPERADMINUSER_PASSWORD=password
ALFRESCO_ADMINISTRATOR_USERNAME=superadminuser
ALFRESCO_ADMINISTRATOR_PASSWORD=password
SALESUSER_USERNAME=salesuser
SALESUSER_PASSWORD=password
TESTADMIN_USERNAME=testadmin
TESTADMIN_PASSWORD=password
TESTUSER_USERNAME=testuser
TESTUSER_PASSWORD=password

EOF

        echo -e "${GREEN}✓ .env file created at: $env_file${NC}"
    fi

    echo ""
    echo -e "${GREEN}=== 🎯 NEXT STEPS FOR PLAYWRIGHT TESTS ===${NC}"
    echo -e "${YELLOW}1. Add the following entries to your /etc/hosts file:${NC}"
    echo -e "${CYAN}   127.0.0.1 $GATEWAY_HOST${NC}"
    echo -e "${CYAN}   127.0.0.1 $SSO_HOST${NC}"
    echo ""
    echo -e "${YELLOW}2. Start port forwarding to Traefik:${NC}"
    echo -e "${CYAN}   kubectl port-forward svc/traefik $local_port:80 -n traefik${NC}"
    echo ""
    echo -e "${YELLOW}3. Configure user credentials in the .env file:${NC}"
    echo -e "${CYAN}   # Add your test user credentials to the .env file${NC}"
    echo -e "${CYAN}   # Copy from root .env or use your own test credentials${NC}"
    echo ""
    echo -e "${YELLOW}4. Run the Playwright tests:${NC}"
    echo -e "${CYAN}   cd activiti-cloud-acceptance-tests-playwright${NC}"
    echo -e "${CYAN}   npm test${NC}"
    echo ""
    echo -e "${GREEN}The .env file has been automatically configured with the correct values!${NC}"
}

# Validation
if [[ -z "$ENVIRONMENT_NAME" ]]; then
    echo -e "${RED}Error: Environment name is required (use -n or --name)${NC}" >&2
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

configure_cluster
configure_keycloak
check_prerequisites
ensure_local_values
generate_environment
perform_installation
configure_hosts_file
generate_env_file

echo -e "${GREEN}=== Done! ===${NC}"
echo ""
echo -e "${BLUE}Next steps for local development:${NC}"
echo -e "${CYAN}1. Port forwarding will be automatically started by Playwright tests${NC}"
echo -e "${CYAN}2. Run tests: cd activiti-cloud-acceptance-tests-playwright && npm test${NC}"
echo -e "${CYAN}3. Or start port forwarding manually:${NC}"
echo -e "${CYAN}   kubectl port-forward -n traefik svc/traefik 8080:80${NC}"
echo -e "${CYAN}4. Access services via the configured /etc/hosts entries on localhost:8080${NC}"
echo ""
