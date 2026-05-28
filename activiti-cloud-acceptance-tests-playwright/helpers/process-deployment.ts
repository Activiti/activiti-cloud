/*
 * Copyright 2017-2020 Alfresco Software, Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { RuntimeBundleService } from '../services/runtime-bundle.service';

/**
 * BPMN keys required for process-instance-actions.story parity (Serenity runtime-acceptance-tests).
 * Deployed from activiti/example-runtime-bundle on chart install.
 */
export const RUNTIME_PROCESS_INSTANCE_ACTIONS_REQUIRED_KEYS = [
    'SimpleProcess',
    'ProcessWithVariables',
    'ConnectorProcess',
    'fixSystemFailure',
    'SingleTaskProcess',
    'HeadersConnectorProcess',
] as const;

/** BPMN keys for task-actions.story wave 1 (Serenity runtime-acceptance-tests). */
export const RUNTIME_TASK_ACTIONS_WAVE1_REQUIRED_KEYS = [
    'ProcessWithVariables',
    'SingleTaskProcess',
    'SingleTaskProcessUserCandidates',
    'SingleTaskProcessGroupCandidates',
    'singletask-b6095889-6177-4b73-b3d9-316e47749a36',
] as const;

export const RUNTIME_ACCEPTANCE_REQUIRED_PROCESS_KEYS = [
    ...new Set([
        ...RUNTIME_PROCESS_INSTANCE_ACTIONS_REQUIRED_KEYS,
        ...RUNTIME_TASK_ACTIONS_WAVE1_REQUIRED_KEYS,
    ]),
] as const;

/** @deprecated Use RUNTIME_PROCESS_INSTANCE_ACTIONS_REQUIRED_KEYS */
export const PROCESS_INSTANCE_ACTIONS_CORE_KEYS = RUNTIME_PROCESS_INSTANCE_ACTIONS_REQUIRED_KEYS;

export function formatMissingProcessCatalogMessage(missingKeys: string[]): string {
    return (
        `Missing process definitions on runtime-bundle: ${missingKeys.join(', ')}.\n` +
        'Fix: npm run cluster:prereqs  (ensures activiti/example-runtime-bundle image + acceptance policies)\n' +
        'Or set ACCEPTANCE_RUNTIME_BUNDLE_IMAGE in .env and re-run cluster:prereqs.'
    );
}

export async function getDeployedProcessDefinitionKeys(
    runtimeBundleService: RuntimeBundleService
): Promise<Set<string>> {
    const definitions = await runtimeBundleService.getProcessDefinitions();
    return new Set(definitions.map((definition) => definition.key));
}

export async function getMissingRequiredProcessDefinitionKeys(
    runtimeBundleService: RuntimeBundleService,
    requiredKeys: readonly string[] = RUNTIME_ACCEPTANCE_REQUIRED_PROCESS_KEYS
): Promise<string[]> {
    const deployedKeys = await getDeployedProcessDefinitionKeys(runtimeBundleService);
    return requiredKeys.filter((key) => !deployedKeys.has(key));
}

function sleep(ms: number): Promise<void> {
    return new Promise((resolve) => setTimeout(resolve, ms));
}

function isTransientProcessCatalogError(error: unknown): boolean {
    if (!(error instanceof Error)) {
        return false;
    }
    const message = error.message;
    return (
        message.includes('Cannot list process definitions (HTTP 401)') ||
        message.includes('Cannot list process definitions (HTTP 403)')
    );
}

function processCatalogPollTimeoutMs(): number {
    const configured = Number(process.env.ACCEPTANCE_PROCESS_CATALOG_TIMEOUT_MS);
    if (Number.isFinite(configured) && configured > 0) {
        return configured;
    }
    return process.env.CI === 'true' || process.env.GITHUB_ACTIONS === 'true' ? 300_000 : 120_000;
}

/**
 * After runtime-bundle rollout, BPMN auto-deployment can lag behind pod Ready.
 * Poll until required keys are visible on /rb/v1/process-definitions (or timeout).
 */
export async function waitForRequiredProcessDefinitions(
    runtimeBundleService: RuntimeBundleService,
    requiredKeys: readonly string[] = RUNTIME_ACCEPTANCE_REQUIRED_PROCESS_KEYS,
    options?: { timeoutMs?: number; intervalMs?: number }
): Promise<void> {
    const timeoutMs = options?.timeoutMs ?? processCatalogPollTimeoutMs();
    const intervalMs = options?.intervalMs ?? 10_000;
    const deadline = Date.now() + timeoutMs;
    let attempt = 0;

    while (Date.now() < deadline) {
        attempt += 1;
        try {
            const missing = await getMissingRequiredProcessDefinitionKeys(runtimeBundleService, requiredKeys);
            if (missing.length === 0) {
                if (attempt > 1) {
                    console.log(`✓ Process catalog ready after ${attempt} attempt(s)`);
                }
                return;
            }

            const remainingSec = Math.max(0, Math.ceil((deadline - Date.now()) / 1000));
            console.log(
                `Waiting for runtime-bundle BPMN catalog (${missing.length} missing, ~${remainingSec}s left): ${missing.join(', ')}`
            );
        } catch (error) {
            if (!isTransientProcessCatalogError(error)) {
                throw error;
            }
            const remainingSec = Math.max(0, Math.ceil((deadline - Date.now()) / 1000));
            console.log(
                `Waiting for runtime-bundle auth (~${remainingSec}s left): ${error instanceof Error ? error.message.split('\n')[0] : String(error)}`
            );
        }
        await sleep(intervalMs);
    }

    const missing = await getMissingRequiredProcessDefinitionKeys(runtimeBundleService, requiredKeys);
    if (missing.length > 0) {
        throw new Error(formatMissingProcessCatalogMessage(missing));
    }
}

export async function verifyAcceptanceProcessCatalog(): Promise<void> {
    const { applyResolvedHostsToEnv } = await import('../config/connection/env-hosts');
    const { ContextFactory } = await import('../fixtures/context-factory');

    applyResolvedHostsToEnv();

    const context = await ContextFactory.getContextByUserName('testUser');
    try {
        await waitForRequiredProcessDefinitions(new RuntimeBundleService(context));
    } finally {
        await context.dispose();
    }
}

export async function assertRequiredProcessDefinitionsDeployed(
    runtimeBundleService: RuntimeBundleService,
    requiredKeys: readonly string[] = RUNTIME_ACCEPTANCE_REQUIRED_PROCESS_KEYS
): Promise<void> {
    const missing = await getMissingRequiredProcessDefinitionKeys(runtimeBundleService, requiredKeys);
    if (missing.length > 0) {
        throw new Error(formatMissingProcessCatalogMessage(missing));
    }
}
