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
} from '../../../models/runtime-bundle.models';
import { CloudProcessDefinition } from '../../../models/process-definition.models';
import { CloudVariableInstance } from '../../../models/process-variable.models';
import { CloudTask, TaskSearchRequest } from '../../../models/task.models';
import { BaseService } from '../../base.service';
import { RuntimeBundleService } from '../../runtime-bundle.service';
import { CustomAPIRequest } from '../../../fixtures/context.models';
import { isDiagramShown } from '../../../helpers/diagram-utils';
import { HttpStatusCheck, SearchPageParams } from '../../../models/base-service.models';
import {
    QUERY_ADMIN_V1_BASE,
    QueryAdminApplicationsEndpoint,
    QueryAdminIntegrationContextsEndpoint,
    QueryAdminProcessDefinitionsEndpoint,
    QueryAdminProcessInstancesEndpoint,
    QueryAdminServiceTasksEndpoint,
    QueryAdminTasksEndpoint,
} from './endpoints/index';

export class QueryAdminService extends BaseService {
    private readonly processInstancesEndpoint: QueryAdminProcessInstancesEndpoint;
    private readonly processDefinitionsEndpoint: QueryAdminProcessDefinitionsEndpoint;
    private readonly serviceTasksEndpoint: QueryAdminServiceTasksEndpoint;
    private readonly tasksEndpoint: QueryAdminTasksEndpoint;
    private readonly applicationsEndpoint: QueryAdminApplicationsEndpoint;
    private readonly integrationContextsEndpoint: QueryAdminIntegrationContextsEndpoint;

    constructor(context: CustomAPIRequest) {
        super(context);
        this.processInstancesEndpoint = new QueryAdminProcessInstancesEndpoint(context);
        this.processDefinitionsEndpoint = new QueryAdminProcessDefinitionsEndpoint(context);
        this.serviceTasksEndpoint = new QueryAdminServiceTasksEndpoint(context);
        this.tasksEndpoint = new QueryAdminTasksEndpoint(context);
        this.applicationsEndpoint = new QueryAdminApplicationsEndpoint(context);
        this.integrationContextsEndpoint = new QueryAdminIntegrationContextsEndpoint(context);
    }

    async getAllProcessInstancesAdmin(): Promise<CloudProcessInstance[]> {
        return this.processInstancesEndpoint.getAllProcessInstancesAdmin();
    }

    async getProcessInstanceAdmin(processInstanceId: string): Promise<CloudProcessInstance> {
        return this.processInstancesEndpoint.getProcessInstanceAdmin(processInstanceId);
    }

    async getProcessInstanceAdminWhenSynced(processInstanceId: string): Promise<CloudProcessInstance | undefined> {
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
        return this.processInstancesEndpoint.getProcessInstancesAdminWithParams(params);
    }

    async getProcessInstancesAdminWithVariableKeys(variableKeys: string): Promise<CloudProcessInstance[]> {
        return this.processInstancesEndpoint.getProcessInstancesAdminWithVariableKeys(variableKeys);
    }

    async searchProcessInstancesAdmin(
        searchRequest: ProcessInstanceSearchRequest = {},
        page?: SearchPageParams
    ): Promise<CloudProcessInstance[]> {
        return this.processInstancesEndpoint.searchProcessInstancesAdmin(searchRequest, page);
    }

    async countProcessInstancesAdmin(searchRequest: ProcessInstanceSearchRequest = {}, page?: SearchPageParams): Promise<number> {
        return this.processInstancesEndpoint.countProcessInstancesAdmin(searchRequest, page);
    }

    async getProcessInstanceVariablesAdmin(processInstanceId: string): Promise<CloudVariableInstance[]> {
        return this.processInstancesEndpoint.getProcessInstanceVariablesAdmin(processInstanceId);
    }

    async getSubprocessesAdmin(processInstanceId: string): Promise<CloudProcessInstance[]> {
        return this.processInstancesEndpoint.getSubprocessesAdmin(processInstanceId);
    }

    async getSequenceFlowsAdmin(processInstanceId: string): Promise<Record<string, unknown>[]> {
        return this.processInstancesEndpoint.getSequenceFlowsAdmin(processInstanceId);
    }

    async getBpmnActivitiesAdmin(processInstanceId: string): Promise<Record<string, unknown>[]> {
        return this.processInstancesEndpoint.getBpmnActivitiesAdmin(processInstanceId);
    }

    async getLinkedProcessesAdmin(linkedProcessInstanceId: string): Promise<CloudProcessInstance[]> {
        return this.processInstancesEndpoint.getLinkedProcessesAdmin(linkedProcessInstanceId);
    }

    async getProcessInstanceAppVersionsAdmin(): Promise<string[]> {
        return this.processInstancesEndpoint.getProcessInstanceAppVersionsAdmin();
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
        return instances.filter((instance) => instance.businessKey === businessKey).map((instance) => instance.status);
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
        return this.processInstancesEndpoint.getProcessInstanceDiagram(processInstanceId);
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
        return this.processInstancesEndpoint.getProcessInstanceDiagramStatus(processInstanceId);
    }

    async getAllProcessDefinitionsAdmin(): Promise<CloudProcessDefinition[]> {
        return this.processDefinitionsEndpoint.getAllProcessDefinitionsAdmin();
    }

    async getProcessDefinitionByKey(processDefinitionKey: string): Promise<CloudProcessDefinition> {
        const definitions = await this.getAllProcessDefinitionsAdmin();
        return RuntimeBundleService.pickHighestVersionByKey(definitions, processDefinitionKey);
    }

    async getProcessModel(processDefinitionId: string): Promise<string> {
        return this.processInstancesEndpoint.getProcessModel(processDefinitionId);
    }

    async getServiceTasksForProcessInstance(processInstanceId: string): Promise<CloudServiceTask[]> {
        return this.serviceTasksEndpoint.getServiceTasksForProcessInstance(processInstanceId);
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
        return this.serviceTasksEndpoint.getServiceTasksByStatusForProcessInstance(processInstanceId, status);
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
        return this.serviceTasksEndpoint.getServiceTaskById(serviceTaskId);
    }

    async getServiceTaskIntegrationContext(serviceTaskId: string): Promise<CloudIntegrationContext> {
        return this.serviceTasksEndpoint.getServiceTaskIntegrationContext(serviceTaskId);
    }

    async findServiceTaskIntegrationContext(serviceTaskId: string): Promise<CloudIntegrationContext | undefined> {
        try {
            return await this.getServiceTaskIntegrationContext(serviceTaskId);
        } catch {
            return undefined;
        }
    }

    async getServiceTaskIntegrationContexts(serviceTaskId: string): Promise<CloudIntegrationContext[]> {
        return this.serviceTasksEndpoint.getServiceTaskIntegrationContexts(serviceTaskId);
    }

    async findServiceTaskIntegrationContexts(serviceTaskId: string): Promise<CloudIntegrationContext[] | undefined> {
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
        return this.serviceTasksEndpoint.getServiceTasksByQuery(params);
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
        return this.tasksEndpoint.getAllTasksAdmin();
    }

    async getTasksAdminWithVariableKeys(variableKeys: string): Promise<CloudTask[]> {
        return this.tasksEndpoint.getTasksAdminWithVariableKeys(variableKeys);
    }

    async getTasksAdminFiltered(filters: {
        processInstanceId?: string;
        status?: string;
        id?: string;
        skipCount?: number;
        maxItems?: number;
        sort?: string[];
    }): Promise<CloudTask[]> {
        return this.tasksEndpoint.getTasksAdminFiltered(filters);
    }

    async getProcessInstancesAdminFiltered(filters: {
        status?: ProcessInstanceStatus;
        skipCount?: number;
        maxItems?: number;
    }): Promise<CloudProcessInstance[]> {
        return this.getProcessInstancesAdminWithParams(filters);
    }

    async searchTasksAdmin(searchRequest: TaskSearchRequest = {}, page?: SearchPageParams): Promise<CloudTask[]> {
        return this.tasksEndpoint.searchTasksAdmin(searchRequest, page);
    }

    async countTasksAdmin(searchRequest: TaskSearchRequest = {}, page?: SearchPageParams): Promise<number> {
        return this.tasksEndpoint.countTasksAdmin(searchRequest, page);
    }

    async getTaskAdminById(taskId: string): Promise<CloudTask> {
        return this.tasksEndpoint.getTaskAdminById(taskId);
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
        return this.tasksEndpoint.getTaskCandidateUsersAdmin(taskId);
    }

    async getTaskCandidateGroupsAdmin(taskId: string): Promise<string[]> {
        return this.tasksEndpoint.getTaskCandidateGroupsAdmin(taskId);
    }

    async getTaskVariablesAdmin(taskId: string): Promise<CloudVariableInstance[]> {
        return this.tasksEndpoint.getTaskVariablesAdmin(taskId);
    }

    async waitForTaskVariablesAdmin(taskId: string, expected: Record<string, unknown>): Promise<CloudVariableInstance[]> {
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
        return this.applicationsEndpoint.getApplicationsAdmin();
    }

    async getIntegrationContextAdmin(integrationContextId: string): Promise<CloudIntegrationContext> {
        return this.integrationContextsEndpoint.getIntegrationContextAdmin(integrationContextId);
    }

    async deleteAllProcessInstancesAdmin(): Promise<CloudProcessInstance[]> {
        return this.processInstancesEndpoint.deleteAllProcessInstancesAdmin();
    }

    async deleteAllTasksAdmin(): Promise<CloudTask[]> {
        return this.tasksEndpoint.deleteAllTasksAdmin();
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

    buildUnauthenticatedGetStatusChecks(fakeResourceId: string): readonly HttpStatusCheck<QueryAdminService>[] {
        return [
            BaseService.getStatusCheck<QueryAdminService>('tasks list', `${QUERY_ADMIN_V1_BASE}/tasks`),
            BaseService.getStatusCheck<QueryAdminService>('process instances list', `${QUERY_ADMIN_V1_BASE}/process-instances`),
            BaseService.getStatusCheck<QueryAdminService>(
                'process instances with variable keys',
                `${QUERY_ADMIN_V1_BASE}/process-instances?variableKeys=start1`
            ),
            BaseService.getStatusCheck<QueryAdminService>(
                'process instance by id',
                `${QUERY_ADMIN_V1_BASE}/process-instances/${fakeResourceId}`
            ),
            BaseService.getStatusCheck<QueryAdminService>('task by id', `${QUERY_ADMIN_V1_BASE}/tasks/${encodeURIComponent(fakeResourceId)}`),
            BaseService.getStatusCheck<QueryAdminService>(
                'task candidate groups',
                `${QUERY_ADMIN_V1_BASE}/tasks/${encodeURIComponent(fakeResourceId)}/candidate-groups`
            ),
            BaseService.getStatusCheck<QueryAdminService>(
                'task candidate users',
                `${QUERY_ADMIN_V1_BASE}/tasks/${encodeURIComponent(fakeResourceId)}/candidate-users`
            ),
            BaseService.getStatusCheck<QueryAdminService>(
                'task variables',
                `${QUERY_ADMIN_V1_BASE}/tasks/${encodeURIComponent(fakeResourceId)}/variables`
            ),
            BaseService.getStatusCheck<QueryAdminService>('applications list', `${QUERY_ADMIN_V1_BASE}/applications`),
            BaseService.getStatusCheck<QueryAdminService>(
                'integration context by id',
                `${QUERY_ADMIN_V1_BASE}/integration-contexts/${encodeURIComponent(fakeResourceId)}`
            ),
        ];
    }

    buildUnauthenticatedPostStatusChecks(fakeResourceId: string): readonly HttpStatusCheck<QueryAdminService>[] {
        const taskSearchBody = { onlyStandalone: false, onlyRoot: false, id: [fakeResourceId] };
        return [
            BaseService.postStatusCheck<QueryAdminService>('process instance search', `${QUERY_ADMIN_V1_BASE}/process-instances/search`, {
                id: [fakeResourceId],
            }),
            BaseService.postStatusCheck<QueryAdminService>('process instance count', `${QUERY_ADMIN_V1_BASE}/process-instances/count`, {
                id: [fakeResourceId],
            }),
            BaseService.postStatusCheck<QueryAdminService>('task search', `${QUERY_ADMIN_V1_BASE}/tasks/search`, taskSearchBody),
            BaseService.postStatusCheck<QueryAdminService>('task count', `${QUERY_ADMIN_V1_BASE}/tasks/count`, taskSearchBody),
        ];
    }

    buildNotFoundGetStatusChecks(fakeResourceId: string): readonly HttpStatusCheck<QueryAdminService>[] {
        return [
            BaseService.getStatusCheck<QueryAdminService>(
                'process instance by id',
                `${QUERY_ADMIN_V1_BASE}/process-instances/${fakeResourceId}`
            ),
            BaseService.getStatusCheck<QueryAdminService>('task by id', `${QUERY_ADMIN_V1_BASE}/tasks/${encodeURIComponent(fakeResourceId)}`),
            BaseService.getStatusCheck<QueryAdminService>(
                'task candidate groups',
                `${QUERY_ADMIN_V1_BASE}/tasks/${encodeURIComponent(fakeResourceId)}/candidate-groups`
            ),
            BaseService.getStatusCheck<QueryAdminService>(
                'task candidate users',
                `${QUERY_ADMIN_V1_BASE}/tasks/${encodeURIComponent(fakeResourceId)}/candidate-users`
            ),
            BaseService.getStatusCheck<QueryAdminService>(
                'integration context by id',
                `${QUERY_ADMIN_V1_BASE}/integration-contexts/${encodeURIComponent(fakeResourceId)}`
            ),
        ];
    }

    buildBadRequestPostStatusChecks(fakeResourceId: string): readonly HttpStatusCheck<QueryAdminService>[] {
        return [
            BaseService.postStatusCheck<QueryAdminService>('task search', `${QUERY_ADMIN_V1_BASE}/tasks/search`, { id: [fakeResourceId] }),
            BaseService.postStatusCheck<QueryAdminService>('task count', `${QUERY_ADMIN_V1_BASE}/tasks/count`, { id: [fakeResourceId] }),
            BaseService.postStatusCheck<QueryAdminService>('process instance search', `${QUERY_ADMIN_V1_BASE}/process-instances/search`, {
                id: 'not-an-array',
            }),
        ];
    }

    buildForbiddenGetStatusChecks(
        taskId: string,
        processInstanceId: string
    ): readonly HttpStatusCheck<QueryAdminService>[] {
        return [
            BaseService.getStatusCheck<QueryAdminService>('applications list', `${QUERY_ADMIN_V1_BASE}/applications`),
            BaseService.getStatusCheck<QueryAdminService>('tasks list', `${QUERY_ADMIN_V1_BASE}/tasks`),
            BaseService.getStatusCheck<QueryAdminService>('process instances list', `${QUERY_ADMIN_V1_BASE}/process-instances`),
            BaseService.getStatusCheck<QueryAdminService>(
                'process instances with variable keys',
                `${QUERY_ADMIN_V1_BASE}/process-instances?variableKeys=start1`
            ),
            BaseService.getStatusCheck<QueryAdminService>('task by id', `${QUERY_ADMIN_V1_BASE}/tasks/${encodeURIComponent(taskId)}`),
            BaseService.getStatusCheck<QueryAdminService>(
                'task variables',
                `${QUERY_ADMIN_V1_BASE}/tasks/${encodeURIComponent(taskId)}/variables`
            ),
            BaseService.getStatusCheck<QueryAdminService>(
                'process instance variables',
                `${QUERY_ADMIN_V1_BASE}/process-instances/${processInstanceId}/variables`
            ),
            BaseService.getStatusCheck<QueryAdminService>(
                'process instance sequence flows',
                `${QUERY_ADMIN_V1_BASE}/process-instances/${processInstanceId}/sequence-flows`
            ),
            BaseService.getStatusCheck<QueryAdminService>(
                'process instance subprocesses',
                `${QUERY_ADMIN_V1_BASE}/process-instances/${processInstanceId}/subprocesses`
            ),
        ];
    }

    buildForbiddenPostStatusChecks(taskId: string, fakeResourceId: string): readonly HttpStatusCheck<QueryAdminService>[] {
        const taskSearchBody = { onlyStandalone: false, onlyRoot: false, id: [taskId] };
        return [
            BaseService.postStatusCheck<QueryAdminService>('task search', `${QUERY_ADMIN_V1_BASE}/tasks/search`, taskSearchBody),
            BaseService.postStatusCheck<QueryAdminService>('task count', `${QUERY_ADMIN_V1_BASE}/tasks/count`, taskSearchBody),
            BaseService.postStatusCheck<QueryAdminService>('process instance search', `${QUERY_ADMIN_V1_BASE}/process-instances/search`, {
                id: [fakeResourceId],
            }),
            BaseService.postStatusCheck<QueryAdminService>('process instance count', `${QUERY_ADMIN_V1_BASE}/process-instances/count`, {
                id: [fakeResourceId],
            }),
        ];
    }
}
