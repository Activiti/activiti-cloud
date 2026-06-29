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
    ProcessInstanceSearchRequest,
    ProcessQueryParams,
} from '../../../models/runtime-bundle.models';
import { CloudVariableInstance } from '../../../models/process-variable.models';
import { CloudTask } from '../../../models/task.models';
import { SearchPageParams } from '../../../models/base-service.models';
import { CustomAPIRequest } from '../../../fixtures/context.models';
import { BaseService } from '../../base.service';
import { parseCountResponse, searchEndpoint } from '../shared/search-utils';
import { queryV1Base } from './query-base-path';

export class QueryProcessInstancesEndpoint extends BaseService {
    private readonly basePath: string;

    constructor(context: CustomAPIRequest, admin: boolean = false) {
        super(context);
        this.basePath = queryV1Base(admin);
    }

    async getAllProcessInstances(): Promise<CloudProcessInstance[]> {
        const response = await this.get(`${this.basePath}/process-instances`);
        return this.unwrapList<CloudProcessInstance>(response, 'processInstances');
    }

    async getProcessInstance(processInstanceId: string): Promise<CloudProcessInstance> {
        const response = await this.get(`${this.basePath}/process-instances/${processInstanceId}`);
        return this.unwrapEntity<CloudProcessInstance>(response);
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

    async getProcessInstancesWithVariableKeys(variableKeys: string): Promise<CloudProcessInstance[]> {
        const response = await this.get(
            `${this.basePath}/process-instances?variableKeys=${encodeURIComponent(variableKeys)}`
        );
        return this.unwrapList<CloudProcessInstance>(response, 'processInstances');
    }

    async getProcessInstanceDiagram(processInstanceId: string): Promise<string> {
        return this.getText(`${this.basePath}/process-instances/${processInstanceId}/diagram`, {
            Accept: 'image/svg+xml',
        });
    }

    async getProcessInstanceDiagramStatus(processInstanceId: string): Promise<number> {
        const response = await this.get(`${this.basePath}/process-instances/${processInstanceId}/diagram`, {
            headers: { Accept: 'image/svg+xml' },
        });
        return response.httpStatus ?? 200;
    }

    async getProcessModel(processDefinitionId: string): Promise<string> {
        return this.getText(`${this.basePath}/process-definitions/${processDefinitionId}/model`);
    }

    async getProcessInstanceVariables(processInstanceId: string): Promise<CloudVariableInstance[]> {
        const response = await this.get(`${this.basePath}/process-instances/${processInstanceId}/variables`);
        return this.unwrapList<CloudVariableInstance>(response, 'variables');
    }

    async getSubprocesses(processInstanceId: string): Promise<CloudProcessInstance[]> {
        const response = await this.get(`${this.basePath}/process-instances/${processInstanceId}/subprocesses`);
        return this.unwrapList<CloudProcessInstance>(response, 'processInstances');
    }

    async getSequenceFlows(processInstanceId: string): Promise<Record<string, unknown>[]> {
        const response = await this.get(`${this.basePath}/process-instances/${processInstanceId}/sequence-flows`);
        return this.unwrapList<Record<string, unknown>>(response, 'list');
    }

    async getBpmnActivities(processInstanceId: string): Promise<Record<string, unknown>[]> {
        const response = await this.get(`${this.basePath}/process-instances/${processInstanceId}/bpmn-activities`);
        return this.unwrapList<Record<string, unknown>>(response, 'list');
    }

    async getLinkedProcesses(linkedProcessInstanceId: string): Promise<CloudProcessInstance[]> {
        const response = await this.get(`${this.basePath}/process-instances/${linkedProcessInstanceId}/linkedprocesses`);
        return this.unwrapList<CloudProcessInstance>(response, 'processInstances');
    }

    async getProcessInstanceAppVersions(): Promise<string[]> {
        const response = await this.get(`${this.basePath}/process-instances/appVersions`);
        if (Array.isArray(response)) {
            return response as string[];
        }
        return [];
    }

    async getTasksByProcessInstanceId(processInstanceId: string): Promise<CloudTask[]> {
        const response = await this.get(`${this.basePath}/process-instances/${processInstanceId}/tasks`);
        return this.unwrapList<CloudTask>(response, 'tasks');
    }

    async searchProcessInstances(
        searchRequest: ProcessInstanceSearchRequest = {},
        page?: SearchPageParams
    ): Promise<CloudProcessInstance[]> {
        const response = await this.post(searchEndpoint(`${this.basePath}/process-instances/search`, page), {
            data: searchRequest,
        });
        return this.unwrapList<CloudProcessInstance>(response, 'processInstances');
    }

    async countProcessInstances(
        searchRequest: ProcessInstanceSearchRequest = {},
        page?: SearchPageParams
    ): Promise<number> {
        const response = await this.post(searchEndpoint(`${this.basePath}/process-instances/count`, page), {
            data: searchRequest,
        });
        return parseCountResponse(response);
    }

    async linkProcessInstances(
        mainProcessInstanceId: string,
        processInstanceIds: string[],
        linkProcessInstanceType: string
    ): Promise<void> {
        await this.post(`${this.basePath}/process-instances/${mainProcessInstanceId}/link`, {
            data: {
                processInstanceIds,
                linkProcessInstanceType,
            },
        });
    }

    async deleteAllProcessInstances(): Promise<CloudProcessInstance[]> {
        const response = await this.delete(`${this.basePath}/process-instances`);
        return this.unwrapList<CloudProcessInstance>(response, 'processInstances');
    }
}
