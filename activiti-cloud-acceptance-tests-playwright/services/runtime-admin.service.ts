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

import { CloudProcessInstance, ProcessQueryParams } from '../models/runtime-bundle.models';
import { BaseService, RequestResponse } from './base.service';
import { CustomAPIRequest } from '../fixtures/context.models';

export class RuntimeAdminService extends BaseService {
    private readonly basePath = '/rb/admin/v1';

    constructor(context: CustomAPIRequest) {
        super(context);
    }

    async getAllProcessInstances(): Promise<CloudProcessInstance[]> {
        const response = await this.get(`${this.basePath}/process-instances`);
        return this.unwrapList<CloudProcessInstance>(response, 'processInstances');
    }

    async getProcessInstancesWithParams(params?: ProcessQueryParams): Promise<CloudProcessInstance[]> {
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

    async destroyProcessInstance(processInstanceId: string, force = true): Promise<void> {
        await this.delete(`${this.basePath}/process-instances/${processInstanceId}/destroy?force=${force}`);
    }

    async replayServiceTask(executionId: string, flowNodeId: string): Promise<RequestResponse> {
        return this.post(`${this.basePath}/executions/${executionId}/replay/service-task`, {
            data: { flowNodeId },
        });
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

    async deleteProcessVariables(processInstanceId: string, variableNames: string[]): Promise<void> {
        await this.delete(`${this.basePath}/process-instances/${processInstanceId}/variables`, {
            data: {
                payloadType: 'RemoveProcessVariablesPayload',
                variableNames,
            },
        });
    }
}
