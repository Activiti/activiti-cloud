/**
 * Test configuration helper for Playwright tests
 * Handles environment detection and URL configuration for both CI and local development
 */

export interface TestConfiguration {
  baseURL: string;
  identityURL: string;
  isCI: boolean;
  usePortForwarding: boolean;
  localPort?: string;
}

/**
 * Get test configuration based on environment
 * Detects CI vs local development and returns appropriate URLs and settings
 */
export function getTestConfiguration(): TestConfiguration {
  const isCI = process.env.CI === 'true' || process.env.GITHUB_ACTIONS === 'true';
  const previewName = process.env.PREVIEW_NAME || 'pr-123-rabbit-n-d';

  if (isCI) {
    // CI/CD Environment (GitHub Actions)
    const clusterName = process.env.CLUSTER_NAME || 'activiti';
    const clusterDomain = process.env.CLUSTER_DOMAIN || 'envalfresco.com';
    const globalGatewayDomain = `${clusterName}.${clusterDomain}`;

    return {
      baseURL: `https://gateway-${previewName}.${globalGatewayDomain}`,
      identityURL: `https://identity-${previewName}.${globalGatewayDomain}`,
      isCI: true,
      usePortForwarding: false
    };
  } else {
    // Local Development Environment
    const localPort = process.env.LOCAL_PORT || '8080';
    const clusterName = process.env.CLUSTER_NAME || 'activiti-hackathon';
    const clusterDomain = process.env.CLUSTER_DOMAIN || 'envalfresco.com';
    const globalGatewayDomain = `${clusterName}.${clusterDomain}`;

    return {
      baseURL: `http://gateway-${previewName}.${globalGatewayDomain}:${localPort}`,
      identityURL: `http://identity-${previewName}.${globalGatewayDomain}:${localPort}`,
      isCI: false,
      usePortForwarding: true,
      localPort: localPort
    };
  }
}

/**
 * Get required hosts for /etc/hosts file in local development
 */
export function getRequiredHosts(): string[] {
  const previewName = process.env.PREVIEW_NAME || 'pr-123-rabbit-n-d';
  const clusterName = process.env.CLUSTER_NAME || 'activiti-hackathon';
  const clusterDomain = process.env.CLUSTER_DOMAIN || 'envalfresco.com';

  return [
    `gateway-${previewName}.${clusterName}.${clusterDomain}`,
    `identity-${previewName}.${clusterName}.${clusterDomain}`
  ];
}

// Export the configuration instance for direct use
export const testConfig = getTestConfiguration();
