import { getDevelopGatewayHost, isDevelopProfile } from './cluster-profile';

function isCiEnv(): boolean {
    return process.env.CI === 'true' || process.env.GITHUB_ACTIONS === 'true';
}

function localPortSuffix(): string | undefined {
    if (isCiEnv()) {
        return undefined;
    }
    return process.env.LOCAL_PORT || '8080';
}

export function buildGatewayHost(previewName: string, clusterName: string, clusterDomain: string, port?: string): string {
    const host = `gateway-${previewName}.${clusterName}.${clusterDomain}`;
    return port ? `${host}:${port}` : host;
}

export function buildIdentityHost(previewName: string, clusterName: string, clusterDomain: string, port?: string): string {
    const host = `identity-${previewName}.${clusterName}.${clusterDomain}`;
    return port ? `${host}:${port}` : host;
}

export function resolveGatewayHostEnv(): string {
    if (isDevelopProfile()) {
        const explicit = process.env.GATEWAY_HOST?.trim();
        return explicit || getDevelopGatewayHost();
    }

    const preview = process.env.PREVIEW_NAME;
    const cluster = process.env.CLUSTER_NAME;
    const domain = process.env.CLUSTER_DOMAIN || 'envalfresco.com';
    const port = localPortSuffix();
    const explicit = process.env.GATEWAY_HOST?.trim();

    if (!preview || !cluster) {
        if (!explicit) {
            throw new Error('Set GATEWAY_HOST or both PREVIEW_NAME and CLUSTER_NAME');
        }
        return explicit;
    }

    const expectedCore = `gateway-${preview}.${cluster}.${domain}`;

    if (!explicit) {
        return buildGatewayHost(preview, cluster, domain, port);
    }

    const hostWithoutPort = explicit.replace(/:\d+$/, '');
    if (!hostWithoutPort.includes(`${preview}.${cluster}`)) {
        const resolved = buildGatewayHost(preview, cluster, domain, port);
        console.warn(
            `⚠️  GATEWAY_HOST "${explicit}" does not match PREVIEW_NAME/CLUSTER_NAME — using ${resolved}`
        );
        return resolved;
    }

    return explicit;
}

export function applyResolvedHostsToEnv(): void {
    process.env.GATEWAY_HOST = resolveGatewayHostEnv();

    if (!process.env.GATEWAY_URL?.trim()) {
        const protocol = process.env.GATEWAY_PROTOCOL || 'http';
        process.env.GATEWAY_URL = `${protocol}://${process.env.GATEWAY_HOST}`;
    }

    if (!process.env.IDENTITY_HOST?.trim() && process.env.PREVIEW_NAME && process.env.CLUSTER_NAME) {
        const port = localPortSuffix();
        process.env.IDENTITY_HOST = buildIdentityHost(
            process.env.PREVIEW_NAME,
            process.env.CLUSTER_NAME,
            process.env.CLUSTER_DOMAIN ?? 'envalfresco.com',
            port
        );
    }
}
