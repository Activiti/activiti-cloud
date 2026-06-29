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

import {
    CloudProcessInstance,
    ProcessInstanceStatus,
    StartProcessPayload,
    ProcessQueryParams,
    UpdateProcessPayload,
    CreateProcessInstancePayload,
    SignalPayload,
} from '../models/runtime-bundle.models';
import { CloudProcessDefinition, ConnectorDefinition, ProcessDefinitionMeta } from '../models/process-definition.models';
import { CloudVariableInstance } from '../models/process-variable.models';
import { BaseService, RequestResponse } from './base.service';
import { CustomAPIRequest } from '../fixtures/context.models';

export class RuntimeBundleService extends BaseService {
    private readonly basePath: string;

    constructor(context: CustomAPIRequest, runtimeBasePath: string = '/rb') {
        super(context);
        this.basePath = `${runtimeBasePath.replace(/\/$/, '')}/v1`;
    }

    async startProcess(payload: Omit<StartProcessPayload, 'payloadType'>): Promise<CloudProcessInstance> {
        const { name, businessKey, processDefinitionKey, ...rest } = payload;
        if (!processDefinitionKey && !rest.processDefinitionId) {
            throw new Error('startProcess requires processDefinitionKey or processDefinitionId');
        }
        const body = {
            payloadType: 'StartProcessPayload' as const,
            ...rest,
            ...(processDefinitionKey ? { processDefinitionKey } : {}),
            name: name ?? this.defaultProcessInstanceName(),
            businessKey: businessKey ?? this.defaultBusinessKey(),
        };

        const response = await this.post(`${this.basePath}/process-instances`, { data: body });

        const processInstance = this.unwrapEntity<CloudProcessInstance>(response);
        if (processInstance.id) {
            this.trackCreatedResource(`${this.basePath}/process-instances/${processInstance.id}`);
        }
        return processInstance;
    }

    async createProcess(payload: Omit<CreateProcessInstancePayload, 'payloadType'>): Promise<CloudProcessInstance> {
        const { name, businessKey, processDefinitionKey, ...rest } = payload;
        const body = {
            payloadType: 'CreateProcessInstancePayload' as const,
            ...rest,
            ...(processDefinitionKey ? { processDefinitionKey } : {}),
            name: name ?? this.defaultProcessInstanceName(),
            businessKey: businessKey ?? this.defaultBusinessKey(),
        };

        const response = await this.post(`${this.basePath}/process-instances/create`, { data: body });
        const processInstance = this.unwrapEntity<CloudProcessInstance>(response);
        if (processInstance.id) {
            this.trackCreatedResource(`${this.basePath}/process-instances/${processInstance.id}`);
        }
        return processInstance;
    }

    async startCreatedProcess(processInstanceId: string): Promise<CloudProcessInstance> {
        const response = await this.post(`${this.basePath}/process-instances/${processInstanceId}/start`, {
            data: {
                payloadType: 'StartProcessPayload',
            },
        });
        return this.unwrapEntity<CloudProcessInstance>(response);
    }

    async sendSignal(name: string, variables?: Record<string, unknown>): Promise<RequestResponse> {
        const body: Omit<SignalPayload, 'payloadType'> & { payloadType: 'SignalPayload' } = {
            payloadType: 'SignalPayload',
            name,
            ...(variables ? { variables } : {}),
        };
        return this.post(`${this.basePath}/process-instances/signal`, { data: body });
    }

    async getHomeInfo(): Promise<Record<string, unknown>> {
        const response = await this.get(this.basePath);
        return this.unwrapEntity<Record<string, unknown>>(response);
    }

    async getProcessDefinitionById(processDefinitionId: string): Promise<CloudProcessDefinition> {
        const response = await this.get(`${this.basePath}/process-definitions/${processDefinitionId}`);
        return this.unwrapEntity<CloudProcessDefinition>(response);
    }

    async getProcessDefinitionMeta(processDefinitionId: string): Promise<ProcessDefinitionMeta> {
        const response = await this.get(`${this.basePath}/process-definitions/${processDefinitionId}/meta`);
        return this.unwrapEntity<ProcessDefinitionMeta>(response);
    }

    async getProcessDefinitionStaticValues(processDefinitionId: string): Promise<Record<string, unknown>> {
        const response = await this.get(`${this.basePath}/process-definitions/${processDefinitionId}/static-values`);
        return this.unwrapMappingResponse(response);
    }

    async getProcessDefinitionConstantValues(processDefinitionId: string): Promise<Record<string, unknown>> {
        const response = await this.get(`${this.basePath}/process-definitions/${processDefinitionId}/constant-values`);
        return this.unwrapMappingResponse(response);
    }

    private unwrapMappingResponse(response: RequestResponse): Record<string, unknown> {
        const { httpStatus, body, ...rest } = response;
        if (httpStatus && httpStatus >= 400) {
            throw new Error(`Mapping values request failed (${httpStatus})`);
        }
        if (body && typeof body === 'object' && !Array.isArray(body)) {
            return body as Record<string, unknown>;
        }
        return rest as Record<string, unknown>;
    }

    async getConnectorDefinitions(): Promise<ConnectorDefinition[]> {
        const response = await this.get(`${this.basePath}/connector-definitions`);
        return this.unwrapList<ConnectorDefinition>(response, 'list');
    }

    async getConnectorDefinitionById(connectorDefinitionId: string): Promise<ConnectorDefinition> {
        const response = await this.get(`${this.basePath}/connector-definitions/${connectorDefinitionId}`);
        return this.unwrapEntity<ConnectorDefinition>(response);
    }

    async startProcessWithVariables(
        processDefinitionKey: string,
        variables: Record<string, unknown>,
        options?: { name?: string; businessKey?: string }
    ): Promise<CloudProcessInstance> {
        return this.startProcess({
            processDefinitionKey,
            variables,
            name: options?.name,
            businessKey: options?.businessKey,
        });
    }

    async getProcessInstance(processInstanceId: string): Promise<CloudProcessInstance> {
        const response = await this.get(
            `${this.basePath}/process-instances/${processInstanceId}`
        );

        return this.unwrapEntity<CloudProcessInstance>(response);
    }

    async waitForProcessInstanceStatus(
        processInstanceId: string,
        expectedStatus: ProcessInstanceStatus
    ): Promise<CloudProcessInstance> {
        return RuntimeBundleService.waitFor(
            () => this.getProcessInstance(processInstanceId),
            (instance) => instance.status === expectedStatus,
            'processStatus',
            `process ${processInstanceId} to reach status ${expectedStatus}`
        );
    }

    async isProcessInstanceNotFoundInRuntime(processInstanceId: string): Promise<boolean> {
        const response = await this.get(
            `${this.basePath}/process-instances/${processInstanceId}`
        );
        return response.httpStatus === 404;
    }

    async waitForProcessInstanceNotFoundInRuntime(processInstanceId: string): Promise<boolean> {
        return RuntimeBundleService.waitFor(
            () => this.isProcessInstanceNotFoundInRuntime(processInstanceId),
            (notFound) => notFound,
            'querySync',
            `process ${processInstanceId} to be removed from runtime`
        );
    }

    async getSubProcesses(parentProcessInstanceId: string): Promise<CloudProcessInstance[]> {
        const response = await this.get(
            `${this.basePath}/process-instances/${parentProcessInstanceId}/subprocesses`
        );
        return this.unwrapList<CloudProcessInstance>(response, 'processInstances');
    }

    async waitForSubProcesses(parentProcessInstanceId: string): Promise<CloudProcessInstance[]> {
        return RuntimeBundleService.waitFor(
            () => this.getSubProcesses(parentProcessInstanceId),
            (subprocesses) => subprocesses.length > 0,
            'querySync',
            `subprocesses of process ${parentProcessInstanceId}`
        );
    }

    async findSubProcesses(
        parentProcessInstanceId: string
    ): Promise<CloudProcessInstance[] | undefined> {
        try {
            return await this.getSubProcesses(parentProcessInstanceId);
        } catch {
            return undefined;
        }
    }

    async getProcessInstances(params?: ProcessQueryParams): Promise<CloudProcessInstance[]> {
        const searchParams = new URLSearchParams();

        if (params?.status) searchParams.append('status', params.status);
        if (params?.processDefinitionKey) searchParams.append('processDefinitionKey', params.processDefinitionKey);
        if (params?.businessKey) searchParams.append('businessKey', params.businessKey);
        if (params?.name) searchParams.append('name', params.name);

        const response = await this.get(
            `${this.basePath}/process-instances?${searchParams.toString()}`
        );

        return this.unwrapList<CloudProcessInstance>(response, 'processInstances');
    }

    async deleteProcessInstance(processInstanceId: string): Promise<void> {
        await this.delete(`${this.basePath}/process-instances/${processInstanceId}`);
    }

    async suspendProcessInstance(processInstanceId: string): Promise<CloudProcessInstance> {
        const response = await this.post(
            `${this.basePath}/process-instances/${processInstanceId}/suspend`,
            { data: {} }
        );
        return this.unwrapEntity<CloudProcessInstance>(response);
    }

    async resumeProcessInstance(processInstanceId: string): Promise<CloudProcessInstance> {
        const response = await this.post(
            `${this.basePath}/process-instances/${processInstanceId}/resume`,
            { data: {} }
        );
        return this.unwrapEntity<CloudProcessInstance>(response);
    }

    async getProcessDefinitions(): Promise<CloudProcessDefinition[]> {
        const response = await this.get(`${this.basePath}/process-definitions`);
        const status = response.httpStatus;
        if (status === 401 || status === 403) {
            throw new Error(
                `Cannot list process definitions (HTTP ${status}). ` +
                    'Preview installs use seeded users (testuser/password) and client activiti with KEYCLOAK_CLIENT_SECRET from the namespace — not vars.KEYCLOAK_USERNAME / secrets.KEYCLOAK_PASSWORD.'
            );
        }
        return this.unwrapList<CloudProcessDefinition>(response, 'processDefinitions');
    }

    async getProcessDefinitionByKey(processDefinitionKey: string): Promise<CloudProcessDefinition> {
        // RB's GET /process-definitions/{id} is keyed by processDefinitionId (e.g. `key:version:uuid`),
        // not by processDefinitionKey. Resolve the key to the deployed definition with the highest
        // appVersion (parity with Serenity's ProcessDefinitionActions#getProcessDefinition).
        const definitions = await this.getProcessDefinitions();
        return RuntimeBundleService.pickHighestVersionByKey(definitions, processDefinitionKey);
    }

    static pickHighestVersionByKey(
        definitions: CloudProcessDefinition[],
        key: string
    ): CloudProcessDefinition {
        const matches = definitions.filter((def) => def.key === key);
        if (matches.length === 0) {
            throw new Error(`No process definition found matching key ${key}`);
        }
        return matches.reduce((best, current) => {
            const bestVersion = parseInt(String(best.appVersion ?? '0'), 10);
            const currentVersion = parseInt(String(current.appVersion ?? '0'), 10);
            return currentVersion > bestVersion ? current : best;
        });
    }

    async getProcessInstanceDiagram(processInstanceId: string): Promise<string> {
        return this.getText(`${this.basePath}/process-instances/${processInstanceId}/model`, {
            Accept: 'image/svg+xml',
        });
    }

    async getProcessDefinitionDiagram(processDefinitionId: string): Promise<string> {
        return this.getText(`${this.basePath}/process-definitions/${processDefinitionId}/model`, {
            Accept: 'image/svg+xml',
        });
    }

    async getSwaggerSpecification(group: string = 'Runtime Bundle'): Promise<string> {
        const root = this.basePath.replace(/\/v1$/, '');
        return this.getText(`${root}/v3/api-docs/${encodeURIComponent(group)}`);
    }

    async getProcessInstanceVariables(processInstanceId: string): Promise<CloudVariableInstance[]> {
        const response = await this.get(
            `${this.basePath}/process-instances/${processInstanceId}/variables`,
            { headers: { 'Content-Type': 'application/json' } }
        );
        return this.unwrapList<CloudVariableInstance>(response, 'variables');
    }

    async getProcessInstanceVariableValue(
        processInstanceId: string,
        variableName: string
    ): Promise<unknown> {
        const variables = await this.getProcessInstanceVariables(processInstanceId);
        return variables.find((variable) => variable.name === variableName)?.value;
    }

    async waitForProcessInstanceVariableValue(
        processInstanceId: string,
        variableName: string,
        expectedValue: unknown
    ): Promise<unknown> {
        return RuntimeBundleService.waitFor(
            () => this.getProcessInstanceVariableValue(processInstanceId, variableName),
            (value) => value === expectedValue,
            'querySync',
            `process variable ${variableName} on process ${processInstanceId} to equal ${String(expectedValue)}`
        );
    }

    async waitForProcessInstanceVariableDeleted(
        processInstanceId: string,
        variableName: string
    ): Promise<unknown> {
        return RuntimeBundleService.waitFor(
            () => this.getProcessInstanceVariableValue(processInstanceId, variableName),
            (value) => value === undefined,
            'querySync',
            `process variable ${variableName} on process ${processInstanceId} to be deleted`
        );
    }

    async waitForProcessInstanceVariablesIncluding(
        processInstanceId: string,
        variableNames: readonly string[]
    ): Promise<CloudVariableInstance[]> {
        return RuntimeBundleService.waitFor(
            () => this.getProcessInstanceVariables(processInstanceId),
            (variables) => {
                const names = new Set(variables.map((variable) => variable.name));
                return variableNames.every((name) => names.has(name));
            },
            'querySync',
            `process ${processInstanceId} variables to include [${variableNames.join(',')}]`
        );
    }

    async waitForProcessInstanceVariableValues(
        processInstanceId: string,
        expected: Record<string, unknown>
    ): Promise<CloudVariableInstance[]> {
        return RuntimeBundleService.waitFor(
            () => this.getProcessInstanceVariables(processInstanceId),
            (variables) =>
                Object.entries(expected).every(
                    ([name, value]) =>
                        variables.find((variable) => variable.name === name)?.value === value
                ),
            'querySync',
            `process ${processInstanceId} variables to match ${JSON.stringify(expected)}`
        );
    }

    async setProcessVariables(
        processInstanceId: string,
        variables: Record<string, unknown>
    ): Promise<void> {
        await this.put(`${this.basePath}/process-instances/${processInstanceId}/variables`, {
            data: {
                payloadType: 'SetProcessVariablesPayload',
                variables,
            },
        });
    }

    async updateProcessInstance(
        processInstanceId: string,
        payload: Omit<UpdateProcessPayload, 'payloadType'>
    ): Promise<CloudProcessInstance> {
        const response = await this.put(`${this.basePath}/process-instances/${processInstanceId}`, {
            data: {
                payloadType: 'UpdateProcessPayload',
                ...payload,
            },
        });
        return this.unwrapEntity<CloudProcessInstance>(response);
    }

    async sendStartMessage(payload: {
        name: string;
        businessKey?: string;
        variables?: Record<string, unknown>;
    }): Promise<CloudProcessInstance> {
        const body = {
            payloadType: 'StartMessagePayload' as const,
            ...payload,
        };

        const response = await this.post(`${this.basePath}/process-instances/message`, { data: body });

        const processInstance = this.unwrapEntity<CloudProcessInstance>(response);
        if (processInstance.id) {
            this.trackCreatedResource(`${this.basePath}/process-instances/${processInstance.id}`);
        }
        return processInstance;
    }

    async trySendStartMessage(payload: {
        name: string;
        businessKey?: string;
        variables?: Record<string, unknown>;
    }): Promise<RequestResponse> {
        const body = {
            payloadType: 'StartMessagePayload' as const,
            ...payload,
        };
        return this.post(`${this.basePath}/process-instances/message`, { data: body });
    }

    async sendReceiveMessage(payload: {
        name: string;
        correlationKey?: string;
        variables?: Record<string, unknown>;
    }): Promise<RequestResponse> {
        const body = {
            payloadType: 'ReceiveMessagePayload' as const,
            ...payload,
        };
        return this.put(`${this.basePath}/process-instances/message`, { data: body });
    }

    async getDeployedProcessDefinitionKeys(): Promise<Set<string>> {
        const definitions = await this.getProcessDefinitions();
        return new Set(definitions.map((definition) => definition.key));
    }

    async getMissingRequiredProcessDefinitionKeys(
        requiredKeys: readonly string[]
    ): Promise<string[]> {
        const deployedKeys = await this.getDeployedProcessDefinitionKeys();
        return requiredKeys.filter((key) => !deployedKeys.has(key));
    }

    /**
     * After runtime-bundle rollout, BPMN auto-deployment can lag behind pod Ready.
     * Poll until required keys are visible on /rb/v1/process-definitions (or timeout).
     */
    async waitForRequiredProcessDefinitions(
        requiredKeys: readonly string[],
        options?: { timeoutMs?: number; intervalMs?: number }
    ): Promise<void> {
        const timeoutMs = options?.timeoutMs ?? RuntimeBundleService.processCatalogPollTimeoutMs();
        const intervalMs = options?.intervalMs ?? 10_000;
        const deadline = Date.now() + timeoutMs;
        let attempt = 0;

        while (Date.now() < deadline) {
            attempt += 1;
            try {
                const missing = await this.getMissingRequiredProcessDefinitionKeys(requiredKeys);
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
                if (!RuntimeBundleService.isTransientProcessCatalogError(error)) {
                    throw error;
                }
                const remainingSec = Math.max(0, Math.ceil((deadline - Date.now()) / 1000));
                console.log(
                    `Waiting for runtime-bundle auth (~${remainingSec}s left): ${error instanceof Error ? error.message.split('\n')[0] : String(error)}`
                );
            }
            await RuntimeBundleService.sleep(intervalMs);
        }

        const missing = await this.getMissingRequiredProcessDefinitionKeys(requiredKeys);
        if (missing.length > 0) {
            throw new Error(RuntimeBundleService.formatMissingProcessCatalogMessage(missing));
        }
    }

    private static formatMissingProcessCatalogMessage(missingKeys: string[]): string {
        return (
            `Missing process definitions on runtime-bundle: ${missingKeys.join(', ')}.\n` +
            'Fix: npm run cluster:prereqs  (ensures activiti/example-runtime-bundle image + acceptance policies)\n' +
            'Or set ACCEPTANCE_RUNTIME_BUNDLE_IMAGE in .env and re-run cluster:prereqs.'
        );
    }

    private static isTransientProcessCatalogError(error: unknown): boolean {
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

    private static processCatalogPollTimeoutMs(): number {
        const configured = Number(process.env.ACCEPTANCE_PROCESS_CATALOG_TIMEOUT_MS);
        if (Number.isFinite(configured) && configured > 0) {
            return configured;
        }
        return process.env.CI === 'true' || process.env.GITHUB_ACTIONS === 'true' ? 300_000 : 120_000;
    }

    private static sleep(ms: number): Promise<void> {
        return new Promise((resolve) => setTimeout(resolve, ms));
    }
}
