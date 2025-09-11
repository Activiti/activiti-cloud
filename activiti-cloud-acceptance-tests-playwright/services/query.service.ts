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
import { CloudTask, TaskQueryParams } from '../models/task.models';
import { BaseService } from './base.service';
import { CustomAPIRequest } from '../context.models';

export class QueryService extends BaseService {
    private readonly basePath = '/query/v1';

    constructor(context: CustomAPIRequest) {
        super(context);
    }

    async getAllProcessInstances(): Promise<CloudProcessInstance[]> {
        const response = await this.get(`${this.basePath}/process-instances`);
        const result = response as any;
        return result.content || [];
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

        const result = response as any;
        return result.content || [];
    }

    async getAllTasks(): Promise<CloudTask[]> {
        const response = await this.get(`${this.basePath}/tasks`);
        const result = response as any;
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

        const response = await this.get(
            `${this.basePath}/tasks?${searchParams.toString()}`
        );

        const result = response as any;
        return result.content || [];
    }
}
