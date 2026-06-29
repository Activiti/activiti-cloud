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

import { CloudProcessDefinition } from '../../../../models/process-definition.models';
import { CloudVariableInstance } from '../../../../models/process-variable.models';
import {
    CloudProcessInstance,
    ProcessInstanceSearchRequest,
    ProcessQueryParams,
} from '../../../../models/runtime-bundle.models';
import { SearchPageParams } from '../../../../models/base-service.models';
import { BaseService } from '../../../base.service';
import { CustomAPIRequest } from '../../../../fixtures/context.models';
import { parseCountResponse, searchEndpoint } from '../../shared/search-utils';

export const QUERY_ADMIN_V1_BASE = '/query/admin/v1';

export class QueryAdminProcessInstancesEndpoint extends BaseService {
    constructor(context: CustomAPIRequest) {
        super(context);
    }

    async getAllProcessInstancesAdmin(): Promise<CloudProcessInstance[]> {
        const response = await this.get(`${QUERY_ADMIN_V1_BASE}/process-instances`);
        return this.unwrapList<CloudProcessInstance>(response, 'processInstances');
    }

    async getProcessInstanceAdmin(processInstanceId: string): Promise<CloudProcessInstance> {
        const response = await this.get(`${QUERY_ADMIN_V1_BASE}/process-instances/${processInstanceId}`);
        return this.unwrapEntity<CloudProcessInstance>(response);
    }

    async getProcessInstancesAdminWithParams(params?: ProcessQueryParams): Promise<CloudProcessInstance[]> {
        const searchParams = new URLSearchParams();
        if (params?.status) searchParams.append('status', params.status);
        if (params?.processDefinitionKey) searchParams.append('processDefinitionKey', params.processDefinitionKey);
        if (params?.businessKey) searchParams.append('businessKey', params.businessKey);
        if (params?.name) searchParams.append('name', params.name);

        const response = await this.get(`${QUERY_ADMIN_V1_BASE}/process-instances?${searchParams.toString()}`);
        return this.unwrapList<CloudProcessInstance>(response, 'processInstances');
    }

    async getProcessInstancesAdminWithVariableKeys(variableKeys: string): Promise<CloudProcessInstance[]> {
        const response = await this.get(
            `${QUERY_ADMIN_V1_BASE}/process-instances?variableKeys=${encodeURIComponent(variableKeys)}`
        );
        return this.unwrapList<CloudProcessInstance>(response, 'processInstances');
    }

    async searchProcessInstancesAdmin(
        searchRequest: ProcessInstanceSearchRequest = {},
        page?: SearchPageParams
    ): Promise<CloudProcessInstance[]> {
        const response = await this.post(searchEndpoint(`${QUERY_ADMIN_V1_BASE}/process-instances/search`, page), {
            data: searchRequest,
        });
        return this.unwrapList<CloudProcessInstance>(response, 'processInstances');
    }

    async countProcessInstancesAdmin(searchRequest: ProcessInstanceSearchRequest = {}, page?: SearchPageParams): Promise<number> {
        const response = await this.post(searchEndpoint(`${QUERY_ADMIN_V1_BASE}/process-instances/count`, page), {
            data: searchRequest,
        });
        return parseCountResponse(response);
    }

    async getProcessInstanceVariablesAdmin(processInstanceId: string): Promise<CloudVariableInstance[]> {
        const response = await this.get(`${QUERY_ADMIN_V1_BASE}/process-instances/${processInstanceId}/variables`);
        return this.unwrapList<CloudVariableInstance>(response, 'variables');
    }

    async getSubprocessesAdmin(processInstanceId: string): Promise<CloudProcessInstance[]> {
        const response = await this.get(`${QUERY_ADMIN_V1_BASE}/process-instances/${processInstanceId}/subprocesses`);
        return this.unwrapList<CloudProcessInstance>(response, 'processInstances');
    }

    async getSequenceFlowsAdmin(processInstanceId: string): Promise<Record<string, unknown>[]> {
        const response = await this.get(`${QUERY_ADMIN_V1_BASE}/process-instances/${processInstanceId}/sequence-flows`);
        return this.unwrapList<Record<string, unknown>>(response, 'list');
    }

    async getBpmnActivitiesAdmin(processInstanceId: string): Promise<Record<string, unknown>[]> {
        const response = await this.get(`${QUERY_ADMIN_V1_BASE}/process-instances/${processInstanceId}/bpmn-activities`);
        return this.unwrapList<Record<string, unknown>>(response, 'list');
    }

    async getLinkedProcessesAdmin(linkedProcessInstanceId: string): Promise<CloudProcessInstance[]> {
        const response = await this.get(`${QUERY_ADMIN_V1_BASE}/process-instances/${linkedProcessInstanceId}/linkedprocesses`);
        return this.unwrapList<CloudProcessInstance>(response, 'processInstances');
    }

    async getProcessInstanceAppVersionsAdmin(): Promise<string[]> {
        const response = await this.get(`${QUERY_ADMIN_V1_BASE}/process-instances/appVersions`);
        if (Array.isArray(response)) {
            return response as string[];
        }
        return [];
    }

    async getProcessInstanceDiagram(processInstanceId: string): Promise<string> {
        return this.getText(`${QUERY_ADMIN_V1_BASE}/process-instances/${processInstanceId}/diagram`, {
            Accept: 'image/svg+xml',
        });
    }

    async getProcessInstanceDiagramStatus(processInstanceId: string): Promise<number> {
        const response = await this.get(`${QUERY_ADMIN_V1_BASE}/process-instances/${processInstanceId}/diagram`, {
            headers: { Accept: 'image/svg+xml' },
        });
        return response.httpStatus ?? 200;
    }

    async getAllProcessDefinitionsAdmin(): Promise<CloudProcessDefinition[]> {
        const response = await this.get(`${QUERY_ADMIN_V1_BASE}/process-definitions`);
        return this.unwrapList<CloudProcessDefinition>(response, 'processDefinitions');
    }

    async getProcessModel(processDefinitionId: string): Promise<string> {
        return this.getText(`${QUERY_ADMIN_V1_BASE}/process-definitions/${processDefinitionId}/model`);
    }

    async deleteAllProcessInstancesAdmin(): Promise<CloudProcessInstance[]> {
        const response = await this.delete(`${QUERY_ADMIN_V1_BASE}/process-instances`);
        return this.unwrapList<CloudProcessInstance>(response, 'processInstances');
    }
}
