import { mkdir, writeFile } from 'fs/promises';
import { request } from '@playwright/test';
import { resolveGatewayConnection } from '../../connection/gateway-url';
import { acceptanceLog, acceptanceStep } from '../../../helpers/acceptance-progress';
import { paths } from '../../paths';

interface OpenApiDocument {
    paths?: Record<string, unknown>;
    servers?: Array<{ url?: string }>;
}

const OPENAPI_SPECS = [
    {
        file: 'runtime-bundle.json',
        path: '/rb/v3/api-docs/Runtime%20Bundle',
        label: 'Runtime Bundle',
        gatewayPrefix: '/rb',
    },
    {
        file: 'query.json',
        path: '/query/v3/api-docs/Query',
        label: 'Query',
        gatewayPrefix: '/query',
    },
    {
        file: 'audit.json',
        path: '/audit/v3/api-docs/Audit',
        label: 'Audit',
        gatewayPrefix: '/audit',
    },
] as const;

export function getCachedOpenApiSpecPaths(): string[] {
    return OPENAPI_SPECS.map(({ file }) => `${paths.openapiSpecs}/${file}`);
}

export function normalizeOpenApiSpecForGateway(
    spec: OpenApiDocument,
    gatewayPrefix: string,
    gatewayBaseUrl: string
): OpenApiDocument {
    const normalized: OpenApiDocument = { ...spec, paths: {} };
    const prefix = gatewayPrefix.replace(/\/$/, '');

    for (const [pathTemplate, operation] of Object.entries(spec.paths ?? {})) {
        normalized.paths![`${prefix}${pathTemplate}`] = operation;
    }

    normalized.servers = [{ url: gatewayBaseUrl.replace(/\/$/, '') }];
    return normalized;
}

export async function cacheOpenApiSpecs(): Promise<void> {
    const { baseURL, hostHeader } = resolveGatewayConnection();
    const gatewayBaseUrl = baseURL.replace(/\/$/, '');
    const headers: Record<string, string> = { accept: 'application/json' };
    if (hostHeader) {
        headers.Host = hostHeader;
    }

    await mkdir(paths.openapiSpecs, { recursive: true });

    const api = await request.newContext({ baseURL: gatewayBaseUrl, extraHTTPHeaders: headers });
    try {
        for (const { file, path: specPath, label, gatewayPrefix } of OPENAPI_SPECS) {
            acceptanceStep('discovery', `Caching OpenAPI spec: ${label}`);
            const response = await api.get(specPath);
            if (!response.ok()) {
                throw new Error(
                    `Failed to fetch OpenAPI spec "${label}" from ${gatewayBaseUrl}${specPath}: HTTP ${response.status()}`
                );
            }

            const rawSpec = (await response.json()) as OpenApiDocument;
            const normalizedSpec = normalizeOpenApiSpecForGateway(rawSpec, gatewayPrefix, gatewayBaseUrl);
            await writeFile(`${paths.openapiSpecs}/${file}`, `${JSON.stringify(normalizedSpec)}\n`, 'utf8');
            acceptanceLog('discovery', `✓ Cached ${file}`);
        }
    } finally {
        await api.dispose();
    }
}
