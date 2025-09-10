#!/bin/bash

# Fix Makefile for local development
# This script patches the Makefile to work better in local environments

set -e

# Color codes
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}=== Fixing Makefile for Local Development ===${NC}"

# Check if we're in the right directory
if [[ ! -f "Makefile" ]]; then
    echo "Error: Makefile not found. Please run from repository root."
    exit 1
fi

# Create backup of original Makefile
if [[ ! -f "Makefile.backup" ]]; then
    echo -e "${YELLOW}Creating backup of original Makefile...${NC}"
    cp Makefile Makefile.backup
fi

# Create a local-friendly version of Makefile
echo -e "${YELLOW}Creating local development fixes...${NC}"

# Fix 1: Handle missing python command by using python3
sed -i.tmp 's/python -c/python3 -c/g' Makefile

# Fix 2: Add check for yq and install instructions
cat > Makefile.local << 'EOF'
# Local development overrides
check-tools:
	@command -v yq >/dev/null 2>&1 || { \
		echo "❌ yq is required but not installed."; \
		echo ""; \
		echo "To install yq:"; \
		echo "  macOS: brew install yq"; \
		echo "  Linux: sudo apt-get install yq"; \
		echo "  Or download from: https://github.com/mikefarah/yq/releases"; \
		echo ""; \
		exit 1; \
	}
	@command -v helm >/dev/null 2>&1 || { \
		echo "❌ helm is required but not installed."; \
		echo ""; \
		echo "To install helm:"; \
		echo "  macOS: brew install helm"; \
		echo "  Linux: curl https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3 | bash"; \
		echo ""; \
		exit 1; \
	}
	@command -v kubectl >/dev/null 2>&1 || { \
		echo "❌ kubectl is required but not installed."; \
		echo ""; \
		echo "Please install kubectl and configure it to connect to your cluster."; \
		echo ""; \
		exit 1; \
	}
	@echo "✅ All required tools are available"

local-install: check-tools
	@echo "=== Local Activiti Cloud Installation ==="
	@echo "PREVIEW_NAME: $(PREVIEW_NAME)"
	@echo "MESSAGING_BROKER: $(MESSAGING_BROKER)"
	@echo "MESSAGING_PARTITIONED: $(MESSAGING_PARTITIONED)"
	@echo "MESSAGING_DESTINATIONS: $(MESSAGING_DESTINATIONS)"
	@echo ""
	@$(MAKE) install

.PHONY: check-tools local-install

EOF

# Append local overrides to main Makefile
echo "" >> Makefile
echo "# Local development additions" >> Makefile
cat Makefile.local >> Makefile
rm Makefile.local

# Clean up temporary files
rm -f Makefile.tmp

echo -e "${GREEN}✅ Makefile patched for local development${NC}"
echo ""
echo -e "${YELLOW}New targets available:${NC}"
echo -e "  ${BLUE}make check-tools${NC}     - Check if required tools are installed"
echo -e "  ${BLUE}make local-install${NC}   - Install with tool checks"
echo ""
echo -e "${YELLOW}To restore original Makefile:${NC}"
echo -e "  ${BLUE}cp Makefile.backup Makefile${NC}"
