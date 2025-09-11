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

import { APIRequestContext, APIResponse } from '@playwright/test';
import { CloudProcessInstance, ProcessQueryParams } from '../models/runtime-bundle.models';
import { CloudTask, TaskQueryParams } from '../models/task.models';

export class QueryService {
    private readonly basePath = '/query/v1';

    constructor(private readonly context: APIRequestContext) {}

    async getAllProcessInstances(): Promise<CloudProcessInstance[]> {
        const response: APIResponse = await this.context.get(`${this.basePath}/process-instances`);

        if (!response.ok()) {
            throw new Error(`Failed to get process instances: ${response.status()} ${response.statusText()}`);
        }

        const result = await response.json();
        return result.content || [];
    }

    async getProcessInstances(params?: ProcessQueryParams): Promise<CloudProcessInstance[]> {
        const searchParams = new URLSearchParams();

        if (params?.status) searchParams.append('status', params.status);
        if (params?.processDefinitionKey) searchParams.append('processDefinitionKey', params.processDefinitionKey);
        if (params?.businessKey) searchParams.append('businessKey', params.businessKey);
        if (params?.name) searchParams.append('name', params.name);

        const response: APIResponse = await this.context.get(
            `${this.basePath}/process-instances?${searchParams.toString()}`
        );

        if (!response.ok()) {
            throw new Error(`Failed to get process instances: ${response.status()} ${response.statusText()}`);
        }

        const result = await response.json();
        return result.content || [];
    }

    async getAllTasks(): Promise<CloudTask[]> {
        const response: APIResponse = await this.context.get(`${this.basePath}/tasks`);

        if (!response.ok()) {
            throw new Error(`Failed to get tasks: ${response.status()} ${response.statusText()}`);
        }

        const result = await response.json();
        return result.content || [];
    }

    async getTasks(params?: TaskQueryParams): Promise<CloudTask[]> {
        const searchParams = new URLSearchParams();

        if (params?.status) searchParams.append('status', params.status);
        if (params?.assignee) searchParams.append('assignee', params.assignee);
        if (params?.owner) searchParams.append('owner', params.owner);
        if (params?.processInstanceId) searchParams.append('processInstanceId', params.processInstanceId);
        if (params?.processDefinitionKey) searchParams.append('processDefinitionKey', params.processDefinitionKey);
        if (params?.name) searchParams.append('name', params.name);

        const response: APIResponse = await this.context.get(
            `${this.basePath}/tasks?${searchParams.toString()}`
        );

        if (!response.ok()) {
            throw new Error(`Failed to get tasks: ${response.status()} ${response.statusText()}`);
        }

        const result = await response.json();
        return result.content || [];
    }
}
