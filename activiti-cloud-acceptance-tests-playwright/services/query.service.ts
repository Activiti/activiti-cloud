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
import { CloudProcessDefinition } from '../models/process-definition.models';
import { CloudVariableInstance } from '../models/process-variable.models';
import { CloudTask, TaskQueryParams, TaskStatus } from '../models/task.models';
import { BaseService } from './base.service';
import { CustomAPIRequest } from '../fixtures/context.models';

export class QueryService extends BaseService {
    private readonly basePath = '/query/v1';

    constructor(context: CustomAPIRequest) {
        super(context);
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

        const response = await this.get(
            `${this.basePath}/process-instances?${searchParams.toString()}`
        );

        return this.unwrapList<CloudProcessInstance>(response, 'processInstances');
    }

    async getAllTasks(): Promise<CloudTask[]> {
        const response = await this.get(`${this.basePath}/tasks`);
        return this.unwrapList<CloudTask>(response, 'tasks');
    }

    async getTasks(params?: TaskQueryParams): Promise<CloudTask[]> {
        const searchParams = new URLSearchParams();

        if (params?.status) searchParams.append('status', params.status);
        if (params?.assignee) searchParams.append('assignee', params.assignee);
        if (params?.owner) searchParams.append('owner', params.owner);
        if (params?.processInstanceId) searchParams.append('processInstanceId', params.processInstanceId);
        if (params?.processDefinitionKey) searchParams.append('processDefinitionKey', params.processDefinitionKey);
        if (params?.name) searchParams.append('name', params.name);

        const response = await this.get(
            `${this.basePath}/tasks?${searchParams.toString()}`
        );

        return this.unwrapList<CloudTask>(response, 'tasks');
    }

    async getProcessDefinitions(): Promise<CloudProcessDefinition[]> {
        const response = await this.get(`${this.basePath}/process-definitions`);
        return this.unwrapList<CloudProcessDefinition>(response, 'processDefinitions');
    }

    async getProcessInstanceDiagram(processInstanceId: string): Promise<string> {
        return this.getText(`${this.basePath}/process-instances/${processInstanceId}/diagram`, {
            Accept: 'image/svg+xml',
        });
    }

    async getProcessInstanceVariables(processInstanceId: string): Promise<CloudVariableInstance[]> {
        const response = await this.get(`${this.basePath}/process-instances/${processInstanceId}/variables`);
        return this.unwrapList<CloudVariableInstance>(response, 'variables');
    }

    async getProcessInstancesByName(namePattern: string): Promise<CloudProcessInstance[]> {
        return this.getProcessInstances({ name: namePattern });
    }

    async getTaskById(taskId: string): Promise<CloudTask | undefined> {
        const response = await this.get(`${this.basePath}/tasks?id=${encodeURIComponent(taskId)}`);
        const tasks = this.unwrapList<CloudTask>(response, 'tasks');
        return tasks[0];
    }

    async getTasksByProcessInstanceId(processInstanceId: string): Promise<CloudTask[]> {
        const response = await this.get(`${this.basePath}/process-instances/${processInstanceId}/tasks`);
        return this.unwrapList<CloudTask>(response, 'tasks');
    }

    async queryTaskByIdAndStatus(taskId: string, status: TaskStatus): Promise<CloudTask[]> {
        const response = await this.get(
            `${this.basePath}/tasks?status=${status}&id=${encodeURIComponent(taskId)}`
        );
        return this.unwrapList<CloudTask>(response, 'tasks');
    }

    async getStandaloneTasks(): Promise<CloudTask[]> {
        const response = await this.get(
            `${this.basePath}/tasks?standalone=true&sort=createdDate,desc&sort=id,desc`
        );
        return this.unwrapList<CloudTask>(response, 'tasks');
    }

    async getTasksByNameAndDescription(namePrefix: string, descriptionPrefix: string): Promise<CloudTask[]> {
        const response = await this.get(
            `${this.basePath}/tasks?name=${encodeURIComponent(namePrefix)}&description=${encodeURIComponent(descriptionPrefix)}`
        );
        return this.unwrapList<CloudTask>(response, 'tasks');
    }

    async getCandidateUsers(taskId: string): Promise<string[]> {
        const response = await this.get(`${this.basePath}/tasks/${taskId}/candidate-users`);
        if (Array.isArray(response)) {
            return response as string[];
        }
        if (Array.isArray(response.body)) {
            return response.body as string[];
        }
        return [];
    }
}
