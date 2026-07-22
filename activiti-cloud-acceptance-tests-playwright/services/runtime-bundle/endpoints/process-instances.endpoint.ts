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
    CreateProcessInstancePayload,
    ProcessQueryParams,
    StartProcessPayload,
    UpdateProcessPayload,
} from '../../../models/runtime-bundle.models';
import { CloudVariableInstance } from '../../../models/process-variable.models';
import { CustomAPIRequest } from '../../../fixtures/context.models';
import { BaseService, RequestResponse } from '../../base.service';
import { rbV1Base } from './rb-base-path';

export class RbProcessInstancesEndpoint extends BaseService {
    private readonly basePath: string;

    constructor(context: CustomAPIRequest, runtimeBasePath: string = '/rb') {
        super(context);
        this.basePath = rbV1Base(runtimeBasePath);
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
        if (!processDefinitionKey && !rest.processDefinitionId) {
            throw new Error('createProcess requires processDefinitionKey or processDefinitionId');
        }
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
        const body = {
            payloadType: 'SignalPayload' as const,
            name,
            ...(variables ? { variables } : {}),
        };
        return this.post(`${this.basePath}/process-instances/signal`, { data: body });
    }

    async getProcessInstance(processInstanceId: string): Promise<CloudProcessInstance> {
        const response = await this.get(`${this.basePath}/process-instances/${processInstanceId}`);
        return this.unwrapEntity<CloudProcessInstance>(response);
    }

    async getProcessInstanceResponse(processInstanceId: string): Promise<RequestResponse> {
        return this.get(`${this.basePath}/process-instances/${processInstanceId}`);
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

        const response = await this.get(`${this.basePath}/process-instances?${searchParams.toString()}`);
        return this.unwrapList<CloudProcessInstance>(response, 'processInstances');
    }

    async deleteProcessInstance(processInstanceId: string): Promise<void> {
        await this.delete(`${this.basePath}/process-instances/${processInstanceId}`);
    }

    async suspendProcessInstance(processInstanceId: string): Promise<CloudProcessInstance> {
        const response = await this.post(`${this.basePath}/process-instances/${processInstanceId}/suspend`, {
            data: {},
        });
        return this.unwrapEntity<CloudProcessInstance>(response);
    }

    async resumeProcessInstance(processInstanceId: string): Promise<CloudProcessInstance> {
        const response = await this.post(`${this.basePath}/process-instances/${processInstanceId}/resume`, {
            data: {},
        });
        return this.unwrapEntity<CloudProcessInstance>(response);
    }

    async getProcessInstanceDiagram(processInstanceId: string): Promise<string> {
        return this.getText(`${this.basePath}/process-instances/${processInstanceId}/model`, {
            Accept: 'image/svg+xml',
        });
    }

    async getProcessInstanceVariables(processInstanceId: string): Promise<CloudVariableInstance[]> {
        const response = await this.get(`${this.basePath}/process-instances/${processInstanceId}/variables`, {
            headers: { 'Content-Type': 'application/json' },
        });
        return this.unwrapList<CloudVariableInstance>(response, 'variables');
    }

    async setProcessVariables(processInstanceId: string, variables: Record<string, unknown>): Promise<void> {
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
