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
} from '../../models/runtime-bundle.models';
import { CloudProcessDefinition, ConnectorDefinition, ProcessDefinitionMeta } from '../../models/process-definition.models';
import { CloudVariableInstance } from '../../models/process-variable.models';
import { BaseService, RequestResponse } from '../base.service';
import { CustomAPIRequest } from '../../fixtures/context.models';
import { DirtyContextRegistry } from '../../helpers/dirty-context';
import { TestScope } from '../../helpers/test-isolation';
import {
    RbConnectorDefinitionsEndpoint,
    RbOpenApiSpecEndpoint,
    RbProcessDefinitionsEndpoint,
    RbProcessInstancesEndpoint,
} from './endpoints/index';
import { pickHighestVersionByKey } from './shared/process-definition-utils';

export class RuntimeBundleService extends BaseService {
    readonly processInstances: RbProcessInstancesEndpoint;
    readonly processDefinitions: RbProcessDefinitionsEndpoint;
    readonly connectorDefinitions: RbConnectorDefinitionsEndpoint;
    readonly openApiSpec: RbOpenApiSpecEndpoint;

    private readonly runtimeBasePath: string;

    constructor(context: CustomAPIRequest, runtimeBasePath: string = '/rb') {
        super(context);
        this.runtimeBasePath = runtimeBasePath;
        this.processInstances = new RbProcessInstancesEndpoint(context, runtimeBasePath);
        this.processDefinitions = new RbProcessDefinitionsEndpoint(context, runtimeBasePath);
        this.connectorDefinitions = new RbConnectorDefinitionsEndpoint(context, runtimeBasePath);
        this.openApiSpec = new RbOpenApiSpecEndpoint(context, runtimeBasePath);
    }

    attachIsolation(dirtyRegistry?: DirtyContextRegistry, testScope?: TestScope, basePath?: string): void {
        super.attachIsolation(dirtyRegistry, testScope);
        const isolationBasePath = basePath ?? `${this.runtimeBasePath.replace(/\/$/, '')}/v1`;
        this.processInstances.attachIsolation(dirtyRegistry, testScope, isolationBasePath);
    }

    async startProcess(payload: Omit<StartProcessPayload, 'payloadType'>): Promise<CloudProcessInstance> {
        return this.processInstances.startProcess(payload);
    }

    async createProcess(payload: Omit<CreateProcessInstancePayload, 'payloadType'>): Promise<CloudProcessInstance> {
        return this.processInstances.createProcess(payload);
    }

    async startCreatedProcess(processInstanceId: string): Promise<CloudProcessInstance> {
        return this.processInstances.startCreatedProcess(processInstanceId);
    }

    async sendSignal(name: string, variables?: Record<string, unknown>): Promise<RequestResponse> {
        return this.processInstances.sendSignal(name, variables);
    }

    async getHomeInfo(): Promise<Record<string, unknown>> {
        return this.openApiSpec.getHomeInfo();
    }

    async getProcessDefinitionById(processDefinitionId: string): Promise<CloudProcessDefinition> {
        return this.processDefinitions.getProcessDefinitionById(processDefinitionId);
    }

    async getProcessDefinitionMeta(processDefinitionId: string): Promise<ProcessDefinitionMeta> {
        return this.processDefinitions.getProcessDefinitionMeta(processDefinitionId);
    }

    async getProcessDefinitionStaticValues(processDefinitionId: string): Promise<Record<string, unknown>> {
        return this.processDefinitions.getProcessDefinitionStaticValues(processDefinitionId);
    }

    async getProcessDefinitionConstantValues(processDefinitionId: string): Promise<Record<string, unknown>> {
        return this.processDefinitions.getProcessDefinitionConstantValues(processDefinitionId);
    }

    async getConnectorDefinitions(): Promise<ConnectorDefinition[]> {
        return this.connectorDefinitions.getConnectorDefinitions();
    }

    async getConnectorDefinitionById(connectorDefinitionId: string): Promise<ConnectorDefinition> {
        return this.connectorDefinitions.getConnectorDefinitionById(connectorDefinitionId);
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
        return this.processInstances.getProcessInstance(processInstanceId);
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
        const response = await this.processInstances.getProcessInstanceResponse(processInstanceId);
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
        return this.processInstances.getSubProcesses(parentProcessInstanceId);
    }

    async waitForSubProcesses(parentProcessInstanceId: string): Promise<CloudProcessInstance[]> {
        return RuntimeBundleService.waitFor(
            () => this.getSubProcesses(parentProcessInstanceId),
            (subprocesses) => subprocesses.length > 0,
            'querySync',
            `subprocesses of process ${parentProcessInstanceId}`
        );
    }

    async findSubProcesses(parentProcessInstanceId: string): Promise<CloudProcessInstance[] | undefined> {
        try {
            return await this.getSubProcesses(parentProcessInstanceId);
        } catch {
            return undefined;
        }
    }

    async getProcessInstances(params?: ProcessQueryParams): Promise<CloudProcessInstance[]> {
        return this.processInstances.getProcessInstances(params);
    }

    async deleteProcessInstance(processInstanceId: string): Promise<void> {
        return this.processInstances.deleteProcessInstance(processInstanceId);
    }

    async suspendProcessInstance(processInstanceId: string): Promise<CloudProcessInstance> {
        return this.processInstances.suspendProcessInstance(processInstanceId);
    }

    async resumeProcessInstance(processInstanceId: string): Promise<CloudProcessInstance> {
        return this.processInstances.resumeProcessInstance(processInstanceId);
    }

    async getProcessDefinitions(): Promise<CloudProcessDefinition[]> {
        return this.processDefinitions.getProcessDefinitions();
    }

    async getProcessDefinitionByKey(processDefinitionKey: string): Promise<CloudProcessDefinition> {
        const definitions = await this.getProcessDefinitions();
        return pickHighestVersionByKey(definitions, processDefinitionKey);
    }

    async getProcessInstanceDiagram(processInstanceId: string): Promise<string> {
        return this.processInstances.getProcessInstanceDiagram(processInstanceId);
    }

    async getProcessDefinitionDiagram(processDefinitionId: string): Promise<string> {
        return this.processDefinitions.getProcessDefinitionDiagram(processDefinitionId);
    }

    async getSwaggerSpecification(group: string = 'Runtime Bundle'): Promise<string> {
        return this.openApiSpec.getSwaggerSpecification(group);
    }

    async getProcessInstanceVariables(processInstanceId: string): Promise<CloudVariableInstance[]> {
        return this.processInstances.getProcessInstanceVariables(processInstanceId);
    }

    async getProcessInstanceVariableValue(processInstanceId: string, variableName: string): Promise<unknown> {
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

    async waitForProcessInstanceVariableDeleted(processInstanceId: string, variableName: string): Promise<unknown> {
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
                    ([name, value]) => variables.find((variable) => variable.name === name)?.value === value
                ),
            'querySync',
            `process ${processInstanceId} variables to match ${JSON.stringify(expected)}`
        );
    }

    async setProcessVariables(processInstanceId: string, variables: Record<string, unknown>): Promise<void> {
        return this.processInstances.setProcessVariables(processInstanceId, variables);
    }

    async updateProcessInstance(
        processInstanceId: string,
        payload: Omit<UpdateProcessPayload, 'payloadType'>
    ): Promise<CloudProcessInstance> {
        return this.processInstances.updateProcessInstance(processInstanceId, payload);
    }

    async sendStartMessage(payload: {
        name: string;
        businessKey?: string;
        variables?: Record<string, unknown>;
    }): Promise<CloudProcessInstance> {
        return this.processInstances.sendStartMessage(payload);
    }

    async trySendStartMessage(payload: {
        name: string;
        businessKey?: string;
        variables?: Record<string, unknown>;
    }): Promise<RequestResponse> {
        return this.processInstances.trySendStartMessage(payload);
    }

    async sendReceiveMessage(payload: {
        name: string;
        correlationKey?: string;
        variables?: Record<string, unknown>;
    }): Promise<RequestResponse> {
        return this.processInstances.sendReceiveMessage(payload);
    }

    async getDeployedProcessDefinitionKeys(): Promise<Set<string>> {
        const definitions = await this.getProcessDefinitions();
        return new Set(definitions.map((definition) => definition.key));
    }

    async getMissingRequiredProcessDefinitionKeys(requiredKeys: readonly string[]): Promise<string[]> {
        const deployedKeys = await this.getDeployedProcessDefinitionKeys();
        return requiredKeys.filter((key) => !deployedKeys.has(key));
    }

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
