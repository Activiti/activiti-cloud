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

import { CloudVariableInstance } from '../../../models/process-variable.models';
import { CloudTask, TaskQueryParams, TaskSearchRequest } from '../../../models/task.models';
import { SearchPageParams } from '../../../models/base-service.models';
import { CustomAPIRequest } from '../../../fixtures/context.models';
import { BaseService } from '../../base.service';
import { parseCountResponse, searchEndpoint, toTaskSearchBody } from '../shared/search-utils';
import { QUERY_V1_BASE } from './process-instances.endpoint';

export class QueryTasksEndpoint extends BaseService {
    constructor(context: CustomAPIRequest) {
        super(context);
    }

    async getAllTasks(): Promise<CloudTask[]> {
        const response = await this.get(`${QUERY_V1_BASE}/tasks`);
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

        const response = await this.get(`${QUERY_V1_BASE}/tasks?${searchParams.toString()}`);
        return this.unwrapList<CloudTask>(response, 'tasks');
    }

    async getTaskById(taskId: string): Promise<CloudTask | undefined> {
        const response = await this.get(`${QUERY_V1_BASE}/tasks?id=${encodeURIComponent(taskId)}`);
        const tasks = this.unwrapList<CloudTask>(response, 'tasks');
        return tasks[0];
    }

    async getTask(taskId: string): Promise<CloudTask> {
        const response = await this.get(`${QUERY_V1_BASE}/tasks/${encodeURIComponent(taskId)}`);
        return this.unwrapEntity<CloudTask>(response);
    }

    async getStandaloneTasks(): Promise<CloudTask[]> {
        const response = await this.get(`${QUERY_V1_BASE}/tasks?standalone=true&sort=createdDate,desc&sort=id,desc`);
        return this.unwrapList<CloudTask>(response, 'tasks');
    }

    async getTasksByNameAndDescription(namePrefix: string, descriptionPrefix: string): Promise<CloudTask[]> {
        const response = await this.get(
            `${QUERY_V1_BASE}/tasks?name=${encodeURIComponent(namePrefix)}&description=${encodeURIComponent(descriptionPrefix)}`
        );
        return this.unwrapList<CloudTask>(response, 'tasks');
    }

    async getCandidateUsers(taskId: string): Promise<string[]> {
        const response = await this.get(`${QUERY_V1_BASE}/tasks/${taskId}/candidate-users`);
        if (Array.isArray(response)) {
            return response as string[];
        }
        if (Array.isArray(response.body)) {
            return response.body as string[];
        }
        return [];
    }

    async getCandidateGroups(taskId: string): Promise<string[]> {
        const response = await this.get(`${QUERY_V1_BASE}/tasks/${taskId}/candidate-groups`);
        if (Array.isArray(response)) {
            return response as string[];
        }
        if (Array.isArray(response.body)) {
            return response.body as string[];
        }
        return [];
    }

    async getTaskVariables(taskId: string): Promise<CloudVariableInstance[]> {
        const response = await this.get(`${QUERY_V1_BASE}/tasks/${taskId}/variables`);
        return this.unwrapList<CloudVariableInstance>(response, 'variables');
    }

    async getRootTasksByProcessInstance(processInstanceId: string): Promise<CloudTask[]> {
        const response = await this.get(
            `${QUERY_V1_BASE}/tasks?rootTasksOnly=true&processInstanceId=${encodeURIComponent(processInstanceId)}&sort=createdDate,desc&sort=id,desc`
        );
        return this.unwrapList<CloudTask>(response, 'tasks');
    }

    async searchTasks(searchRequest: TaskSearchRequest = {}, page?: SearchPageParams): Promise<CloudTask[]> {
        const response = await this.post(searchEndpoint(`${QUERY_V1_BASE}/tasks/search`, page), {
            data: toTaskSearchBody(searchRequest),
        });
        return this.unwrapList<CloudTask>(response, 'tasks');
    }

    async countTasks(searchRequest: TaskSearchRequest = {}, page?: SearchPageParams): Promise<number> {
        const response = await this.post(searchEndpoint(`${QUERY_V1_BASE}/tasks/count`, page), {
            data: toTaskSearchBody(searchRequest),
        });
        return parseCountResponse(response);
    }
}
