/*
 * Copyright 2017-2026 Alfresco Software, Ltd.
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

import { bootstrapAcceptanceEnv } from '../config/bootstrap';
import { withAuthenticatedContext } from '../fixtures/auth-context';
import { ProcessDefinitionRegistry } from '../models/process-definition-registry';
import { RuntimeBundleService } from '../services/runtime-bundle.service';

/** BPMN keys required for process-instance-actions (derived from ProcessDefinitionRegistry). */
export const RUNTIME_PROCESS_INSTANCE_ACTIONS_REQUIRED_KEYS =
    ProcessDefinitionRegistry.definitionKeysForProcessNames(
        ProcessDefinitionRegistry.processInstanceActionsProcessNames
    );

/** BPMN keys for task-actions wave 1 (derived from ProcessDefinitionRegistry). */
export const RUNTIME_TASK_ACTIONS_WAVE1_REQUIRED_KEYS = ProcessDefinitionRegistry.definitionKeysForProcessNames(
    ProcessDefinitionRegistry.taskActionsWave1ProcessNames
);

/** BPMN keys for task-actions wave 2 (derived from ProcessDefinitionRegistry). */
export const RUNTIME_TASK_ACTIONS_WAVE2_REQUIRED_KEYS = ProcessDefinitionRegistry.definitionKeysForProcessNames(
    ProcessDefinitionRegistry.taskActionsWave2ProcessNames
);

/** BPMN keys for service-tasks story (derived from ProcessDefinitionRegistry). */
export const RUNTIME_SERVICE_TASK_ACTIONS_REQUIRED_KEYS =
    ProcessDefinitionRegistry.definitionKeysForProcessNames(
        ProcessDefinitionRegistry.serviceTaskActionsProcessNames
    );

/** Keys required by Playwright specs on this PR (wave 1 + process-instance; wave 2 from task-extended). */
export const RUNTIME_ACCEPTANCE_REQUIRED_PROCESS_KEYS = [
    ...new Set([
        ...RUNTIME_PROCESS_INSTANCE_ACTIONS_REQUIRED_KEYS,
        ...RUNTIME_TASK_ACTIONS_WAVE1_REQUIRED_KEYS,
        ...RUNTIME_TASK_ACTIONS_WAVE2_REQUIRED_KEYS,
    ]),
] as const;

/** Service-task BPMN keys — used when service-tasks.spec.ts lands (separate PR). */
export const RUNTIME_SERVICE_TASK_OPTIONAL_PROCESS_KEYS = RUNTIME_SERVICE_TASK_ACTIONS_REQUIRED_KEYS;

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

async function isProcessDefinitionKeyDeployed(
    runtimeBundleService: RuntimeBundleService,
    processDefinitionKey: string
): Promise<boolean> {
    try {
        const definition = await runtimeBundleService.getProcessDefinitionByKey(processDefinitionKey);
        return definition.key === processDefinitionKey;
    } catch {
        return false;
    }
}

export async function getMissingRequiredProcessDefinitionKeys(
    runtimeBundleService: RuntimeBundleService,
    requiredKeys: readonly string[] = RUNTIME_ACCEPTANCE_REQUIRED_PROCESS_KEYS
): Promise<string[]> {
    const missing: string[] = [];
    for (const key of requiredKeys) {
        if (!(await isProcessDefinitionKeyDeployed(runtimeBundleService, key))) {
            missing.push(key);
        }
    }
    return missing;
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
        message.includes('Cannot list process definitions (HTTP 403)') ||
        message.includes('Cannot list process definitions (HTTP 500)') ||
        message.includes('openid-connect/certs') ||
        message.includes('decode the Jwt') ||
        message.includes('Invalid accessToken object instance')
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
    bootstrapAcceptanceEnv();

    await withAuthenticatedContext('testUser', async (context) => {
        await waitForRequiredProcessDefinitions(new RuntimeBundleService(context));
    });
}
