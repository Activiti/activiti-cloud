# Activiti Cloud Playwright Tests

This directory contains Playwright-based acceptance tests for the Activiti Cloud identity management functionality, migrated from the original Java/Serenity framework.

## Prerequisites

Before running the tests, ensure you have the following:

### Required Tools

- **Node.js** (v18 or higher)
- **npm** (comes with Node.js)
- **kubectl** (Kubernetes CLI tool)
- **Access to a Kubernetes cluster** with Activiti Cloud deployed

### Environment Setup

1. **Install dependencies:**

   ```bash
   npm install
   ```

2. **Configure environment variables:**
   Copy the `.env` file and update it with your environment-specific values:

   ```bash
   cp .env.example .env
   # Edit .env with your cluster and gateway details
   ```

3. **Set up port-forwarding:**
   The tests require port-forwarding to access the Kubernetes services locally.

   **Option A - Using the helper script:**

   ```bash
   ./scripts/start-port-forward.sh
   ```

   **Option B - Manual setup:**

   ```bash
   kubectl port-forward svc/ingress-nginx-controller 8080:80 -n default
   ```

## Running Tests

### Automatic Precondition Checks

The test suite includes automatic precondition checks that verify:

- ✅ `kubectl` is available and working
- ✅ Port-forwarding is active and accessible
- ✅ Gateway connectivity through the port-forward
- ✅ Environment variables are properly configured

These checks run automatically before each test execution and will provide clear error messages if any prerequisites are missing.

### Test Execution

```bash
# Run all tests
npm test

# Run tests with debugging
DEBUG=pw:api npm test

# Run specific test file
npx playwright test tests/identity-management.spec.ts

# Run tests in headed mode (with browser UI)
npx playwright test --headed

# Run tests with trace for debugging
npx playwright test --trace on
```

### Test Reports

After running tests, reports are generated in:

- **HTML Report:** `playwright-report/index.html`
- **JUnit XML:** `reporter/junit.xml`
- **JSON Results:** `reporter/results.json`

View the HTML report:

```bash
npx playwright show-report
```

## Test Structure

### Test Categories

1. **Group Search Tests**

   - Search groups by name patterns
   - Empty search results handling
   - Search without parameters

2. **User Search Tests**

   - Search users by name patterns
   - Empty search results handling
   - Search without parameters

3. **Advanced Search Tests**
   - Search with role parameters
   - Search with group parameters

### Architecture

- **`tests/`** - Test specifications
- **`services/`** - API service abstractions
- **`fixtures/`** - Test fixtures and context setup
- **`setup/`** - Global setup and teardown scripts
- **`scripts/`** - Helper scripts for development

## Troubleshooting

### Port-forwarding Issues

If tests fail with connection errors:

1. **Check if port-forwarding is running:**

   ```bash
   ps aux | grep "kubectl port-forward"
   ```

2. **Verify the service exists:**

   ```bash
   kubectl get svc ingress-nginx-controller -n default
   ```

3. **Test connectivity manually:**

   ```bash
   curl -H "Host: gateway-pr-123-rabbit-n-d.activiti-hackathon.envalfresco.com" http://localhost:8080
   ```

4. **Restart port-forwarding:**
   ```bash
   pkill -f "kubectl port-forward.*8080:80"
   ./scripts/start-port-forward.sh
   ```

### Authentication Issues

If tests fail with 401/403 errors:

1. **Check Keycloak configuration:**

   - Verify the `activiti-keycloak` client exists in the `alfresco` realm
   - Ensure the client secret is correct
   - Confirm the client supports service accounts

2. **Verify environment variables:**
   ```bash
   echo $SSO_HOST
   echo $GATEWAY_HOST
   ```

### CI/CD Environment

For CI/CD environments, set the `CI=true` environment variable to skip port-forwarding precondition checks:

```bash
CI=true npm test
```

## Development

### Adding New Tests

1. Create test files in the `tests/` directory
2. Use the `activiti` test fixture for consistent setup
3. Leverage existing services in `services/` for API calls
4. Follow the existing naming conventions and structure

### Environment Configuration

The `.env` file supports environment-specific configuration:

- `LOCAL_PORT` - Local port for port-forwarding (default: 8080)
- `GATEWAY_HOST` - Gateway service hostname
- `SSO_HOST` - Keycloak authentication endpoint
- `CI` - Set to 'true' to skip local development checks

### Debugging

Enable debug logging:

```bash
DEBUG=pw:api npm test
```

For more detailed debugging, use trace recording:

```bash
npx playwright test --trace on
npx playwright show-trace test-results/trace.zip
```

## Migration Notes

This test suite replaces the original Java/Serenity-based acceptance tests with a modern TypeScript/Playwright implementation, providing:

- ✅ Faster execution
- ✅ Better debugging capabilities
- ✅ Modern TypeScript development experience
- ✅ Comprehensive precondition validation
- ✅ CI/CD friendly configuration
