import type { PlayswagConfiguration } from '@michalfidor/playswag';
import { resolveGatewayConnection } from './connection/gateway-url';
import { getCachedOpenApiSpecPaths } from './lifecycle/setup/openapi-spec-cache';
import { paths } from './paths';

const isCi = process.env.CI === 'true' || process.env.GITHUB_ACTIONS === 'true';

function gatewayBaseUrl(): string {
    return resolveGatewayConnection().baseURL.replace(/\/$/, '');
}

function allowedSpecHosts(): string[] {
    try {
        const { baseURL } = resolveGatewayConnection();
        const host = new URL(baseURL).hostname;
        return host ? [host] : [];
    } catch {
        return [];
    }
}

function buildPlayswagSpecs(): string[] {
    return getCachedOpenApiSpecPaths();
}

export function buildPlayswagProjectUse() {
    return {
        playswagEnabled: process.env.PLAYSWAG_ENABLED !== 'false',
        playswagBaseURL: gatewayBaseUrl(),
        playswagSpecs: buildPlayswagSpecs(),
        playswagAcknowledgedServices: [
            { pattern: '**/identity-adapter-service/**', label: 'identity-adapter' },
            { pattern: '**/auth/**', label: 'keycloak' },
            { pattern: '**/realms/**', label: 'keycloak' },
        ],
    };
}

export function buildPlayswagReporterConfig(): PlayswagConfiguration {
    return {
        specs: buildPlayswagSpecs(),
        baseURL: gatewayBaseUrl(),
        outputDir: paths.playswagCoverage,
        outputFormats: isCi ? ['console', 'json', 'html', 'markdown'] : ['console', 'json', 'html'],
        allowedSpecHosts: allowedSpecHosts(),
        allowPrivateHosts: !isCi,
        acknowledgedServices: [
            { pattern: '**/identity-adapter-service/**', label: 'identity-adapter' },
            { pattern: '**/auth/**', label: 'keycloak' },
            { pattern: '**/realms/**', label: 'keycloak' },
        ],
        failOnThreshold: false,
        failOnSpecError: isCi,
        consoleOutput: {
            showUncoveredOnly: false,
            showOperations: true,
            showTags: true,
        },
        githubActionsOutput: {
            showUncoveredOperations: isCi,
            showUnmatchedHits: isCi,
        },
    };
}
