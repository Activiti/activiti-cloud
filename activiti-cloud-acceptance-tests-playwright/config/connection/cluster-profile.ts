/**
 * Cluster deployment profiles for local/CI acceptance tests.
 *
 * - preview: Activiti Cloud full chart (gateway-{preview}.{cluster}.domain)
 * - develop: Shared develop environment (single host develop.envalfresco.com)
 */

export type ClusterProfile = 'preview' | 'develop';

export function getClusterProfile(): ClusterProfile {
    const explicit = process.env.CLUSTER_PROFILE?.trim().toLowerCase();
    if (explicit === 'develop' || explicit === 'preview') {
        return explicit;
    }

    if (process.env.PREVIEW_NAME?.trim()) {
        return 'preview';
    }

    if (process.env.CLUSTER_NAME?.trim() === 'develop') {
        return 'develop';
    }

    return 'preview';
}

export function isDevelopProfile(): boolean {
    return getClusterProfile() === 'develop';
}

export function getDevelopGatewayHost(): string {
    const port = process.env.LOCAL_PORT || '8080';
    const domain = process.env.CLUSTER_DOMAIN || 'envalfresco.com';
    const cluster = process.env.CLUSTER_NAME || 'develop';
    return `${cluster}.${domain}:${port}`;
}
