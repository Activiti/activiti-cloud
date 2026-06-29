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

import { CloudVariableInstance } from '../../../../models/process-variable.models';
import { CloudTask, TaskSearchRequest } from '../../../../models/task.models';
import { SearchPageParams } from '../../../../models/base-service.models';
import { BaseService } from '../../../base.service';
import { CustomAPIRequest } from '../../../../fixtures/context.models';
import { parseCountResponse, searchEndpoint, toTaskSearchBody } from '../../shared/search-utils';
import { QUERY_ADMIN_V1_BASE } from './process-instances.endpoint';

export class QueryAdminTasksEndpoint extends BaseService {
    constructor(context: CustomAPIRequest) {
        super(context);
    }

    async getAllTasksAdmin(): Promise<CloudTask[]> {
        const response = await this.get(`${QUERY_ADMIN_V1_BASE}/tasks`);
        return this.unwrapList<CloudTask>(response, 'tasks');
    }

    async getTasksAdminWithVariableKeys(variableKeys: string): Promise<CloudTask[]> {
        const response = await this.get(`${QUERY_ADMIN_V1_BASE}/tasks?variableKeys=${encodeURIComponent(variableKeys)}`);
        return this.unwrapList<CloudTask>(response, 'tasks');
    }

    async getTasksAdminFiltered(filters: {
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
        const response = await this.get(`${QUERY_ADMIN_V1_BASE}/tasks?${params.toString()}`);
        return this.unwrapList<CloudTask>(response, 'tasks');
    }

    async searchTasksAdmin(searchRequest: TaskSearchRequest = {}, page?: SearchPageParams): Promise<CloudTask[]> {
        const response = await this.post(searchEndpoint(`${QUERY_ADMIN_V1_BASE}/tasks/search`, page), {
            data: toTaskSearchBody(searchRequest),
        });
        return this.unwrapList<CloudTask>(response, 'tasks');
    }

    async countTasksAdmin(searchRequest: TaskSearchRequest = {}, page?: SearchPageParams): Promise<number> {
        const response = await this.post(searchEndpoint(`${QUERY_ADMIN_V1_BASE}/tasks/count`, page), {
            data: toTaskSearchBody(searchRequest),
        });
        return parseCountResponse(response);
    }

    async getTaskAdminById(taskId: string): Promise<CloudTask> {
        const response = await this.get(`${QUERY_ADMIN_V1_BASE}/tasks/${encodeURIComponent(taskId)}`);
        return this.unwrapEntity<CloudTask>(response);
    }

    async getTaskCandidateUsersAdmin(taskId: string): Promise<string[]> {
        const response = await this.get(`${QUERY_ADMIN_V1_BASE}/tasks/${encodeURIComponent(taskId)}/candidate-users`);
        if (Array.isArray(response)) {
            return response as string[];
        }
        if (Array.isArray(response.body)) {
            return response.body as string[];
        }
        return [];
    }

    async getTaskCandidateGroupsAdmin(taskId: string): Promise<string[]> {
        const response = await this.get(`${QUERY_ADMIN_V1_BASE}/tasks/${encodeURIComponent(taskId)}/candidate-groups`);
        if (Array.isArray(response)) {
            return response as string[];
        }
        if (Array.isArray(response.body)) {
            return response.body as string[];
        }
        return [];
    }

    async getTaskVariablesAdmin(taskId: string): Promise<CloudVariableInstance[]> {
        const response = await this.get(`${QUERY_ADMIN_V1_BASE}/tasks/${encodeURIComponent(taskId)}/variables`);
        return this.unwrapList<CloudVariableInstance>(response, 'variables');
    }

    async deleteAllTasksAdmin(): Promise<CloudTask[]> {
        const response = await this.delete(`${QUERY_ADMIN_V1_BASE}/tasks`);
        return this.unwrapList<CloudTask>(response, 'tasks');
    }
}
