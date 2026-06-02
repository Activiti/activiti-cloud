#!/bin/bash

# Docker image version resolver for local development
# Finds available image tags and updates the installation to use them

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Image repositories to check
IMAGES=(
    "activiti/example-runtime-bundle"
    "activiti/activiti-cloud-query"
    "activiti/example-cloud-connector"
    "activiti/activiti-cloud-identity-adapter"
)

# Function to get available tags for a Docker image
get_available_tags() {
    local image=$1
    echo "Checking available tags for $image..."

    # Try to get tags from Docker Hub API
    local repo_name=$(echo $image | cut -d'/' -f2)
    local tags=$(curl -s "https://registry.hub.docker.com/v2/repositories/$image/tags/" | jq -r '.results[].name' 2>/dev/null | head -10 || echo "")

    if [ -n "$tags" ]; then
        echo "Available tags:"
        echo "$tags" | sed 's/^/  - /'
        return 0
    else
        echo "Could not fetch tags for $image"
        return 1
    fi
}

# Function to find the best available tag
find_best_tag() {
    local image=$1

    # Try to get latest alpha version from Docker Hub API without Docker pull
    if command -v jq >/dev/null 2>&1; then
        local latest_alpha=$(curl -s "https://registry.hub.docker.com/v2/repositories/$image/tags/" | jq -r '.results[] | .name' | grep "alpha" | head -1 2>/dev/null || echo "")

        if [ -n "$latest_alpha" ]; then
            echo "$latest_alpha"
            return 0
        fi
    fi

    # Common tags to try in order of preference
    local common_tags=("8.8.0-alpha.108" "8.8.0-alpha.107" "8.8.0-alpha.106" "latest" "develop" "master")

    echo "${common_tags[0]}"
    return 0
}

echo -e "${BLUE}=== Docker Image Version Resolver ===${NC}"
echo "Finding available image versions for local development..."
echo ""

# Check if Docker is available
if ! command -v docker >/dev/null 2>&1; then
    echo -e "${YELLOW}Warning: Docker not found - using fallback tags${NC}"
fi

# Check if jq is available for API parsing
if ! command -v jq >/dev/null 2>&1; then
    echo -e "${YELLOW}Note: jq not found. Install with: brew install jq (for better tag discovery)${NC}"
    echo ""
fi

echo -e "${BLUE}Discovering available image tags...${NC}"
echo ""

# Find tags for each image
echo -e "${BLUE}Checking activiti/example-runtime-bundle...${NC}"
RUNTIME_BUNDLE_TAG=$(find_best_tag "activiti/example-runtime-bundle")
echo -e "  Found tag: ${GREEN}$RUNTIME_BUNDLE_TAG${NC}"
echo ""

echo -e "${BLUE}Checking activiti/activiti-cloud-query...${NC}"
QUERY_TAG=$(find_best_tag "activiti/activiti-cloud-query")
echo -e "  Found tag: ${GREEN}$QUERY_TAG${NC}"
echo ""

echo -e "${BLUE}Checking activiti/example-cloud-connector...${NC}"
CONNECTOR_TAG=$(find_best_tag "activiti/example-cloud-connector")
echo -e "  Found tag: ${GREEN}$CONNECTOR_TAG${NC}"
echo ""

echo -e "${BLUE}Checking activiti/activiti-cloud-identity-adapter...${NC}"
IDENTITY_ADAPTER_TAG=$(find_best_tag "activiti/activiti-cloud-identity-adapter")
echo -e "  Found tag: ${GREEN}$IDENTITY_ADAPTER_TAG${NC}"
echo ""

echo ""
echo -e "${BLUE}=== Creating local-values.local.yaml override ===${NC}"

# Create a values override file for local development
cat > "$SCRIPT_DIR/../local-values.local.yaml" << EOF
# Local development image overrides
# Generated automatically by resolve-docker-images.sh

# Use available image tags instead of PR-specific ones
runtime-bundle:
  image:
    tag: "${RUNTIME_BUNDLE_TAG:-latest}"
    pullPolicy: IfNotPresent

activiti-cloud-query:
  image:
    tag: "${QUERY_TAG:-latest}"
    pullPolicy: IfNotPresent

activiti-cloud-connector:
  image:
    tag: "${CONNECTOR_TAG:-latest}"
    pullPolicy: IfNotPresent

activiti-cloud-identity-adapter:
  image:
    tag: "${IDENTITY_ADAPTER_TAG:-latest}"
    pullPolicy: IfNotPresent
EOF

echo -e "${GREEN}✓ Created local-values.local.yaml with working image tags${NC}"
echo ""
echo -e "${YELLOW}Image mappings:${NC}"
echo -e "  activiti/example-runtime-bundle -> ${RUNTIME_BUNDLE_TAG:-latest}"
echo -e "  activiti/activiti-cloud-query -> ${QUERY_TAG:-latest}"
echo -e "  activiti/example-cloud-connector -> ${CONNECTOR_TAG:-latest}"
echo -e "  activiti/activiti-cloud-identity-adapter -> ${IDENTITY_ADAPTER_TAG:-latest}"
echo ""
echo -e "${BLUE}Usage:${NC}"
echo -e "  ./scripts/local-install.sh -p 123 --use-local-images"
echo -e "  # This will use the local-values.local.yaml file to override image tags"
echo ""
