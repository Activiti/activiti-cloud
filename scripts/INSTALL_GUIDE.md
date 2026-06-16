# Local Activiti Cloud Installation Guide

This guide helps you set up and install Activiti Cloud locally with complete automation for DNS resolution, authentication, and testing.

## ✅ Current Working Solution

**Great news!** We have a fully working local development setup that handles everything automatically:

- ✅ **DNS Resolution** - Port forwarding + /etc/hosts automation (no VPN needed)
- ✅ **Authentication** - External Keycloak with proper JWT configuration
- ✅ **Docker Images** - Working tags via local-values.local.yaml integration
- ✅ **Playwright Tests** - Automated .env generation and test configuration
- ✅ **Multi-Cluster** - Parameterized for any Rancher-managed cluster

## Quick Start (Recommended)

### One-Command Setup

Switch context to desired context:

```bash
rancher context switch
```

Update kubeconfig:

```bash
./scripts/fix-kubectl-config.sh <context>
```

```bash
# Install npm dependencies and Playwright browsers (one-time setup)
npm install
npx playwright install --with-deps

# Complete automated setup - handles everything
./scripts/local-install.sh -n 123

# Access immediately via localhost
open http://localhost:8080

# Run Playwright tests
npm test
```

That's it! The script handles cluster connection, deployment, DNS configuration, authentication setup, and test preparation automatically.

## Prerequisites

Before running the installation, ensure you have:

### Required Tools

```bash
# Check if you have the required tools
kubectl version --client  # Kubernetes CLI
helm version              # Helm package manager
yq --version             # YAML processor
python3 --version        # Python 3
rancher --version        # Rancher CLI (for cluster access)
```

### Install Missing Tools

**macOS (using Homebrew):**

```bash
brew install kubectl helm yq python@3.11 rancher-cli
```

**Linux (Ubuntu/Debian):**

```bash
# kubectl
curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"
sudo install -o root -g root -m 0755 kubectl /usr/local/bin/kubectl

# helm
curl https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3 | bash

# yq
sudo wget -qO /usr/local/bin/yq https://github.com/mikefarah/yq/releases/latest/download/yq_linux_amd64
sudo chmod +x /usr/local/bin/yq

# rancher CLI
curl -sL "https://github.com/rancher/cli/releases/latest/download/rancher-linux-amd64-v2.7.0.tar.gz" | tar xz
sudo mv ./rancher-v2.7.0/rancher /usr/local/bin/
```

### Cluster Access

You need access to a Rancher-managed Kubernetes cluster. The script supports any cluster, not just the default `activiti`.

## Installation Process

### Step 1: Basic Installation

```bash
./scripts/local-install.sh -n 123

# For different cluster
./scripts/local-install.sh -n 123 -c my-cluster-name

# To see what would happen first
./scripts/local-install.sh --dry-run -n 123
```

### Step 2: What Happens Automatically

The script performs these operations:

1. **Cluster Configuration**
   - Connects to Rancher and switches kubectl context
   - Validates cluster access and permissions
   - Confirms namespace creation capabilities

2. **Environment Setup**
   - Generates PREVIEW_NAME: `pr-123-rabbit-n-d`
   - Creates local-values.local.yaml with working Docker image tags (gitignored)
   - Sets up all required environment variables

3. **Kubernetes Deployment**
   - Creates namespace with proper labels
   - Deploys Activiti Cloud using Helm with local-values.local.yaml
   - Patches all deployments with external Keycloak configuration

4. **Local Access Configuration**
   - Automatically adds entries to /etc/hosts (requires sudo password)
   - Sets up port forwarding: localhost:8080 → traefik
   - Configures DNS resolution for `pr-123-rabbit-n-d.activiti-hackathon.envalfresco.com`

5. **Authentication Setup**
   - Configures external Keycloak URL: `https://activiti-hackathon.envalfresco.com/auth`
   - Sets realm: `alfresco`
   - Patches all services with ACT_KEYCLOAK_URL and ACT_KEYCLOAK_REALM
   - Validates JWT configuration across deployments

6. **Playwright Configuration**
   - Generates .env file with correct configuration
   - Sets HTTP protocol for localhost:8080
   - Configures SSO_HOST for external Keycloak authentication

### Step 3: Immediate Access

After installation completes:

```bash
# Access gateway via localhost
curl http://localhost:8080/rb/actuator/health

# Access with proper Host header
curl -H "Host: pr-123-rabbit-n-d.activiti-hackathon.envalfresco.com" \
     http://localhost:8080/rb/actuator/health

# Run Playwright tests from the repository root
npm test
```

## Configuration Options and Advanced Usage

### Different Brokers

```bash
# Use Kafka instead of default RabbitMQ
./scripts/local-install.sh -n 123 -b kafka

# Use partitioned messaging
./scripts/local-install.sh -n 123 -pt true

# Use override destinations
./scripts/local-install.sh -n 123 -d override

# Combine all options
./scripts/local-install.sh -n 123 -b kafka -pt true -d override
```

### Different Clusters

```bash
# Use different cluster than default activiti-hackathon
./scripts/local-install.sh -n 123 -c my-cluster-name

# This creates: pr-123-rabbit-n-d.my-cluster-name.envalfresco.com
```

### Testing and Validation

```bash
# Dry run to see what would happen
./scripts/local-install.sh --dry-run -n 123

# Check deployment status after installation
kubectl get pods -n pr-123-rabbit-n-d
kubectl get services -n pr-123-rabbit-n-d

# Validate health endpoints
curl http://localhost:8080/rb/actuator/health
curl http://localhost:8080/query/actuator/health
```

## Generated Resources

After successful installation, you'll have:

### Kubernetes Resources

- **Namespace**: `pr-123-rabbit-n-d` (format: pr-{number}-{broker}-{partition}-{destination})
- **Services**: Gateway, Runtime Bundle, Query, Audit, Notifications GraphQL
- **Deployments**: All services with proper Keycloak configuration
- **ConfigMaps**: Environment-specific configuration
- **Secrets**: Generated authentication secrets

### Local Configuration

- **DNS Resolution**: /etc/hosts entries for `*.{cluster}.envalfresco.com`
- **Port Forwarding**: localhost:8080 → traefik:80
- **Environment Files**: .env for Playwright tests
- **Working Images**: local-values.local.yaml with tested tags

### Authentication Setup

- **External Keycloak**: `https://{cluster}.envalfresco.com/auth`
- **Realm**: `alfresco`
- **Client**: `activiti-keycloak`
- **JWT Validation**: Configured across all services

## Cleanup and Management

### Remove Deployment

```bash
# Delete the namespace and all resources
kubectl delete namespace pr-123-rabbit-n-d

# Clean up /etc/hosts entries (manual)
sudo vi /etc/hosts
# Remove lines containing pr-123-rabbit-n-d

# Stop port forwarding
pkill -f "kubectl.*port-forward"
```

### Multiple Environments

```bash
# Run multiple environments simultaneously
./scripts/local-install.sh -n 123  # Creates pr-123-rabbit-n-d
./scripts/local-install.sh -n 456  # Creates pr-456-rabbit-n-d

# Each has its own namespace but shares localhost:8080
# Access via proper Host headers or /etc/hosts entries
```

## Troubleshooting

### Common Issues and Solutions

1. **"kubectl not connected" or authentication errors**

   ```bash
   # Fix cluster configuration
   ./scripts/fix-kubectl-config.sh
   kubectl cluster-info

   # Or use specific cluster
   ./scripts/fix-kubectl-config.sh my-cluster-name
   ```

2. **"Permission denied" for /etc/hosts**

   ```bash
   # The script needs sudo access to modify /etc/hosts
   # Enter your password when prompted
   sudo echo "Testing sudo access"
   ```

3. **Port forwarding issues**

   ```bash
   # Kill existing port forwards
   pkill -f "kubectl.*port-forward"

   # Check for processes using port 8080
   lsof -i :8080

   # Start port forwarding manually to Traefik
   kubectl port-forward -n traefik svc/traefik 8080:80
   ```

4. **DNS resolution not working**

   ```bash
   # Check /etc/hosts entries
   grep "envalfresco.com" /etc/hosts

   # Should show entries like:
   # 127.0.0.1 pr-123-rabbit-n-d.activiti-hackathon.envalfresco.com

   # Test DNS resolution
   nslookup pr-123-rabbit-n-d.activiti-hackathon.envalfresco.com
   ```

5. **Authentication/JWT errors**

   ```bash
   # Check Keycloak configuration
   kubectl get configmap -n pr-123-rabbit-n-d
   kubectl describe deployment runtime-bundle -n pr-123-rabbit-n-d

   # Look for ACT_KEYCLOAK_URL and ACT_KEYCLOAK_REALM
   ```

6. **Playwright tests failing**

   ```bash
   # Check .env file generation
   cat activiti-cloud-acceptance-tests-playwright/.env

   # Should contain:
   # GATEWAY_HOST=http://localhost:8080
   # SSO_HOST=https://activiti-hackathon.envalfresco.com/auth
   ```

### Advanced Debugging

```bash
# Check script prerequisites
./scripts/local-install.sh --help

# Run with verbose output (if supported)
bash -x ./scripts/local-install.sh -n 123

# Check individual components
kubectl get pods -n pr-123-rabbit-n-d -o wide
kubectl logs deployment/runtime-bundle -n pr-123-rabbit-n-d
kubectl describe ingress -n pr-123-rabbit-n-d
```

## Integration with Development Workflow

### With Makefile

The installation generates environment variables compatible with the existing Makefile:

```bash
# After running local-install.sh
export PREVIEW_NAME=pr-123-rabbit-n-d
make test
make delete
```

### With GitHub Actions

The local environment mirrors the GitHub Actions setup:

- Same PREVIEW_NAME format
- Same Docker image tags (via local-values.local.yaml)
- Same authentication configuration
- Same service endpoints and routing

### With IDE Integration

Most IDEs can use the generated configuration:

- IntelliJ IDEA: Use .env files for run configurations
- VS Code: Use .env files with plugins
- Terminal: Source environment variables directly

## Security Considerations

- **No Hardcoded Secrets**: All authentication uses external Keycloak
- **Namespace Isolation**: Each deployment is isolated in its own namespace
- **Local Only**: Port forwarding only binds to localhost
- **Temporary Access**: /etc/hosts entries are for development only
- **Clean State**: Scripts clean up previous deployments before creating new ones

## Best Practices

1. **Use Consistent PR Numbers**: Use the same number for related development work
2. **Clean Up Regularly**: Remove old namespaces to free cluster resources
3. **Test Configuration**: Always use dry-run first for new configurations
4. **Monitor Resources**: Check cluster capacity before creating multiple environments
5. **Update Documentation**: Keep this guide updated as the workflow evolves

---

This represents the current working state of our local development workflow, with all authentication issues resolved and complete automation for reliable deployments.
