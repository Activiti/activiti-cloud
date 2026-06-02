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

export function getTestConfiguration(): TestConfiguration {
    const isCI = process.env.CI === 'true' || process.env.GITHUB_ACTIONS === 'true';
    const previewName = process.env.PREVIEW_NAME || 'pr-123-rabbit-n-d';

    if (isCI) {
        const clusterName = process.env.CLUSTER_NAME || 'activiti';
        const clusterDomain = process.env.CLUSTER_DOMAIN || 'envalfresco.com';
        const globalGatewayDomain = `${clusterName}.${clusterDomain}`;

        return {
            baseURL: `https://gateway-${previewName}.${globalGatewayDomain}`,
            identityURL: `https://identity-${previewName}.${globalGatewayDomain}`,
            isCI: true,
            usePortForwarding: false,
        };
    }

    const localPort = process.env.LOCAL_PORT || '8080';
    const clusterName = process.env.CLUSTER_NAME || 'activiti-hackathon';
    const clusterDomain = process.env.CLUSTER_DOMAIN || 'envalfresco.com';
    const globalGatewayDomain = `${clusterName}.${clusterDomain}`;

    return {
        baseURL: `http://gateway-${previewName}.${globalGatewayDomain}:${localPort}`,
        identityURL: `http://identity-${previewName}.${globalGatewayDomain}:${localPort}`,
        isCI: false,
        usePortForwarding: true,
        localPort,
    };
}

export function getRequiredHosts(): string[] {
    const previewName = process.env.PREVIEW_NAME || 'pr-123-rabbit-n-d';
    const clusterName = process.env.CLUSTER_NAME || 'activiti-hackathon';
    const clusterDomain = process.env.CLUSTER_DOMAIN || 'envalfresco.com';

    return [
        `gateway-${previewName}.${clusterName}.${clusterDomain}`,
        `identity-${previewName}.${clusterName}.${clusterDomain}`,
    ];
}
