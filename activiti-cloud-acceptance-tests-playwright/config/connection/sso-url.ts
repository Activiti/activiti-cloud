import { buildIdentityHost } from './env-hosts';
import { isDevelopProfile } from './cluster-profile';

export interface SsoConnection {
    tokenUrl: string;
    hostHeader?: string;
}

function isCI(): boolean {
    return process.env.CI === 'true' || process.env.GITHUB_ACTIONS === 'true';
}

function stripPort(host: string): string {
    return host.replace(/:\d+$/, '');
}

export function buildPreviewTokenUrl(realm: string): string {
    const preview = process.env.PREVIEW_NAME!;
    const cluster = process.env.CLUSTER_NAME || 'develop';
    const domain = process.env.CLUSTER_DOMAIN || 'envalfresco.com';
    const protocol = process.env.SSO_PROTOCOL || process.env.GATEWAY_PROTOCOL || 'https';
    const identityHost = buildIdentityHost(preview, cluster, domain);
    return `${protocol}://${identityHost}/auth/realms/${realm}/protocol/openid-connect/token`;
}

export function resolveSsoConnection(): SsoConnection {
    const explicit = process.env.SSO_HOST?.trim();
    const realm = process.env.KEYCLOAK_REALM || process.env.REALM || 'activiti';

    const useLocalPortForward =
        !isCI() && process.env.LOCAL_USE_PORT_FORWARD !== 'false' && Boolean(process.env.LOCAL_PORT);

    if (useLocalPortForward && !isDevelopProfile()) {
        const preview = process.env.PREVIEW_NAME;
        const cluster = process.env.CLUSTER_NAME;
        const domain = process.env.CLUSTER_DOMAIN || 'envalfresco.com';
        const localPort = process.env.LOCAL_PORT || '8080';

        if (preview && cluster) {
            const hostHeader = stripPort(buildIdentityHost(preview, cluster, domain));
            return {
                tokenUrl: `http://127.0.0.1:${localPort}/auth/realms/${realm}/protocol/openid-connect/token`,
                hostHeader,
            };
        }
    }

    if (explicit) {
        return { tokenUrl: explicit };
    }

    if (process.env.PREVIEW_NAME) {
        return { tokenUrl: buildPreviewTokenUrl(realm) };
    }

    throw new Error('Set SSO_HOST or PREVIEW_NAME + CLUSTER_NAME for Keycloak token URL');
}
