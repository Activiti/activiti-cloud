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
    ProcessInstanceSearchRequest,
    ProcessInstanceStatus,
    ProcessQueryParams,
    ServiceTaskStatus,
} from '../models/runtime-bundle.models';
import { CloudProcessDefinition } from '../models/process-definition.models';
import { CloudVariableInstance } from '../models/process-variable.models';
import { CloudTask, TaskSearchRequest } from '../models/task.models';
import { BaseService } from './base.service';
import { RuntimeBundleService } from './runtime-bundle.service';
import { CustomAPIRequest } from '../fixtures/context.models';
import { isDiagramShown } from '../helpers/diagram-utils';
import { SearchPageParams, HttpStatusCheck } from '../models/base-service.models';

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

    async getProcessInstancesAdminWithVariableKeys(variableKeys: string): Promise<CloudProcessInstance[]> {
        const response = await this.get(
            `${this.basePath}/process-instances?variableKeys=${encodeURIComponent(variableKeys)}`
        );
        return this.unwrapList<CloudProcessInstance>(response, 'processInstances');
    }

    async searchProcessInstancesAdmin(
        searchRequest: ProcessInstanceSearchRequest = {},
        page?: SearchPageParams
    ): Promise<CloudProcessInstance[]> {
        const response = await this.post(
            QueryAdminService.searchEndpoint(`${this.basePath}/process-instances/search`, page),
            { data: searchRequest }
        );
        return this.unwrapList<CloudProcessInstance>(response, 'processInstances');
    }

    async countProcessInstancesAdmin(
        searchRequest: ProcessInstanceSearchRequest = {},
        page?: SearchPageParams
    ): Promise<number> {
        const response = await this.post(
            QueryAdminService.searchEndpoint(`${this.basePath}/process-instances/count`, page),
            { data: searchRequest }
        );
        return QueryAdminService.parseCountResponse(response);
    }

    async getProcessInstanceVariablesAdmin(processInstanceId: string): Promise<CloudVariableInstance[]> {
        const response = await this.get(`${this.basePath}/process-instances/${processInstanceId}/variables`);
        return this.unwrapList<CloudVariableInstance>(response, 'variables');
    }

    async getSubprocessesAdmin(processInstanceId: string): Promise<CloudProcessInstance[]> {
        const response = await this.get(`${this.basePath}/process-instances/${processInstanceId}/subprocesses`);
        return this.unwrapList<CloudProcessInstance>(response, 'processInstances');
    }

    async getSequenceFlowsAdmin(processInstanceId: string): Promise<Record<string, unknown>[]> {
        const response = await this.get(`${this.basePath}/process-instances/${processInstanceId}/sequence-flows`);
        return this.unwrapList<Record<string, unknown>>(response, 'list');
    }

    async getBpmnActivitiesAdmin(processInstanceId: string): Promise<Record<string, unknown>[]> {
        const response = await this.get(`${this.basePath}/process-instances/${processInstanceId}/bpmn-activities`);
        return this.unwrapList<Record<string, unknown>>(response, 'list');
    }

    async getLinkedProcessesAdmin(linkedProcessInstanceId: string): Promise<CloudProcessInstance[]> {
        const response = await this.get(
            `${this.basePath}/process-instances/${linkedProcessInstanceId}/linkedprocesses`
        );
        return this.unwrapList<CloudProcessInstance>(response, 'processInstances');
    }

    async getProcessInstanceAppVersionsAdmin(): Promise<string[]> {
        const response = await this.get(`${this.basePath}/process-instances/appVersions`);
        if (Array.isArray(response)) {
            return response as string[];
        }
        return [];
    }

    async waitForLinkedProcessAdmin(
        mainProcessInstanceId: string,
        linkedProcessInstanceId: string
    ): Promise<CloudProcessInstance[]> {
        return QueryAdminService.waitFor(
            () => this.getLinkedProcessesAdmin(mainProcessInstanceId),
            (instances) => instances.some((instance) => instance.id === linkedProcessInstanceId),
            'querySync',
            `linked process ${linkedProcessInstanceId} under ${mainProcessInstanceId}`
        );
    }

    private static parseCountResponse(response: { body?: string }): number {
        const raw = response.body;
        if (raw === undefined || raw === '') {
            throw new Error('Unexpected empty count response');
        }
        const count = Number(raw);
        if (Number.isNaN(count)) {
            throw new Error(`Unexpected count response: ${raw}`);
        }
        return count;
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

    async waitForProcessInstanceAdminSynced(processInstanceId: string): Promise<CloudProcessInstance> {
        const instance = await QueryAdminService.waitFor(
            () => this.getProcessInstanceAdminWhenSynced(processInstanceId),
            (value) => value !== undefined,
            'querySync',
            `admin process ${processInstanceId} to be synced to query`
        );
        return instance!;
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

    async getTasksAdminWithVariableKeys(variableKeys: string): Promise<CloudTask[]> {
        const response = await this.get(
            `${this.basePath}/tasks?variableKeys=${encodeURIComponent(variableKeys)}`
        );
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
        const response = await this.get(`${this.basePath}/tasks?${params.toString()}`);
        return this.unwrapList<CloudTask>(response, 'tasks');
    }

    async getProcessInstancesAdminFiltered(filters: {
        status?: ProcessInstanceStatus;
        skipCount?: number;
        maxItems?: number;
    }): Promise<CloudProcessInstance[]> {
        const params = new URLSearchParams();
        if (filters.status) {
            params.set('status', filters.status);
        }
        if (filters.skipCount !== undefined) {
            params.set('skipCount', String(filters.skipCount));
        }
        if (filters.maxItems !== undefined) {
            params.set('maxItems', String(filters.maxItems));
        }
        const response = await this.get(`${this.basePath}/process-instances?${params.toString()}`);
        return this.unwrapList<CloudProcessInstance>(response, 'processInstances');
    }

    async searchTasksAdmin(searchRequest: TaskSearchRequest = {}, page?: SearchPageParams): Promise<CloudTask[]> {
        const response = await this.post(
            QueryAdminService.searchEndpoint(`${this.basePath}/tasks/search`, page),
            {
                data: QueryAdminService.toTaskSearchBody(searchRequest),
            }
        );
        return this.unwrapList<CloudTask>(response, 'tasks');
    }

    async countTasksAdmin(searchRequest: TaskSearchRequest = {}, page?: SearchPageParams): Promise<number> {
        const response = await this.post(
            QueryAdminService.searchEndpoint(`${this.basePath}/tasks/count`, page),
            {
                data: QueryAdminService.toTaskSearchBody(searchRequest),
            }
        );
        return QueryAdminService.parseCountResponse(response);
    }

    async getTaskAdminById(taskId: string): Promise<CloudTask> {
        const response = await this.get(`${this.basePath}/tasks/${encodeURIComponent(taskId)}`);
        return this.unwrapEntity<CloudTask>(response);
    }

    async getTaskAdminByIdWhenSynced(taskId: string): Promise<CloudTask | undefined> {
        try {
            return await this.getTaskAdminById(taskId);
        } catch {
            return undefined;
        }
    }

    async waitForTaskAdminSynced(taskId: string): Promise<CloudTask> {
        const task = await QueryAdminService.waitFor(
            () => this.getTaskAdminByIdWhenSynced(taskId),
            (value) => value !== undefined,
            'querySync',
            `admin task ${taskId} to be synced to query`
        );
        return task!;
    }

    async getTaskCandidateUsersAdmin(taskId: string): Promise<string[]> {
        const response = await this.get(`${this.basePath}/tasks/${encodeURIComponent(taskId)}/candidate-users`);
        if (Array.isArray(response)) {
            return response as string[];
        }
        if (Array.isArray(response.body)) {
            return response.body as string[];
        }
        return [];
    }

    async getTaskCandidateGroupsAdmin(taskId: string): Promise<string[]> {
        const response = await this.get(`${this.basePath}/tasks/${encodeURIComponent(taskId)}/candidate-groups`);
        if (Array.isArray(response)) {
            return response as string[];
        }
        if (Array.isArray(response.body)) {
            return response.body as string[];
        }
        return [];
    }

    async getTaskVariablesAdmin(taskId: string): Promise<CloudVariableInstance[]> {
        const response = await this.get(`${this.basePath}/tasks/${encodeURIComponent(taskId)}/variables`);
        return this.unwrapList<CloudVariableInstance>(response, 'variables');
    }

    async waitForTaskVariablesAdmin(
        taskId: string,
        expected: Record<string, unknown>
    ): Promise<CloudVariableInstance[]> {
        return QueryAdminService.waitFor(
            () => this.getTaskVariablesAdmin(taskId),
            (variables) => {
                const map = Object.fromEntries(variables.map((variable) => [variable.name, variable.value]));
                return Object.keys(expected).every((name) => map[name] === expected[name]);
            },
            'querySync',
            `admin task ${taskId} variables to match ${JSON.stringify(expected)}`
        );
    }

    async getApplicationsAdmin(): Promise<{ name: string; [key: string]: unknown }[]> {
        const response = await this.get(`${this.basePath}/applications`);
        return this.unwrapList<{ name: string; [key: string]: unknown }>(response, 'applications');
    }

    async getIntegrationContextAdmin(integrationContextId: string): Promise<CloudIntegrationContext> {
        const response = await this.get(
            `${this.basePath}/integration-contexts/${encodeURIComponent(integrationContextId)}`
        );
        return this.unwrapEntity<CloudIntegrationContext>(response);
    }

    private static toTaskSearchBody(searchRequest: TaskSearchRequest): TaskSearchRequest {
        return {
            onlyStandalone: false,
            onlyRoot: false,
            ...searchRequest,
        };
    }

    private static searchEndpoint(path: string, page?: SearchPageParams): string {
        if (!page) {
            return path;
        }
        const params = new URLSearchParams();
        if (page.skipCount !== undefined) {
            params.set('skipCount', String(page.skipCount));
        }
        if (page.maxItems !== undefined) {
            params.set('maxItems', String(page.maxItems));
        }
        for (const sort of page.sort ?? []) {
            params.append('sort', sort);
        }
        const query = params.toString();
        return query ? `${path}?${query}` : path;
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

    async getApplicationsAdminHttpStatus(): Promise<number> {
        return this.getHttpStatus(`${this.basePath}/applications`);
    }

    async getAllTasksAdminHttpStatus(): Promise<number> {
        return this.getHttpStatus(`${this.basePath}/tasks`);
    }

    async getAllProcessInstancesAdminHttpStatus(): Promise<number> {
        return this.getHttpStatus(`${this.basePath}/process-instances`);
    }

    async getProcessInstancesAdminWithVariableKeysHttpStatus(variableKeys: string): Promise<number> {
        return this.getHttpStatus(
            `${this.basePath}/process-instances?variableKeys=${encodeURIComponent(variableKeys)}`
        );
    }

    async getProcessInstanceAdminHttpStatus(processInstanceId: string): Promise<number> {
        return this.getHttpStatus(`${this.basePath}/process-instances/${processInstanceId}`);
    }

    async getTaskAdminHttpStatus(taskId: string): Promise<number> {
        return this.getHttpStatus(`${this.basePath}/tasks/${encodeURIComponent(taskId)}`);
    }

    async getTaskCandidateGroupsAdminHttpStatus(taskId: string): Promise<number> {
        return this.getHttpStatus(`${this.basePath}/tasks/${encodeURIComponent(taskId)}/candidate-groups`);
    }

    async getTaskCandidateUsersAdminHttpStatus(taskId: string): Promise<number> {
        return this.getHttpStatus(`${this.basePath}/tasks/${encodeURIComponent(taskId)}/candidate-users`);
    }

    async getTaskVariablesAdminHttpStatus(taskId: string): Promise<number> {
        return this.getHttpStatus(`${this.basePath}/tasks/${encodeURIComponent(taskId)}/variables`);
    }

    async getProcessInstanceVariablesAdminHttpStatus(processInstanceId: string): Promise<number> {
        return this.getHttpStatus(`${this.basePath}/process-instances/${processInstanceId}/variables`);
    }

    async getSequenceFlowsAdminHttpStatus(processInstanceId: string): Promise<number> {
        return this.getHttpStatus(`${this.basePath}/process-instances/${processInstanceId}/sequence-flows`);
    }

    async getSubprocessesAdminHttpStatus(processInstanceId: string): Promise<number> {
        return this.getHttpStatus(`${this.basePath}/process-instances/${processInstanceId}/subprocesses`);
    }

    async getIntegrationContextAdminHttpStatus(integrationContextId: string): Promise<number> {
        return this.getHttpStatus(
            `${this.basePath}/integration-contexts/${encodeURIComponent(integrationContextId)}`
        );
    }

    async postTaskSearchAdminRawHttpStatus(body: unknown): Promise<number> {
        return this.postHttpStatus(`${this.basePath}/tasks/search`, { data: body });
    }

    async postTaskCountAdminRawHttpStatus(body: unknown): Promise<number> {
        return this.postHttpStatus(`${this.basePath}/tasks/count`, { data: body });
    }

    async postProcessInstanceSearchAdminRawHttpStatus(body: unknown): Promise<number> {
        return this.postHttpStatus(`${this.basePath}/process-instances/search`, { data: body });
    }

    async postProcessInstanceCountAdminRawHttpStatus(body: unknown): Promise<number> {
        return this.postHttpStatus(`${this.basePath}/process-instances/count`, { data: body });
    }

    async postTaskSearchAdminMissingBooleansHttpStatus(resourceId: string): Promise<number> {
        return this.postTaskSearchAdminRawHttpStatus({ id: [resourceId] });
    }

    async postTaskCountAdminMissingBooleansHttpStatus(resourceId: string): Promise<number> {
        return this.postTaskCountAdminRawHttpStatus({ id: [resourceId] });
    }

    async postProcessInstanceSearchAdminInvalidBodyHttpStatus(): Promise<number> {
        return this.postProcessInstanceSearchAdminRawHttpStatus({ id: 'not-an-array' });
    }

    async postTaskSearchAdminByIdHttpStatus(taskId: string): Promise<number> {
        return this.postTaskSearchAdminRawHttpStatus({
            onlyStandalone: false,
            onlyRoot: false,
            id: [taskId],
        });
    }

    async postTaskCountAdminByIdHttpStatus(taskId: string): Promise<number> {
        return this.postTaskCountAdminRawHttpStatus({
            onlyStandalone: false,
            onlyRoot: false,
            id: [taskId],
        });
    }
}

export type QueryAdminHttpStatusCheck = HttpStatusCheck<QueryAdminService>;

export function buildQueryAdminUnauthenticatedGetStatusChecks(
    fakeResourceId: string
): readonly QueryAdminHttpStatusCheck[] {
    return [
        { label: 'tasks list', run: (service) => service.getAllTasksAdminHttpStatus() },
        { label: 'process instances list', run: (service) => service.getAllProcessInstancesAdminHttpStatus() },
        {
            label: 'process instances with variable keys',
            run: (service) => service.getProcessInstancesAdminWithVariableKeysHttpStatus('start1'),
        },
        {
            label: 'process instance by id',
            run: (service) => service.getProcessInstanceAdminHttpStatus(fakeResourceId),
        },
        { label: 'task by id', run: (service) => service.getTaskAdminHttpStatus(fakeResourceId) },
        {
            label: 'task candidate groups',
            run: (service) => service.getTaskCandidateGroupsAdminHttpStatus(fakeResourceId),
        },
        {
            label: 'task candidate users',
            run: (service) => service.getTaskCandidateUsersAdminHttpStatus(fakeResourceId),
        },
        {
            label: 'task variables',
            run: (service) => service.getTaskVariablesAdminHttpStatus(fakeResourceId),
        },
        { label: 'applications list', run: (service) => service.getApplicationsAdminHttpStatus() },
        {
            label: 'integration context by id',
            run: (service) => service.getIntegrationContextAdminHttpStatus(fakeResourceId),
        },
    ];
}

export function buildQueryAdminUnauthenticatedPostStatusChecks(
    fakeResourceId: string
): readonly QueryAdminHttpStatusCheck[] {
    const taskSearchBody = { onlyStandalone: false, onlyRoot: false, id: [fakeResourceId] };
    return [
        {
            label: 'process instance search',
            run: (service) => service.postProcessInstanceSearchAdminRawHttpStatus({ id: [fakeResourceId] }),
        },
        {
            label: 'process instance count',
            run: (service) => service.postProcessInstanceCountAdminRawHttpStatus({ id: [fakeResourceId] }),
        },
        { label: 'task search', run: (service) => service.postTaskSearchAdminRawHttpStatus(taskSearchBody) },
        { label: 'task count', run: (service) => service.postTaskCountAdminRawHttpStatus(taskSearchBody) },
    ];
}

export function buildQueryAdminNotFoundGetStatusChecks(
    fakeResourceId: string
): readonly QueryAdminHttpStatusCheck[] {
    return [
        {
            label: 'process instance by id',
            run: (service) => service.getProcessInstanceAdminHttpStatus(fakeResourceId),
        },
        { label: 'task by id', run: (service) => service.getTaskAdminHttpStatus(fakeResourceId) },
        {
            label: 'task candidate groups',
            run: (service) => service.getTaskCandidateGroupsAdminHttpStatus(fakeResourceId),
        },
        {
            label: 'task candidate users',
            run: (service) => service.getTaskCandidateUsersAdminHttpStatus(fakeResourceId),
        },
        {
            label: 'integration context by id',
            run: (service) => service.getIntegrationContextAdminHttpStatus(fakeResourceId),
        },
    ];
}

export function buildQueryAdminBadRequestPostStatusChecks(
    fakeResourceId: string
): readonly QueryAdminHttpStatusCheck[] {
    return [
        {
            label: 'task search',
            run: (service) => service.postTaskSearchAdminMissingBooleansHttpStatus(fakeResourceId),
        },
        {
            label: 'task count',
            run: (service) => service.postTaskCountAdminMissingBooleansHttpStatus(fakeResourceId),
        },
        {
            label: 'process instance search',
            run: (service) => service.postProcessInstanceSearchAdminInvalidBodyHttpStatus(),
        },
    ];
}

export function buildQueryAdminForbiddenGetStatusChecks(
    taskId: string,
    processInstanceId: string
): readonly QueryAdminHttpStatusCheck[] {
    return [
        { label: 'applications list', run: (service) => service.getApplicationsAdminHttpStatus() },
        { label: 'tasks list', run: (service) => service.getAllTasksAdminHttpStatus() },
        { label: 'process instances list', run: (service) => service.getAllProcessInstancesAdminHttpStatus() },
        {
            label: 'process instances with variable keys',
            run: (service) => service.getProcessInstancesAdminWithVariableKeysHttpStatus('start1'),
        },
        { label: 'task by id', run: (service) => service.getTaskAdminHttpStatus(taskId) },
        { label: 'task variables', run: (service) => service.getTaskVariablesAdminHttpStatus(taskId) },
        {
            label: 'process instance variables',
            run: (service) => service.getProcessInstanceVariablesAdminHttpStatus(processInstanceId),
        },
        {
            label: 'process instance sequence flows',
            run: (service) => service.getSequenceFlowsAdminHttpStatus(processInstanceId),
        },
        {
            label: 'process instance subprocesses',
            run: (service) => service.getSubprocessesAdminHttpStatus(processInstanceId),
        },
    ];
}

export function buildQueryAdminForbiddenPostStatusChecks(
    taskId: string,
    fakeResourceId: string
): readonly QueryAdminHttpStatusCheck[] {
    return [
        { label: 'task search', run: (service) => service.postTaskSearchAdminByIdHttpStatus(taskId) },
        { label: 'task count', run: (service) => service.postTaskCountAdminByIdHttpStatus(taskId) },
        {
            label: 'process instance search',
            run: (service) => service.postProcessInstanceSearchAdminRawHttpStatus({ id: [fakeResourceId] }),
        },
        {
            label: 'process instance count',
            run: (service) => service.postProcessInstanceCountAdminRawHttpStatus({ id: [fakeResourceId] }),
        },
    ];
}
