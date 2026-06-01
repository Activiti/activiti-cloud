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

import { CloudProcessInstance, ProcessQueryParams } from '../models/runtime-bundle.models';
import { CloudProcessDefinition } from '../models/process-definition.models';
import {
    CloudIntegrationContext,
    CloudServiceTask,
    ServiceTaskQueryParams,
} from '../models/service-task.models';
import { BaseService } from './base.service';
import { CustomAPIRequest } from '../fixtures/context.models';
import { toProcessQueryString, toServiceTaskQueryString } from './query-params';

export class QueryAdminService extends BaseService {
    private readonly basePath = '/query/admin/v1';

    constructor(context: CustomAPIRequest) {
        super(context);
    }

    async getAllProcessInstancesAdmin(): Promise<CloudProcessInstance[]> {
        const response = await this.get(`${this.basePath}/process-instances`);
        return this.unwrapList<CloudProcessInstance>(response, 'processInstances');
    }

    async getProcessInstancesAdminWithParams(params?: ProcessQueryParams): Promise<CloudProcessInstance[]> {
        const query = toProcessQueryString(params);
        const response = await this.get(
            `${this.basePath}/process-instances${query ? `?${query}` : ''}`
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

    async getServiceTasksForProcessInstance(processInstanceId: string): Promise<CloudServiceTask[]> {
        const response = await this.get(
            `${this.basePath}/process-instances/${processInstanceId}/service-tasks`
        );
        return this.unwrapList<CloudServiceTask>(response, 'serviceTasks');
    }

    async getServiceTasksByStatus(
        processInstanceId: string,
        status: string
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

    async getServiceTasksByQuery(params?: ServiceTaskQueryParams): Promise<CloudServiceTask[]> {
        const query = toServiceTaskQueryString(params);
        const response = await this.get(`${this.basePath}/service-tasks${query ? `?${query}` : ''}`);
        return this.unwrapList<CloudServiceTask>(response, 'serviceTasks');
    }

    async getIntegrationContext(serviceTaskId: string): Promise<CloudIntegrationContext> {
        const response = await this.get(`${this.basePath}/service-tasks/${serviceTaskId}/integration-context`);
        return this.unwrapEntity<CloudIntegrationContext>(response);
    }

    async getAllIntegrationContexts(serviceTaskId: string): Promise<CloudIntegrationContext[]> {
        const response = await this.get(`${this.basePath}/service-tasks/${serviceTaskId}/integration-contexts`);
        return this.unwrapList<CloudIntegrationContext>(response, 'integrationContexts');
    }
}
