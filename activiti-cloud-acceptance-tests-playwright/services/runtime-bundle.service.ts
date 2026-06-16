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
    StartProcessPayload,
    ProcessQueryParams,
    UpdateProcessPayload,
} from '../models/runtime-bundle.models';
import { CloudProcessDefinition } from '../models/process-definition.models';
import { CloudVariableInstance } from '../models/process-variable.models';
import { pickHighestVersionByKey } from '../helpers/process-definition';
import { BaseService, RequestResponse } from './base.service';
import { CustomAPIRequest } from '../fixtures/context.models';

export class RuntimeBundleService extends BaseService {
    private readonly basePath: string;

    constructor(context: CustomAPIRequest, runtimeBasePath: string = '/rb') {
        super(context);
        this.basePath = `${runtimeBasePath.replace(/\/$/, '')}/v1`;
    }

    async startProcess(payload: Omit<StartProcessPayload, 'payloadType'>): Promise<CloudProcessInstance> {
        const { name, businessKey, ...rest } = payload;
        const body = {
            payloadType: 'StartProcessPayload' as const,
            ...rest,
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

    async isProcessInstanceNotFoundInRuntime(processInstanceId: string): Promise<boolean> {
        const response = await this.get(
            `${this.basePath}/process-instances/${processInstanceId}`
        );
        return response.httpStatus === 404;
    }

    async getSubProcesses(parentProcessInstanceId: string): Promise<CloudProcessInstance[]> {
        const response = await this.get(
            `${this.basePath}/process-instances/${parentProcessInstanceId}/subprocesses`
        );
        return this.unwrapList<CloudProcessInstance>(response, 'processInstances');
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
        return pickHighestVersionByKey(definitions, processDefinitionKey);
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
}
