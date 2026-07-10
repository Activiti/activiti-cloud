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
import { queryV1Base } from './query-base-path';

export class QueryTasksEndpoint extends BaseService {
    private readonly basePath: string;

    constructor(context: CustomAPIRequest, admin: boolean = false) {
        super(context);
        this.basePath = queryV1Base(admin);
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

        const response = await this.get(`${this.basePath}/tasks?${searchParams.toString()}`);
        return this.unwrapList<CloudTask>(response, 'tasks');
    }

    async getTasksWithVariableKeys(variableKeys: string): Promise<CloudTask[]> {
        const response = await this.get(`${this.basePath}/tasks?variableKeys=${encodeURIComponent(variableKeys)}`);
        return this.unwrapList<CloudTask>(response, 'tasks');
    }

    async getTasksFiltered(filters: {
        processInstanceId?: string;
        status?: string;
        id?: string;
        skipCount?: number;
        maxItems?: number;
        sort?: string[];
    }): Promise<CloudTask[]> {
        const params = new URLSearchParams();
        if (filters.processInstanceId) {
            params.set('processInstanceId', filters.processInstanceId);
        }
        if (filters.status) {
            params.set('status', filters.status);
        }
        if (filters.id) {
            params.set('id', filters.id);
        }
        if (filters.skipCount !== undefined) {
            params.set('skipCount', String(filters.skipCount));
        }
        if (filters.maxItems !== undefined) {
            params.set('maxItems', String(filters.maxItems));
        }
        for (const sort of filters.sort ?? []) {
            params.append('sort', sort);
        }
        const response = await this.get(`${this.basePath}/tasks?${params.toString()}`);
        return this.unwrapList<CloudTask>(response, 'tasks');
    }

    async getTaskById(taskId: string): Promise<CloudTask | undefined> {
        const response = await this.get(`${this.basePath}/tasks?id=${encodeURIComponent(taskId)}`);
        const tasks = this.unwrapList<CloudTask>(response, 'tasks');
        return tasks[0];
    }

    async getTask(taskId: string): Promise<CloudTask> {
        const response = await this.get(`${this.basePath}/tasks/${encodeURIComponent(taskId)}`);
        return this.unwrapEntity<CloudTask>(response);
    }

    async getStandaloneTasks(): Promise<CloudTask[]> {
        const response = await this.get(`${this.basePath}/tasks?standalone=true&sort=createdDate,desc&sort=id,desc`);
        return this.unwrapList<CloudTask>(response, 'tasks');
    }

    async getTasksByNameAndDescription(namePrefix: string, descriptionPrefix: string): Promise<CloudTask[]> {
        const response = await this.get(
            `${this.basePath}/tasks?name=${encodeURIComponent(namePrefix)}&description=${encodeURIComponent(descriptionPrefix)}`
        );
        return this.unwrapList<CloudTask>(response, 'tasks');
    }

    async getCandidateUsers(taskId: string): Promise<string[]> {
        const response = await this.get(`${this.basePath}/tasks/${encodeURIComponent(taskId)}/candidate-users`);
        if (Array.isArray(response)) {
            return response as string[];
        }
        if (Array.isArray(response.body)) {
            return response.body as string[];
        }
        return [];
    }

    async getCandidateGroups(taskId: string): Promise<string[]> {
        const response = await this.get(`${this.basePath}/tasks/${encodeURIComponent(taskId)}/candidate-groups`);
        if (Array.isArray(response)) {
            return response as string[];
        }
        if (Array.isArray(response.body)) {
            return response.body as string[];
        }
        return [];
    }

    async getTaskVariables(taskId: string): Promise<CloudVariableInstance[]> {
        const response = await this.get(`${this.basePath}/tasks/${encodeURIComponent(taskId)}/variables`);
        return this.unwrapList<CloudVariableInstance>(response, 'variables');
    }

    async getRootTasksByProcessInstance(processInstanceId: string): Promise<CloudTask[]> {
        const response = await this.get(
            `${this.basePath}/tasks?rootTasksOnly=true&processInstanceId=${encodeURIComponent(processInstanceId)}&sort=createdDate,desc&sort=id,desc`
        );
        return this.unwrapList<CloudTask>(response, 'tasks');
    }

    async searchTasks(searchRequest: TaskSearchRequest = {}, page?: SearchPageParams): Promise<CloudTask[]> {
        const response = await this.post(searchEndpoint(`${this.basePath}/tasks/search`, page), {
            data: toTaskSearchBody(searchRequest),
        });
        return this.unwrapList<CloudTask>(response, 'tasks');
    }

    async countTasks(searchRequest: TaskSearchRequest = {}, page?: SearchPageParams): Promise<number> {
        const response = await this.post(searchEndpoint(`${this.basePath}/tasks/count`, page), {
            data: toTaskSearchBody(searchRequest),
        });
        return parseCountResponse(response);
    }

    async deleteAllTasks(): Promise<CloudTask[]> {
        const response = await this.delete(`${this.basePath}/tasks`);
        return this.unwrapList<CloudTask>(response, 'tasks');
    }

    async postTasksListQuery(body: {
        standalone?: boolean;
        rootTasksOnly?: boolean;
        variableKeys?: string[];
    } = {}): Promise<CloudTask[]> {
        const response = await this.post(`${this.basePath}/tasks`, {
            data: {
                ...(body.standalone !== undefined ? { standalone: body.standalone } : {}),
                ...(body.rootTasksOnly !== undefined ? { rootTasksOnly: body.rootTasksOnly } : {}),
                ...(body.variableKeys ? { variableKeys: body.variableKeys } : {}),
            },
        });
        return this.unwrapList<CloudTask>(response, 'tasks');
    }
}
