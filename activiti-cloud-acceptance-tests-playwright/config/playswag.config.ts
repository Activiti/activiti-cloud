import type { PlayswagConfiguration } from '@michalfidor/playswag';
import { resolveGatewayConnection } from './connection/gateway-url';
import { getCachedOpenApiSpecPaths } from './lifecycle/setup/openapi-spec-cache';
import { paths } from './paths';

const isCi = process.env.CI === 'true' || process.env.GITHUB_ACTIONS === 'true';

const PLAYSWAG_ACKNOWLEDGED_SERVICES = [
    { pattern: '**/identity-adapter-service/**', label: 'identity-adapter' },
    { pattern: '**/auth/**', label: 'keycloak' },
    { pattern: '**/realms/**', label: 'keycloak' },
] as const;

const PLAYSWAG_EXCLUDE_PATTERNS = ['**/v3/api-docs/**', '**/actuator/**'];

const PLAYSWAG_CONSOLE_OUTPUT = {
    showOperations: false,
    showUncoveredOnly: false,
    showParams: false,
    showBodyProperties: false,
    showStatusCodeBreakdown: true,
} as const;

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

function sharedPlayswagOptions() {
    return {
        playswagEnabled: process.env.PLAYSWAG_ENABLED !== 'false',
        playswagBaseURL: gatewayBaseUrl(),
        playswagAcknowledgedServices: [...PLAYSWAG_ACKNOWLEDGED_SERVICES],
    };
}

export function buildPlayswagProjectUse() {
    return sharedPlayswagOptions();
}

export function buildPlayswagReporterConfig(): PlayswagConfiguration {
    return {
        specs: buildPlayswagSpecs(),
        baseURL: gatewayBaseUrl(),
        outputDir: paths.playswagCoverage,
        outputFormats: isCi ? ['console', 'json', 'html', 'markdown'] : ['console', 'json', 'html'],
        allowedSpecHosts: allowedSpecHosts(),
        allowPrivateHosts: !isCi,
        excludePatterns: PLAYSWAG_EXCLUDE_PATTERNS,
        acknowledgedServices: [...PLAYSWAG_ACKNOWLEDGED_SERVICES],
        failOnThreshold: false,
        failOnSpecError: isCi,
        consoleOutput: PLAYSWAG_CONSOLE_OUTPUT,
        githubActionsOutput: {
            showUncoveredOperations: false,
            showUnmatchedHits: false,
        },
    };
}
