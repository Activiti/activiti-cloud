# Activiti Cloud Scripts

This directory contains scripts for complete Activiti Cloud environment setup and management, replicating the logic used in GitHub Actions.

## 🚀 Main Script: `setup-environment.sh`

**Complete environment setup tool that consolidates all functionality into one script.**

**Usage:**

```bash
./scripts/setup-environment.sh [options]
```

**Options:**

- `-n, --name <name>` - Environment name (e.g., test-123, local-dev)
- `-r, --run <number>` - GitHub run number (e.g., 456789)
- `-b, --broker <broker>` - Messaging broker: `rabbitmq`|`kafka` (default: rabbitmq)
- `-pt, --partitioned <bool>` - Partitioned: `true`|`false` (default: false)
- `-d, --destinations <type>` - Destinations: `default`|`override` (default: default)
- `--mode <mode>` - Setup mode: `full`|`env-only`|`test-only`|`playwright`
- `--dry-run` - Show what would be executed without running
- `-h, --help` - Show help message

**Examples:**

```bash
# Complete setup for environment "test-123"
./scripts/setup-environment.sh -n test-123

# Just generate environment variables
./scripts/setup-environment.sh -n local-dev --mode env-only

# Setup for Playwright testing
./scripts/setup-environment.sh -n playwright-test --mode playwright

# Test existing deployment
./scripts/setup-environment.sh -n test-123 --mode test-only

# Advanced configuration
./scripts/setup-environment.sh -n kafka-test -b kafka -pt true -d override
```

## Setup Modes

### 1. **`--mode full`** (Default)

Complete setup with installation, host configuration, and health checks.

- ✅ Generate environment variables
- ✅ Run complete installation (uses `local-install.sh`)
- ✅ Setup /etc/hosts entries
- ✅ Run health checks
- ✅ Provide access instructions

### 2. **`--mode env-only`**

Only generate and display environment variables.

- ✅ Generate PREVIEW_NAME and all related variables
- ✅ Display export commands for manual use
- ✅ Compatible with existing workflows

### 3. **`--mode test-only`**

Setup access and test existing deployment.

- ✅ Setup /etc/hosts entries
- ✅ Run comprehensive health checks
- ✅ Optional port forwarding setup
- ✅ Verify service endpoints

### 4. **`--mode playwright`**

Complete setup optimized for Playwright tests.

- ✅ Full installation (if needed)
- ✅ Setup /etc/hosts entries
- ✅ Create Playwright .env configuration
- ✅ Install dependencies
- ✅ Run health checks

## Supporting Scripts

### `local-install.sh` - **Main Installation Script**

**Complete local installation that replicates the GitHub Actions workflow.**

**Features:**

- ✅ Prerequisites checking (kubectl, helm, yq, python3)
- ✅ Environment variable generation
- ✅ Namespace cleanup and creation
- ✅ Full Helm chart installation
- ✅ Dry-run support
- ✅ Progress reporting

**Usage:**

```bash
./scripts/local-install.sh -p 123
./scripts/local-install.sh -p 456 -b kafka -pt true -d override
./scripts/local-install.sh --dry-run -p 123
```

### `check-deployment-status.sh` - **Deployment Status Checker**

Check pod status and service availability for existing deployments.

**Usage:**

```bash
./scripts/check-deployment-status.sh -p 123
```

### Utility Scripts

- `fix-makefile.sh` - Patches the Makefile for local development compatibility
- `fix-kubectl-config.sh` - kubectl configuration fixes
- `setup-rancher.sh` - Rancher CLI setup
- `kubectl-wrapper.sh` - kubectl wrapper for Rancher
- `resolve-docker-images.sh` - Docker image resolution

## Generated Environment Variables

The scripts generate the following environment variables:

- `PREVIEW_NAME` - The main preview environment name
- `MESSAGING_BROKER` - The messaging broker configuration
- `MESSAGING_PARTITIONED` - The partitioning configuration
- `MESSAGING_DESTINATIONS` - The destinations configuration
- `GLOBAL_GATEWAY_DOMAIN` - The global gateway domain
- `GATEWAY_HOST` - The gateway host URL
- `SSO_HOST` - The SSO host URL

## PREVIEW_NAME Format

The `PREVIEW_NAME` follows this pattern:

```
{type}-{number}-{broker}-{partition}-{destination}
```

Where:

- **type**: `pr` (for pull requests) or `gh` (for GitHub runs)
- **number**: PR number or GitHub run number
- **broker**: First 6 characters of broker name (`rabbit` or `kafka`)
- **partition**: First character of partitioning (`p` for partitioned, `n` for non-partitioned)
- **destination**: First character of destinations (`d` for default, `o` for override)

**Examples:**

- `pr-123-rabbit-n-d` - PR #123, RabbitMQ, non-partitioned, default destinations
- `pr-456-kafka-p-o` - PR #456, Kafka, partitioned, override destinations
- `gh-789012-rabbit-p-d` - GitHub run #789012, RabbitMQ, partitioned, default destinations

## Use Cases

### Complete Environment Setup

```bash
# Setup everything for environment "test-123"
./scripts/setup-environment.sh -n test-123

# Advanced configuration
./scripts/setup-environment.sh -n kafka-test -b kafka -pt true -d override
```

### Environment Variables Only

```bash
# Generate variables and show export commands
./scripts/setup-environment.sh -n local-dev --mode env-only

# Use in current shell
source <(./scripts/setup-environment.sh -n local-dev --mode env-only 2>/dev/null | grep '^export')
```

### Playwright Testing

```bash
# Complete Playwright setup
./scripts/setup-environment.sh -n playwright-test --mode playwright

# Then run tests
cd activiti-cloud-acceptance-tests-playwright
npm test
```

### Testing Existing Deployment

```bash
# Test and setup access to existing deployment
./scripts/setup-environment.sh -n test-123 --mode test-only

# Check detailed deployment status
./scripts/check-deployment-status.sh -p 123
```

## Migration from Old Scripts

The new consolidated script replaces these previous scripts:

- `generate-preview-name.sh` → `setup-environment.sh --mode env-only`
- `quick-preview-env.sh` → `setup-environment.sh --mode env-only`
- `setup-playwright-local.sh` → `setup-environment.sh --mode playwright`
- `setup-local-access.sh` → `setup-environment.sh --mode test-only`
- `health-check-enhanced.sh` → Integrated into all modes
- `test-health-checks-local.sh` → Integrated into all modes

## Integration with Existing Tools

These scripts generate the same `PREVIEW_NAME` format as the GitHub Actions workflow, ensuring consistency between local and CI/CD environments.

The generated environment variables can be used with:

- Kubernetes deployments (`kubectl` commands)
- Helm charts (`helm install/upgrade`)
- Makefile targets
- Local testing scripts
- API endpoint configuration
