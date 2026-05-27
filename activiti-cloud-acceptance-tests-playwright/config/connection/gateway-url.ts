/**
 * Resolves how API clients reach the gateway in local vs CI environments.
 */

export interface GatewayConnection {
    baseURL: string;
    hostHeader?: string;
}

function isCI(): boolean {
    return process.env.CI === 'true' || process.env.GITHUB_ACTIONS === 'true';
}

function stripPort(host: string): string {
    return host.replace(/:\d+$/, '');
}

export function resolveGatewayConnection(): GatewayConnection {
    const protocol = process.env.GATEWAY_PROTOCOL || 'https';
    const gatewayHost = process.env.GATEWAY_HOST;
    const gatewayUrl = process.env.GATEWAY_URL;

    if (!gatewayHost && !gatewayUrl) {
        throw new Error('Set GATEWAY_HOST or GATEWAY_URL in activiti-cloud-acceptance-tests-playwright/.env');
    }

    const useLocalPortForward =
        !isCI() && process.env.LOCAL_USE_PORT_FORWARD !== 'false' && Boolean(process.env.LOCAL_PORT);

    if (useLocalPortForward) {
        const localPort = process.env.LOCAL_PORT || '8080';
        const hostHeader = stripPort(gatewayHost || new URL(gatewayUrl!).host);

        return {
            baseURL: `http://localhost:${localPort}`,
            hostHeader,
        };
    }

    if (gatewayUrl?.startsWith('http')) {
        return { baseURL: gatewayUrl.replace(/\/$/, '') };
    }

    if (gatewayHost?.includes('localhost')) {
        return { baseURL: `${protocol}://${gatewayHost}` };
    }

    return { baseURL: `${protocol}://${gatewayHost}` };
}
