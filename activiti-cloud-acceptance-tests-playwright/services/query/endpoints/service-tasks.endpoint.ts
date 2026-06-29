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

import { CloudIntegrationContext, CloudServiceTask, ServiceTaskStatus } from '../../../models/runtime-bundle.models';
import { CustomAPIRequest } from '../../../fixtures/context.models';
import { BaseService } from '../../base.service';
import { QUERY_ADMIN_V1_BASE } from './query-base-path';

export class QueryServiceTasksEndpoint extends BaseService {
    constructor(context: CustomAPIRequest) {
        super(context);
    }

    async getServiceTasksForProcessInstance(processInstanceId: string): Promise<CloudServiceTask[]> {
        const response = await this.get(`${QUERY_ADMIN_V1_BASE}/process-instances/${processInstanceId}/service-tasks`);
        return this.unwrapList<CloudServiceTask>(response, 'serviceTasks');
    }

    async getServiceTasksByStatusForProcessInstance(
        processInstanceId: string,
        status: ServiceTaskStatus | string
    ): Promise<CloudServiceTask[]> {
        const response = await this.get(
            `${QUERY_ADMIN_V1_BASE}/process-instances/${processInstanceId}/service-tasks?status=${encodeURIComponent(status)}`
        );
        return this.unwrapList<CloudServiceTask>(response, 'serviceTasks');
    }

    async getServiceTaskById(serviceTaskId: string): Promise<CloudServiceTask> {
        const response = await this.get(`${QUERY_ADMIN_V1_BASE}/service-tasks/${serviceTaskId}`);
        return this.unwrapEntity<CloudServiceTask>(response);
    }

    async getServiceTaskIntegrationContext(serviceTaskId: string): Promise<CloudIntegrationContext> {
        const response = await this.get(`${QUERY_ADMIN_V1_BASE}/service-tasks/${serviceTaskId}/integration-context`);
        return this.unwrapEntity<CloudIntegrationContext>(response);
    }

    async getServiceTaskIntegrationContexts(serviceTaskId: string): Promise<CloudIntegrationContext[]> {
        const response = await this.get(`${QUERY_ADMIN_V1_BASE}/service-tasks/${serviceTaskId}/integration-contexts`);
        return this.unwrapList<CloudIntegrationContext>(response, 'cloudIntegrationContexts');
    }

    async getServiceTasksByQuery(params: {
        processDefinitionKey?: string;
        status?: ServiceTaskStatus | string;
    }): Promise<CloudServiceTask[]> {
        const searchParams = new URLSearchParams();
        if (params.processDefinitionKey) searchParams.append('processDefinitionKey', params.processDefinitionKey);
        if (params.status) searchParams.append('status', String(params.status));
        const response = await this.get(`${QUERY_ADMIN_V1_BASE}/service-tasks?${searchParams.toString()}`);
        return this.unwrapList<CloudServiceTask>(response, 'serviceTasks');
    }
}
