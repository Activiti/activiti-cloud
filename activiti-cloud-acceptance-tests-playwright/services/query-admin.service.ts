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
    ProcessInstanceStatus,
    ProcessQueryParams,
    ServiceTaskStatus,
} from '../models/runtime-bundle.models';
import { CloudProcessDefinition } from '../models/process-definition.models';
import { CloudTask } from '../models/task.models';
import { BaseService } from './base.service';
import { RuntimeBundleService } from './runtime-bundle.service';
import { CustomAPIRequest } from '../fixtures/context.models';
import { isDiagramShown } from '../helpers/diagram-utils';

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

    async getProcessInstanceAdminWhenSynced(
        processInstanceId: string
    ): Promise<CloudProcessInstance | undefined> {
        try {
            return await this.getProcessInstanceAdmin(processInstanceId);
        } catch (error) {
            if (QueryAdminService.isProcessInstanceNotFoundError(error)) {
                return undefined;
            }
            throw error;
        }
    }

    private static isProcessInstanceNotFoundError(error: unknown): boolean {
        const message = error instanceof Error ? error.message : String(error);
        return message.includes('Unable to find process instance');
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

    async waitForProcessInstancesAdminCountGreaterThan(
        params: ProcessQueryParams,
        minCount: number
    ): Promise<CloudProcessInstance[]> {
        return QueryAdminService.waitFor(
            () => this.getProcessInstancesAdminWithParams(params),
            (instances) => instances.length > minCount,
            'querySync',
            `admin process instances ${JSON.stringify(params)} count > ${minCount}`
        );
    }

    async getProcessInstanceStatusesByBusinessKey(
        processDefinitionKey: string,
        businessKey: string
    ): Promise<ProcessInstanceStatus[]> {
        const instances = await this.getProcessInstancesAdminWithParams({ processDefinitionKey });
        return instances
            .filter((instance) => instance.businessKey === businessKey)
            .map((instance) => instance.status);
    }

    async waitForProcessInstanceStatusByBusinessKey(
        processDefinitionKey: string,
        businessKey: string,
        expectedStatus: ProcessInstanceStatus
    ): Promise<ProcessInstanceStatus[]> {
        return QueryAdminService.waitFor(
            () => this.getProcessInstanceStatusesByBusinessKey(processDefinitionKey, businessKey),
            (statuses) => statuses.includes(expectedStatus),
            'querySync',
            `${processDefinitionKey} businessKey ${businessKey} to reach status ${expectedStatus}`
        );
    }

    async getProcessInstanceDiagram(processInstanceId: string): Promise<string> {
        return this.getText(`${this.basePath}/process-instances/${processInstanceId}/diagram`, {
            Accept: 'image/svg+xml',
        });
    }

    async waitForProcessInstanceDiagram(processInstanceId: string): Promise<string> {
        return QueryAdminService.waitFor(
            () => this.getProcessInstanceDiagram(processInstanceId),
            (diagram) => isDiagramShown(diagram),
            'querySync',
            `admin process instance diagram for ${processInstanceId}`
        );
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

    async getProcessDefinitionByKey(processDefinitionKey: string): Promise<CloudProcessDefinition> {
        const definitions = await this.getAllProcessDefinitionsAdmin();
        return RuntimeBundleService.pickHighestVersionByKey(definitions, processDefinitionKey);
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

    async waitForServiceTasksForProcessInstance(
        processInstanceId: string,
        predicate: (tasks: CloudServiceTask[]) => boolean,
        description: string
    ): Promise<CloudServiceTask[]> {
        return QueryAdminService.waitFor(
            () => this.getServiceTasksForProcessInstance(processInstanceId),
            predicate,
            'querySync',
            description
        );
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

    async waitForServiceTasksByStatusForProcessInstance(
        processInstanceId: string,
        status: ServiceTaskStatus | string,
        predicate: (tasks: CloudServiceTask[]) => boolean = (tasks) => tasks.length > 0
    ): Promise<CloudServiceTask[]> {
        return QueryAdminService.waitFor(
            () => this.getServiceTasksByStatusForProcessInstance(processInstanceId, status),
            predicate,
            'querySync',
            `service tasks status ${status} for process ${processInstanceId}`
        );
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

    async findServiceTaskIntegrationContext(
        serviceTaskId: string
    ): Promise<CloudIntegrationContext | undefined> {
        try {
            return await this.getServiceTaskIntegrationContext(serviceTaskId);
        } catch {
            return undefined;
        }
    }

    async getServiceTaskIntegrationContexts(serviceTaskId: string): Promise<CloudIntegrationContext[]> {
        const response = await this.get(
            `${this.basePath}/service-tasks/${serviceTaskId}/integration-contexts`
        );
        return this.unwrapList<CloudIntegrationContext>(response, 'cloudIntegrationContexts');
    }

    async findServiceTaskIntegrationContexts(
        serviceTaskId: string
    ): Promise<CloudIntegrationContext[] | undefined> {
        try {
            return await this.getServiceTaskIntegrationContexts(serviceTaskId);
        } catch {
            return undefined;
        }
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

    async waitForServiceTasksByQuery(
        params: { processDefinitionKey?: string; status?: ServiceTaskStatus | string },
        predicate: (tasks: CloudServiceTask[]) => boolean = (tasks) => tasks.length > 0
    ): Promise<CloudServiceTask[]> {
        return QueryAdminService.waitFor(
            () => this.getServiceTasksByQuery(params),
            predicate,
            'querySync',
            `service tasks query ${JSON.stringify(params)}`
        );
    }

    async waitForProcessInstanceAdminStatus(
        processInstanceId: string,
        expectedStatus: ProcessInstanceStatus
    ): Promise<CloudProcessInstance> {
        const instance = await QueryAdminService.waitFor(
            () => this.getProcessInstanceAdminWhenSynced(processInstanceId),
            (value) => value?.status === expectedStatus,
            'querySync',
            `admin process ${processInstanceId} to reach status ${expectedStatus}`
        );
        return instance!;
    }

    async waitForServiceTaskIntegrationContext(
        serviceTaskId: string,
        predicate: (context: CloudIntegrationContext) => boolean
    ): Promise<CloudIntegrationContext> {
        const context = await QueryAdminService.waitFor(
            () => this.findServiceTaskIntegrationContext(serviceTaskId),
            (value) => value !== undefined && predicate(value),
            'querySync',
            `service task ${serviceTaskId} integration context`
        );
        return context!;
    }

    async waitForServiceTaskIntegrationContexts(
        serviceTaskId: string,
        predicate: (contexts: CloudIntegrationContext[]) => boolean
    ): Promise<CloudIntegrationContext[]> {
        const contexts = await QueryAdminService.waitFor(
            () => this.findServiceTaskIntegrationContexts(serviceTaskId),
            (value) => value !== undefined && predicate(value),
            'querySync',
            `service task ${serviceTaskId} integration contexts`
        );
        return contexts!;
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

    async waitForAllProcessInstancesAdminCount(expectedCount: number): Promise<CloudProcessInstance[]> {
        return QueryAdminService.waitFor(
            () => this.getAllProcessInstancesAdmin(),
            (instances) => instances.length === expectedCount,
            'querySync',
            `admin process instances count to equal ${expectedCount}`
        );
    }

    async waitForAllProcessInstancesAdminCountGreaterThan(minCount: number): Promise<CloudProcessInstance[]> {
        return QueryAdminService.waitFor(
            () => this.getAllProcessInstancesAdmin(),
            (instances) => instances.length > minCount,
            'querySync',
            `admin process instances count > ${minCount}`
        );
    }

    async waitForAllTasksAdminCount(expectedCount: number): Promise<CloudTask[]> {
        return QueryAdminService.waitFor(
            () => this.getAllTasksAdmin(),
            (tasks) => tasks.length === expectedCount,
            'querySync',
            `admin tasks count to equal ${expectedCount}`
        );
    }

    async waitForAllTasksAdminCountGreaterThan(minCount: number): Promise<CloudTask[]> {
        return QueryAdminService.waitFor(
            () => this.getAllTasksAdmin(),
            (tasks) => tasks.length > minCount,
            'querySync',
            `admin tasks count > ${minCount}`
        );
    }
}
