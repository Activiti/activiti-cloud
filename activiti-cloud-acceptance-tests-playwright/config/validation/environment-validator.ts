/**
 * Validates .env and connectivity before running acceptance tests locally or in CI.
 */

import { request } from '@playwright/test';
import { ContextFactory } from '../../context-factory';
import { resolveGatewayConnection } from '../connection/gateway-url';
import { isDevelopProfile } from '../connection/cluster-profile';
import { users } from '../../users';
import { timeouts } from '../runtime/timeouts';

export interface EnvCheckResult {
    ok: boolean;
    errors: string[];
    warnings: string[];
}

const REQUIRED_VARS_PREVIEW = ['PREVIEW_NAME', 'SSO_HOST', 'KEYCLOAK_CLIENT_ID'] as const;
const REQUIRED_VARS_DEVELOP = ['SSO_HOST', 'KEYCLOAK_CLIENT_ID'] as const;

export const USERS_BY_PROJECT: Record<string, (keyof typeof users)[]> = {
    identity: ['testUser'],
    security: ['hruser', 'hradmin', 'processadmin'],
    process: ['testUser'],
    runtime: ['testUser', 'hruser', 'testAdminUser', 'processadmin'],
    all: ['testUser', 'hruser', 'hradmin', 'testAdminUser', 'processadmin'],
};

function isPlaceholder(value: string | undefined): boolean {
    return !value || value.startsWith('no-') || value.includes('your-');
}

export function validateEnvironmentVariables(project: string = 'all'): EnvCheckResult {
    const errors: string[] = [];
    const warnings: string[] = [];

    const required = isDevelopProfile() ? REQUIRED_VARS_DEVELOP : REQUIRED_VARS_PREVIEW;
    for (const key of required) {
        if (!process.env[key]?.trim()) {
            errors.push(`Missing required variable: ${key}`);
        }
    }

    const userKeys = USERS_BY_PROJECT[project] ?? USERS_BY_PROJECT.all;
    for (const userKey of userKeys) {
        const creds = users[userKey];
        if (isPlaceholder(creds.username) || isPlaceholder(creds.password)) {
            const envPrefix = userKey === 'testUser' ? 'TESTUSER' : userKey.toUpperCase();
            errors.push(
                `Missing credentials for "${userKey}" — set ${envPrefix}_USERNAME and ${envPrefix}_PASSWORD in .env`
            );
        }
    }

    if (userKeys.includes('hradmin') && !process.env.HRADMIN_USERNAME) {
        errors.push('HRADMIN_USERNAME is required for security tests (often forgotten — copy from hradmin Keycloak user)');
    }

    const clientId = process.env.KEYCLOAK_CLIENT_ID || process.env.REALM;
    if (!clientId?.trim()) {
        errors.push('Set KEYCLOAK_CLIENT_ID (preview default: activiti)');
    }

    if (
        clientId === 'activiti' &&
        !process.env.KEYCLOAK_CLIENT_SECRET?.trim() &&
        process.env.CLUSTER_PROFILE !== 'develop' &&
        process.env.CLUSTER_NAME !== 'develop'
    ) {
        errors.push(
            'KEYCLOAK_CLIENT_SECRET is required for client activiti — use npm run test:setup or read activiti-keycloak-client secret'
        );
    }

    return { ok: errors.length === 0, errors, warnings };
}

export async function checkGatewayReachable(): Promise<{ warnings: string[] }> {
    const warnings: string[] = [];
    const { baseURL, hostHeader } = resolveGatewayConnection();
    const context = await request.newContext();
    const headers: Record<string, string> = {};
    if (hostHeader) {
        headers.Host = hostHeader;
    }

    const ingressProbe = isDevelopProfile() ? '/auth/realms/alfresco' : '/rb/actuator/health';
    const response = await context.get(`${baseURL}${ingressProbe}`, {
        headers,
        timeout: timeouts.http.default,
        ignoreHTTPSErrors: true,
    });

    if (response.status() >= 500) {
        await context.dispose();
        throw new Error(`Ingress probe failed: ${baseURL}${ingressProbe} → HTTP ${response.status()}`);
    }

    if (isDevelopProfile()) {
        const rb = await context.get(`${baseURL}/rb/actuator/health`, {
            headers,
            timeout: timeouts.http.healthCheck,
            ignoreHTTPSErrors: true,
        });
        const identity = await context.get(`${baseURL}/identity-adapter-service/actuator/health`, {
            headers,
            timeout: timeouts.http.healthCheck,
            ignoreHTTPSErrors: true,
        });
        if (rb.status() === 404 || identity.status() === 404) {
            warnings.push(
                'Activiti Cloud API (/rb, /identity-adapter-service) is not deployed on develop — install with make install + GLOBAL_GATEWAY_DOMAIN=develop.envalfresco.com before running Playwright tests'
            );
        }
    }

    await context.dispose();
    return { warnings };
}

export async function checkAuthentication(userKey: keyof typeof users): Promise<void> {
    await ContextFactory.getContextByUserName(userKey);
}

export async function runPreflightChecks(project: string = 'all'): Promise<EnvCheckResult> {
    const result = validateEnvironmentVariables(project);

    if (!result.ok) {
        return result;
    }

    try {
        const gateway = await checkGatewayReachable();
        result.warnings.push(...gateway.warnings);
        console.log('✅ Ingress / port-forward reachable');
    } catch (e) {
        result.errors.push(e instanceof Error ? e.message : String(e));
        result.ok = false;
        return result;
    }

    const userKeys = USERS_BY_PROJECT[project] ?? USERS_BY_PROJECT.all;
    for (const userKey of userKeys) {
        try {
            await checkAuthentication(userKey);
            console.log(`✅ Authentication OK: ${userKey}`);
        } catch (e) {
            result.errors.push(
                `Auth failed for ${userKey}: ${e instanceof Error ? e.message.split('\n')[0] : String(e)}`
            );
            result.ok = false;
        }
    }

    return result;
}
