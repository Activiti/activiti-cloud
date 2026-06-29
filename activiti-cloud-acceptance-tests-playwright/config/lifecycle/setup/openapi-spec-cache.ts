import { mkdir, writeFile } from 'fs/promises';
import { request } from '@playwright/test';
import { resolveGatewayConnection } from '../../connection/gateway-url';
import { acceptanceLog, acceptanceStep } from '../../../helpers/acceptance-progress';
import { paths } from '../../paths';

const OPENAPI_SPECS = [
    { file: 'runtime-bundle.json', path: '/rb/v3/api-docs/Runtime%20Bundle', label: 'Runtime Bundle' },
    { file: 'query.json', path: '/query/v3/api-docs/Query', label: 'Query' },
    { file: 'audit.json', path: '/audit/v3/api-docs/Audit', label: 'Audit' },
] as const;

export function getCachedOpenApiSpecPaths(): string[] {
    return OPENAPI_SPECS.map(({ file }) => `${paths.openapiSpecs}/${file}`);
}

export async function cacheOpenApiSpecs(): Promise<void> {
    const { baseURL, hostHeader } = resolveGatewayConnection();
    const headers: Record<string, string> = { accept: 'application/json' };
    if (hostHeader) {
        headers.Host = hostHeader;
    }

    await mkdir(paths.openapiSpecs, { recursive: true });

    const api = await request.newContext({ baseURL, extraHTTPHeaders: headers });
    try {
        for (const { file, path: specPath, label } of OPENAPI_SPECS) {
            acceptanceStep('discovery', `Caching OpenAPI spec: ${label}`);
            const response = await api.get(specPath);
            if (!response.ok()) {
                throw new Error(
                    `Failed to fetch OpenAPI spec "${label}" from ${baseURL}${specPath}: HTTP ${response.status()}`
                );
            }
            await writeFile(`${paths.openapiSpecs}/${file}`, await response.text(), 'utf8');
            acceptanceLog('discovery', `✓ Cached ${file}`);
        }
    } finally {
        await api.dispose();
    }
}
