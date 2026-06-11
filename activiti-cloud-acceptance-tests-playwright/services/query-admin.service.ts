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
    CloudIntegrationContext,
    CloudProcessInstance,
    CloudServiceTask,
    ProcessQueryParams,
    ServiceTaskStatus,
} from '../models/runtime-bundle.models';
import { CloudProcessDefinition } from '../models/process-definition.models';
import { CloudTask } from '../models/task.models';
import { BaseService } from './base.service';
import { CustomAPIRequest } from '../fixtures/context.models';

export class QueryAdminService extends BaseService {
    private readonly basePath = '/query/admin/v1';

    constructor(context: CustomAPIRequest) {
        super(context);
    }

    async getAllProcessInstancesAdmin(): Promise<CloudProcessInstance[]> {
        const response = await this.get(`${this.basePath}/process-instances`);
        return this.unwrapList<CloudProcessInstance>(response, 'processInstances');
    }

    async getProcessInstanceAdmin(processInstanceId: string): Promise<CloudProcessInstance> {
        const response = await this.get(`${this.basePath}/process-instances/${processInstanceId}`);
        return this.unwrapEntity<CloudProcessInstance>(response);
    }

    async getProcessInstancesAdminWithParams(params?: ProcessQueryParams): Promise<CloudProcessInstance[]> {
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

    async getAllProcessDefinitionsAdmin(): Promise<CloudProcessDefinition[]> {
        const response = await this.get(`${this.basePath}/process-definitions`);
        return this.unwrapList<CloudProcessDefinition>(response, 'processDefinitions');
    }

    async getProcessModel(processDefinitionId: string): Promise<string> {
        return this.getText(`${this.basePath}/process-definitions/${processDefinitionId}/model`);
    }

    async getServiceTasksForProcessInstance(processInstanceId: string): Promise<CloudServiceTask[]> {
        const response = await this.get(
            `${this.basePath}/process-instances/${processInstanceId}/service-tasks`
        );
        return this.unwrapList<CloudServiceTask>(response, 'serviceTasks');
    }

    async getServiceTasksByStatusForProcessInstance(
        processInstanceId: string,
        status: ServiceTaskStatus | string
    ): Promise<CloudServiceTask[]> {
        const response = await this.get(
            `${this.basePath}/process-instances/${processInstanceId}/service-tasks?status=${encodeURIComponent(status)}`
        );
        return this.unwrapList<CloudServiceTask>(response, 'serviceTasks');
    }

    async getServiceTaskById(serviceTaskId: string): Promise<CloudServiceTask> {
        const response = await this.get(`${this.basePath}/service-tasks/${serviceTaskId}`);
        return this.unwrapEntity<CloudServiceTask>(response);
    }

    async getServiceTaskIntegrationContext(serviceTaskId: string): Promise<CloudIntegrationContext> {
        const response = await this.get(
            `${this.basePath}/service-tasks/${serviceTaskId}/integration-context`
        );
        return this.unwrapEntity<CloudIntegrationContext>(response);
    }

    async getServiceTaskIntegrationContexts(serviceTaskId: string): Promise<CloudIntegrationContext[]> {
        const response = await this.get(
            `${this.basePath}/service-tasks/${serviceTaskId}/integration-contexts`
        );
        return this.unwrapList<CloudIntegrationContext>(response, 'cloudIntegrationContexts');
    }

    async getServiceTasksByQuery(params: {
        processDefinitionKey?: string;
        status?: ServiceTaskStatus | string;
    }): Promise<CloudServiceTask[]> {
        const searchParams = new URLSearchParams();
        if (params.processDefinitionKey) searchParams.append('processDefinitionKey', params.processDefinitionKey);
        if (params.status) searchParams.append('status', String(params.status));
        const response = await this.get(`${this.basePath}/service-tasks?${searchParams.toString()}`);
        return this.unwrapList<CloudServiceTask>(response, 'serviceTasks');
    }

    async getAllTasksAdmin(): Promise<CloudTask[]> {
        const response = await this.get(`${this.basePath}/tasks`);
        return this.unwrapList<CloudTask>(response, 'tasks');
    }

    async deleteAllProcessInstancesAdmin(): Promise<CloudProcessInstance[]> {
        const response = await this.delete(`${this.basePath}/process-instances`);
        return this.unwrapList<CloudProcessInstance>(response, 'processInstances');
    }

    async deleteAllTasksAdmin(): Promise<CloudTask[]> {
        const response = await this.delete(`${this.basePath}/tasks`);
        return this.unwrapList<CloudTask>(response, 'tasks');
    }
}
