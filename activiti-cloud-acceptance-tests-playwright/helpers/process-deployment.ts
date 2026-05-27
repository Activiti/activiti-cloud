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

export async function assertRequiredProcessDefinitionsDeployed(
    runtimeBundleService: RuntimeBundleService,
    requiredKeys: readonly string[] = RUNTIME_ACCEPTANCE_REQUIRED_PROCESS_KEYS
): Promise<void> {
    const missing = await getMissingRequiredProcessDefinitionKeys(runtimeBundleService, requiredKeys);
    if (missing.length > 0) {
        throw new Error(formatMissingProcessCatalogMessage(missing));
    }
}
