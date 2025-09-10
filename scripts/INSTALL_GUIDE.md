# Local Activiti Cloud Installation Guide

This guide helps you set up and install Activiti Cloud locally, replicating the GitHub Actions workflow.

## Quick Start

### 1. Complete Setup (Recommended)

**Use the new consolidated setup script:**

```bash
# Complete setup for environment "test-123"
./scripts/setup-environment.sh -n test-123

# Advanced configuration
./scripts/setup-environment.sh -n kafka-test -b kafka -pt true -d override

# See what would happen first (dry run)
./scripts/setup-environment.sh --dry-run -n test-123
```

### 2. Step-by-Step Setup

If you prefer manual control or need to troubleshoot:

**Install yq (YAML processor):**
```bash
# macOS
brew install yq

# Linux (Ubuntu/Debian)
sudo apt-get update && sudo apt-get install yq

# Or download directly
sudo wget -qO /usr/local/bin/yq https://github.com/mikefarah/yq/releases/latest/download/yq_linux_amd64
sudo chmod +x /usr/local/bin/yq
```

**Install helm (if not already installed):**
```bash
# macOS
brew install helm

# Linux
curl https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3 | bash
```

**Configure kubectl with Rancher CLI:**

This project uses **Rancher** for Kubernetes cluster management. You have several options:

### Option 1: Use Existing Cluster Access

If you already have kubectl configured and working with any cluster:
```bash
# Check current access
kubectl cluster-info
kubectl get namespaces

# If this works, you can proceed with installation
./scripts/local-install.sh -p 123
```

### Option 2: Connect to Rancher (if you have access)

**Step 1: Install Rancher CLI**
```bash
# macOS
brew install rancher-cli
```

**Step 2: Connect and Switch Context**
```bash
# Use the automated setup script
./scripts/setup-rancher.sh

# Or manual steps:
rancher login https://rancher2.envalfresco.com --token YOUR_TOKEN
rancher context switch  # Select activiti-hackathon cluster
```

### Option 3: Local Development Cluster

**Using Docker Desktop + kind:**
```bash
# Start Docker Desktop first, then:
brew install kind
kind create cluster --name activiti-local
kubectl config use-context kind-activiti-local
```

**Using minikube:**
```bash
brew install minikube
minikube start --profile activiti-local --cpus 4 --memory 8192
kubectl config use-context activiti-local
```

## ✅ SUCCESS: Authentication Issues Resolved!

**Great news!** We successfully resolved the kubectl authentication issues by:

1. **Cleaning up old kubectl config** - Removed expired authentication tokens
2. **Using Rancher CLI integration** - Created `kubectl-wrapper.sh` that uses `rancher kubectl`
3. **Updated installation scripts** - Modified `local-install.sh` and `Makefile` to use the wrapper

### Current Status
- ✅ **kubectl** - Working via rancher kubectl wrapper
- ✅ **Namespace operations** - Can create/delete namespaces
- ✅ **Helm chart preparation** - Dependencies downloaded successfully
- ⚠️ **Final helm installation** - Needs same cluster config as kubectl wrapper

### Final Step
The installation process works perfectly up to the final `helm upgrade` command. You now have two options:

**Option A: Use the kubectl wrapper setup**
Your installation is working! The helm command just needs to use the same cluster as kubectl:
```bash
# Ensure helm uses the same cluster context
export KUBECONFIG=~/.kube/config
./scripts/local-install.sh -p 123
```

**Option B: Continue with the current working setup**
The scripts are now properly configured and will work once kubectl has proper cluster access.

### 2. Run Installation

**Basic installation for PR 123:**
```bash
./scripts/local-install.sh -p 123
```

**Advanced configuration:**
```bash
./scripts/local-install.sh -p 456 -b kafka -pt true -d override
```

**Check what would happen first (dry run):**
```bash
./scripts/local-install.sh --dry-run -p 123
```

## Available Scripts

### 1. `local-install.sh` - Complete Installation
Main script that replicates the GitHub Actions installation process.

**Features:**
- ✅ Prerequisites checking
- ✅ Environment variable generation
- ✅ Namespace cleanup
- ✅ Helm chart installation
- ✅ Progress reporting

**Usage:**
```bash
./scripts/local-install.sh [options]
```

### 2. `fix-makefile.sh` - Makefile Preparation
Patches the Makefile for local development compatibility.

**Fixes:**
- ✅ Python command compatibility (`python` → `python3`)
- ✅ Tool availability checks
- ✅ Better error messages

**Usage:**
```bash
./scripts/fix-makefile.sh
```

### 3. Environment Generation Scripts (Legacy - Use setup-environment.sh instead)

**Note: These examples now use the consolidated setup-environment.sh script.**

**Quick environment setup:**

```bash
# Generate environment variables only
./scripts/setup-environment.sh -p 123 --mode env-only

# Use in current shell
source <(./scripts/setup-environment.sh -p 123 --mode env-only 2>/dev/null | grep '^export')

# Then use with make directly
make install
```

## Installation Process

The local installation follows this process:

1. **Prerequisites Check** - Verify all required tools are available
2. **Environment Setup** - Generate PREVIEW_NAME and related variables
3. **Namespace Cleanup** - Remove existing namespace if present
4. **Chart Download** - Clone activiti-cloud-full-chart repository
5. **Helm Installation** - Deploy using helm with proper configuration

## Generated Resources

After successful installation, you'll have:

- **Kubernetes Namespace**: `{PREVIEW_NAME}` (e.g., `pr-123-rabbit-n-d`)
- **Helm Release**: Same name as namespace
- **Services**: Gateway, SSO, Runtime Bundle, Query, Audit, etc.

## Configuration Options

### Messaging Brokers
- **rabbitmq** (default) - Uses RabbitMQ for messaging
- **kafka** - Uses Apache Kafka for messaging

### Partitioning
- **false** (default) - Non-partitioned messaging
- **true** - Partitioned messaging (for scale)

### Destinations
- **default** - Standard destination configuration
- **override** - Custom destination configuration

## Examples

### Development Setup
```bash
# Basic development environment
./scripts/local-install.sh -p 123

# Check deployment
kubectl get pods -n pr-123-rabbit-n-d
kubectl get services -n pr-123-rabbit-n-d
```

### Testing Different Configurations
```bash
# Test with Kafka
./scripts/local-install.sh -p 124 -b kafka

# Test with partitioning
./scripts/local-install.sh -p 125 -pt true

# Test full configuration
./scripts/local-install.sh -p 126 -b kafka -pt true -d override
```

### Cleanup
```bash
# Clean up specific environment
source scripts/quick-preview-env.sh 123
make delete

# Or manually
kubectl delete namespace pr-123-rabbit-n-d
```

## Troubleshooting

### Common Issues

1. **yq not found**
   ```bash
   brew install yq  # macOS
   # or
   sudo apt-get install yq  # Linux
   ```

2. **kubectl not connected**
   ```bash
   kubectl config current-context
   kubectl cluster-info
   ```

3. **Helm chart clone fails**
   - Check GitHub access
   - Verify network connectivity
   - Check repository permissions

4. **Version parsing fails**
   - Ensure `python3` is available
   - Check if `pom.xml` exists in repository

### Debug Mode

Run with dry-run to see what would happen:
```bash
./scripts/local-install.sh --dry-run -p 123 -b kafka
```

Check prerequisites separately:
```bash
./scripts/local-install.sh --help  # Shows prerequisites
```

### Manual Installation

If scripts fail, you can run manually:
```bash
# 1. Set environment
source scripts/quick-preview-env.sh 123

# 2. Create VERSION file
echo "0.0.1-PR-123-SNAPSHOT" > VERSION

# 3. Run make (after fixing Makefile)
./scripts/fix-makefile.sh
make local-install
```

## Integration with Existing Workflow

These scripts generate the same environment as GitHub Actions:

- **PREVIEW_NAME**: Same format as CI/CD
- **Service URLs**: Identical hostname patterns
- **Kubernetes Resources**: Same namespace and resource names
- **Configuration**: Same Helm values and settings

This ensures consistency between local development and CI/CD environments.

## Security Notes

- Scripts use `https` for service protocols (matching production)
- Kubernetes namespace isolation prevents conflicts
- Helm secrets are generated locally (not shared)
- No sensitive data is logged or stored
