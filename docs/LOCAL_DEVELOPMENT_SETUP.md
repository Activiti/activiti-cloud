# 🚀 Local Development Setup Guide

This guide helps you set up Activiti Cloud for local development and testing.

## 🎯 Quick Start (One Command)

```bash
./scripts/local-install.sh -n my-environment
```

This single command:

1. 🔧 **Configures cluster connection** (using rancher CLI if needed)
2. 🐳 **Ensures reliable Docker images** (creates/uses `local-values.local.yaml`, gitignored)
3. ⚓ **Deploys Activiti Cloud** to Kubernetes
4. 🧪 **Generates .env file** for Playwright tests
5. 📋 **Shows next steps** for testing

## 📋 Prerequisites

### Required Tools

- **kubectl** - Kubernetes CLI
- **helm** (v3+) - Kubernetes package manager
- **yq** - YAML processor
- **python3** - For version parsing
- **rancher CLI** (optional) - For cluster configuration

### Install Missing Tools (macOS)

```bash
# Install kubectl
brew install kubectl

# Install helm
brew install helm

# Install yq
brew install yq

# Install rancher CLI (for cluster configuration)
brew install rancher-cli
```

## 🔧 Cluster Configuration

### Option 1: Automatic (Recommended)

The script will automatically detect and configure your cluster connection:

```bash
./scripts/local-install.sh -n my-test
```

### Option 2: Manual Cluster Selection

```bash
# Connect to specific cluster
./scripts/local-install.sh -n my-test -c activiti-hackathon

# Or configure cluster first, then deploy
./scripts/fix-kubectl-config.sh activiti-hackathon
./scripts/local-install.sh -n my-test
```

### Option 3: Manual kubectl Configuration

If you prefer to configure kubectl manually:

```bash
# Check available contexts
kubectl config get-contexts

# Switch to desired context
kubectl config use-context your-context

# Then run the install
./scripts/local-install.sh -n my-test
```

## 🚀 Deployment Examples

### Basic Deployment

```bash
./scripts/local-install.sh -n michal-test
```

### Advanced Configuration

```bash
# Kafka with partitioning and custom destinations
./scripts/local-install.sh -n feature-xyz -b kafka -pt true -d override

# Custom cluster and version
./scripts/local-install.sh -n my-env -c activiti-community -v 1.2.3-SNAPSHOT

# Preview what would happen (dry run)
./scripts/local-install.sh -n test-env --dry-run
```

### Use Generated Docker Images (Advanced)

```bash
# Skip local-values.local.yaml and use generated image versions
./scripts/local-install.sh -n test-env --no-local-images
```

## 🧪 Testing with Playwright

After deployment, the script automatically:

1. 📝 **Generates .env file** with correct configuration
2. 🌐 **Shows /etc/hosts entries** to add
3. 🔀 **Provides port forwarding command**

### Complete Testing Setup

1. **Add to /etc/hosts** (as shown in script output):

   ```bash
   echo "127.0.0.1 gateway-pr-your-env-rabbit-n-d.activiti.envalfresco.com" | sudo tee -a /etc/hosts
   echo "127.0.0.1 identity-pr-your-env-rabbit-n-d.activiti.envalfresco.com" | sudo tee -a /etc/hosts
   ```

2. **Start port forwarding**:

   ```bash
   kubectl port-forward svc/ingress-nginx-controller 8080:80 -n default
   ```

3. **Run tests**:
   ```bash
   cd activiti-cloud-acceptance-tests-playwright
   npm test
   ```

## 📁 Generated Files

### local-values.local.yaml

- Contains working Docker image tags (generated, gitignored)
- Automatically created if missing
- Ensures reliable deployments
- Located at repository root

### .env file

- Playwright test configuration
- Located at `activiti-cloud-acceptance-tests-playwright/.env`
- Automatically generated with correct values
- Ready to use for testing

## 🔍 Troubleshooting

### kubectl Connection Issues

```bash
# Check current context
kubectl config current-context

# List available contexts
kubectl config get-contexts

# Configure using rancher CLI
./scripts/fix-kubectl-config.sh activiti

# Test connection
kubectl cluster-info
```

### Deployment Issues

```bash
# Check deployment status
kubectl get pods -n pr-your-env-rabbit-n-d

# Check services
kubectl get services -n pr-your-env-rabbit-n-d

# Check ingress
kubectl get ingress -n pr-your-env-rabbit-n-d

# View logs
kubectl logs -n pr-your-env-rabbit-n-d deployment/runtime-bundle
```

### Port Forwarding Issues

```bash
# Check if port is in use
lsof -i :8080

# Kill existing port forwards
pkill -f "kubectl port-forward"

# Test connectivity
curl -H "Host: your-gateway-host" http://localhost:8080/
```

## 🧹 Cleanup

### Remove Deployment

```bash
make delete PREVIEW_NAME=pr-your-env-rabbit-n-d
```

### Clean Everything

```bash
# Remove namespace
kubectl delete namespace pr-your-env-rabbit-n-d

# Remove from /etc/hosts (manual)
sudo nano /etc/hosts

# Stop port forwarding
pkill -f "kubectl port-forward"
```

## 🎛️ Configuration Reference

### Environment Variables

- `PREVIEW_NAME` - Generated namespace name
- `CLUSTER_NAME` - Target cluster
- `MESSAGING_BROKER` - rabbitmq|kafka
- `MESSAGING_PARTITIONED` - true|false
- `MESSAGING_DESTINATIONS` - default|override

### Generated Namespace Pattern

```
pr-{environment-name}-{broker}-{partitioned}-{destinations}
```

Examples:

- `pr-michal-test-rabbit-n-d` (rabbitmq, non-partitioned, default)
- `pr-feature-xyz-kafka-p-o` (kafka, partitioned, override)

## 📞 Getting Help

### Script Help

```bash
./scripts/local-install.sh --help
./scripts/fix-kubectl-config.sh --help
```

### Common Issues

1. **"Environment name is required"** - Add `-n your-name`
2. **"kubectl not connected"** - Run `./scripts/fix-kubectl-config.sh`
3. **"Helm not found"** - Install with `brew install helm`
4. **Port forwarding fails** - Check if port 8080 is available

### Debug Mode

```bash
# Dry run to see what would happen
./scripts/local-install.sh -n test --dry-run

# Check generated values
kubectl get configmap -n pr-your-env-rabbit-n-d
```

---

**🎉 Happy coding! Your Activiti Cloud development environment is ready!**
