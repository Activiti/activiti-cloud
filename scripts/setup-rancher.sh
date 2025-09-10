#!/bin/bash

# Rancher CLI Setup Script for Activiti Cloud
# This script helps you connect kubectl to the Activiti cluster via Rancher

set -e

# Color codes
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${BLUE}=== Activiti Cloud Rancher Setup ===${NC}"
echo ""

# Check if rancher CLI is installed
check_rancher_cli() {
    if ! command -v rancher &> /dev/null; then
        echo -e "${RED}❌ Rancher CLI not found${NC}"
        echo ""
        echo -e "${YELLOW}Installing Rancher CLI...${NC}"

        if [[ "$OSTYPE" == "darwin"* ]]; then
            # macOS
            if command -v brew &> /dev/null; then
                brew install rancher-cli
            else
                echo -e "${RED}Error: Homebrew not found. Please install Homebrew first.${NC}"
                exit 1
            fi
        elif [[ "$OSTYPE" == "linux"* ]]; then
            # Linux
            echo "Downloading Rancher CLI for Linux..."
            curl -SsL "https://github.com/rancher/cli/releases/latest/download/rancher-linux-amd64-v2.8.5.tar.gz" | tar -xzC /tmp
            sudo mv /tmp/rancher-v2.8.5/rancher /usr/local/bin/
            sudo chmod +x /usr/local/bin/rancher
        else
            echo -e "${RED}Unsupported OS. Please install Rancher CLI manually.${NC}"
            echo "Visit: https://github.com/rancher/cli/releases"
            exit 1
        fi

        echo -e "${GREEN}✅ Rancher CLI installed${NC}"
    else
        echo -e "${GREEN}✅ Rancher CLI found${NC}"
    fi
}

# Check current kubectl context
check_kubectl_context() {
    if kubectl config current-context &> /dev/null; then
        current_context=$(kubectl config current-context)
        echo -e "${YELLOW}Current kubectl context: ${current_context}${NC}"

        if [[ "$current_context" == "activiti" ]]; then
            echo -e "${GREEN}✅ Already connected to activiti cluster${NC}"
            return 0
        fi
    else
        echo -e "${YELLOW}No kubectl context set${NC}"
    fi
    return 1
}

# Connect to Rancher
connect_rancher() {
    echo ""
    echo -e "${BLUE}=== Connecting to Rancher Server ===${NC}"
    echo ""
    echo -e "${YELLOW}You need to connect to the Rancher server to access the activiti cluster.${NC}"
    echo ""
    echo "Choose your authentication method:"
    echo "1) Bearer Token (recommended)"
    echo "2) Username/Password"
    echo "3) Skip (use existing connection)"
    echo ""
    read -p "Enter choice (1-3): " auth_choice

    case $auth_choice in
        1)
            echo ""
            echo -e "${YELLOW}You need a bearer token from the Rancher web interface.${NC}"
            echo "1. Go to https://rancher2.envalfresco.com/"
            echo "2. Login to your account"
            echo "3. Click on your user avatar (top right)"
            echo "4. Select 'Account & API Keys'"
            echo "5. Create a new API Key or copy existing one"
            echo ""
            read -p "Enter your bearer token: " -s token
            echo ""
            echo -e "${YELLOW}Connecting to Rancher...${NC}"
            rancher login https://rancher2.envalfresco.com/ --token "$token"
            ;;
        2)
            echo ""
            read -p "Enter Rancher username: " username
            read -p "Enter Rancher password: " -s password
            echo ""
            echo -e "${YELLOW}Connecting to Rancher...${NC}"
            rancher login https://rancher2.envalfresco.com/ --username "$username" --password "$password"
            ;;
        3)
            echo -e "${YELLOW}Skipping Rancher login (assuming already connected)${NC}"
            ;;
        *)
            echo -e "${RED}Invalid choice${NC}"
            exit 1
            ;;
    esac
}

# Switch to activiti cluster
switch_cluster() {
    echo ""
    echo -e "${BLUE}=== Switching to Activiti Cluster ===${NC}"
    echo ""
    echo -e "${YELLOW}Available contexts:${NC}"
    rancher context ls
    echo ""
    echo -e "${YELLOW}Switching to activiti cluster...${NC}"

    # Try to switch automatically
    if rancher context switch activiti &> /dev/null; then
        echo -e "${GREEN}✅ Successfully switched to activiti cluster${NC}"
    else
        echo -e "${YELLOW}Automatic switch failed. Please select manually:${NC}"
        rancher context switch
    fi
}

# Verify connection
verify_connection() {
    echo ""
    echo -e "${BLUE}=== Verifying Connection ===${NC}"
    echo ""

    if kubectl cluster-info &> /dev/null; then
        echo -e "${GREEN}✅ kubectl connected successfully${NC}"
        echo ""
        echo -e "${YELLOW}Cluster Info:${NC}"
        kubectl cluster-info
        echo ""
        echo -e "${YELLOW}Current Context:${NC}"
        kubectl config current-context
        echo ""
        echo -e "${YELLOW}Available Namespaces:${NC}"
        kubectl get namespaces | head -10
    else
        echo -e "${RED}❌ kubectl connection failed${NC}"
        echo ""
        echo -e "${YELLOW}Troubleshooting steps:${NC}"
        echo "1. Check your Rancher credentials"
        echo "2. Verify you have access to the activiti cluster"
        echo "3. Try switching context again: rancher context switch"
        exit 1
    fi
}

# Main execution
main() {
    check_rancher_cli

    if check_kubectl_context; then
        echo ""
        read -p "Do you want to reconfigure? (y/N): " reconfigure
        if [[ ! "$reconfigure" =~ ^[Yy]$ ]]; then
            verify_connection
            echo -e "${GREEN}=== Setup Complete! ===${NC}"
            return
        fi
    fi

    connect_rancher
    switch_cluster
    verify_connection

    echo ""
    echo -e "${GREEN}=== Setup Complete! ===${NC}"
    echo ""
    echo -e "${YELLOW}You can now run the installation script:${NC}"
    echo -e "${BLUE}./scripts/local-install.sh -p 123${NC}"
}

# Alternative local cluster setup
setup_local_cluster() {
    echo ""
    echo -e "${BLUE}=== Setting Up Local Cluster ===${NC}"
    echo ""
    echo -e "${YELLOW}If you don't have access to the Rancher cluster, you can set up a local one:${NC}"
    echo ""
    echo "1) Minikube (recommended for development)"
    echo "2) Kind (lightweight)"
    echo "3) Skip"
    echo ""
    read -p "Choose option (1-3): " local_choice

    case $local_choice in
        1)
            if ! command -v minikube &> /dev/null; then
                echo -e "${YELLOW}Installing minikube...${NC}"
                if [[ "$OSTYPE" == "darwin"* ]]; then
                    brew install minikube
                else
                    echo "Please install minikube manually: https://minikube.sigs.k8s.io/docs/start/"
                    exit 1
                fi
            fi

            echo -e "${YELLOW}Starting minikube cluster...${NC}"
            minikube start --profile activiti --cpus 4 --memory 8192 --disk-size 50GB
            kubectl config use-context activiti
            echo -e "${GREEN}✅ Local minikube cluster ready${NC}"
            ;;
        2)
            if ! command -v kind &> /dev/null; then
                echo -e "${YELLOW}Installing kind...${NC}"
                if [[ "$OSTYPE" == "darwin"* ]]; then
                    brew install kind
                else
                    echo "Please install kind manually: https://kind.sigs.k8s.io/docs/user/quick-start/"
                    exit 1
                fi
            fi

            echo -e "${YELLOW}Creating kind cluster...${NC}"
            kind create cluster --name activiti
            kubectl config use-context kind-activiti
            echo -e "${GREEN}✅ Local kind cluster ready${NC}"
            ;;
        3)
            echo -e "${YELLOW}Skipping local cluster setup${NC}"
            return 1
            ;;
    esac

    verify_connection
    return 0
}

# Check if user wants local or remote cluster
echo -e "${YELLOW}Do you want to connect to:${NC}"
echo "1) Remote Rancher cluster (activiti.envalfresco.com)"
echo "2) Local development cluster"
echo ""
read -p "Choose option (1-2): " cluster_choice

case $cluster_choice in
    1)
        main
        ;;
    2)
        if setup_local_cluster; then
            echo -e "${GREEN}=== Local Cluster Setup Complete! ===${NC}"
            echo ""
            echo -e "${YELLOW}You can now run the installation script:${NC}"
            echo -e "${BLUE}./scripts/local-install.sh -p 123${NC}"
        fi
        ;;
    *)
        echo -e "${RED}Invalid choice${NC}"
        exit 1
        ;;
esac
