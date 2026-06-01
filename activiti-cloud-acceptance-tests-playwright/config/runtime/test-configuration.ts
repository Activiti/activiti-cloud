/**
 * Test configuration helper for Playwright tests.
 * URLs come from env-hosts (GATEWAY_URL / IDENTITY_HOST) — call bootstrapAcceptanceEnv() first.
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
    const localPort = process.env.LOCAL_PORT || '8080';
    const protocol = process.env.GATEWAY_PROTOCOL || (isCI ? 'https' : 'http');

    const baseURL =
        process.env.GATEWAY_URL?.trim() ??
        (process.env.GATEWAY_HOST?.trim() ? `${protocol}://${process.env.GATEWAY_HOST.trim()}` : '');

    if (!baseURL || baseURL === `${protocol}://`) {
        throw new Error('GATEWAY_URL or GATEWAY_HOST must be set — call bootstrapAcceptanceEnv() first');
    }

    const identityHost = process.env.IDENTITY_HOST?.trim();
    const identityURL = identityHost
        ? identityHost.startsWith('http')
            ? identityHost
            : `${protocol}://${identityHost}`
        : baseURL.replace('://gateway-', '://identity-');

    return {
        baseURL,
        identityURL,
        isCI,
        usePortForwarding: !isCI,
        localPort: isCI ? undefined : localPort,
    };
}
