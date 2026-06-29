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
    CloudProcessInstance,
    ProcessInstanceSearchRequest,
    ProcessInstanceStatus,
    ProcessQueryParams,
} from '../../models/runtime-bundle.models';
import { CloudProcessDefinition } from '../../models/process-definition.models';
import { CloudVariableInstance } from '../../models/process-variable.models';
import { CloudTask, TaskQueryParams, TaskSearchRequest, TaskStatus } from '../../models/task.models';
import { BaseService } from '../base.service';
import { RuntimeBundleService } from '../runtime-bundle.service';
import { CustomAPIRequest } from '../../fixtures/context.models';
import { isDiagramShown } from '../../helpers/diagram-utils';
import { PollProfile } from '../../config/runtime/timeouts';
import { HttpStatusCheck, SearchPageParams } from '../../models/base-service.models';
import {
    QUERY_V1_BASE,
    QueryApplicationsEndpoint,
    QueryOpenApiSpecEndpoint,
    QueryProcessDefinitionsEndpoint,
    QueryProcessInstancesEndpoint,
    QueryTasksEndpoint,
} from './endpoints/index';

export class QueryService extends BaseService {
    private readonly processInstancesEndpoint: QueryProcessInstancesEndpoint;
    private readonly processDefinitionsEndpoint: QueryProcessDefinitionsEndpoint;
    private readonly tasksEndpoint: QueryTasksEndpoint;
    private readonly applicationsEndpoint: QueryApplicationsEndpoint;
    private readonly openApiSpecEndpoint: QueryOpenApiSpecEndpoint;

    constructor(context: CustomAPIRequest) {
        super(context);
        this.processInstancesEndpoint = new QueryProcessInstancesEndpoint(context);
        this.processDefinitionsEndpoint = new QueryProcessDefinitionsEndpoint(context);
        this.tasksEndpoint = new QueryTasksEndpoint(context);
        this.applicationsEndpoint = new QueryApplicationsEndpoint(context);
        this.openApiSpecEndpoint = new QueryOpenApiSpecEndpoint(context);
    }

    async getAllProcessInstances(): Promise<CloudProcessInstance[]> {
        return this.processInstancesEndpoint.getAllProcessInstances();
    }

    async getProcessInstance(processInstanceId: string): Promise<CloudProcessInstance> {
        return this.processInstancesEndpoint.getProcessInstance(processInstanceId);
    }

    async getProcessInstanceWhenSynced(processInstanceId: string): Promise<CloudProcessInstance | undefined> {
        try {
            return await this.getProcessInstance(processInstanceId);
        } catch (error) {
            if (QueryService.isProcessInstanceNotFoundError(error)) {
                return undefined;
            }
            throw error;
        }
    }

    async getProcessInstanceWhenGone(processInstanceId: string): Promise<CloudProcessInstance | undefined> {
        try {
            return await this.getProcessInstance(processInstanceId);
        } catch (error) {
            if (QueryService.isProcessInstanceGoneError(error)) {
                return undefined;
            }
            throw error;
        }
    }

    private static isProcessInstanceNotFoundError(error: unknown): boolean {
        const message = error instanceof Error ? error.message : String(error);
        return message.includes('Unable to find process instance');
    }

    private static isProcessInstanceGoneError(error: unknown): boolean {
        const message = error instanceof Error ? error.message : String(error);
        return message.includes('Unable to find process instance') || message.includes('Operation not permitted');
    }

    async getProcessInstances(params?: ProcessQueryParams): Promise<CloudProcessInstance[]> {
        return this.processInstancesEndpoint.getProcessInstances(params);
    }

    async getAllTasks(): Promise<CloudTask[]> {
        return this.tasksEndpoint.getAllTasks();
    }

    async getTasks(params?: TaskQueryParams): Promise<CloudTask[]> {
        return this.tasksEndpoint.getTasks(params);
    }

    async getProcessDefinitions(): Promise<CloudProcessDefinition[]> {
        return this.processDefinitionsEndpoint.getProcessDefinitions();
    }

    async getProcessDefinitionByKey(processDefinitionKey: string): Promise<CloudProcessDefinition> {
        const definitions = await this.getProcessDefinitions();
        return RuntimeBundleService.pickHighestVersionByKey(definitions, processDefinitionKey);
    }

    async getProcessInstanceDiagram(processInstanceId: string): Promise<string> {
        return this.processInstancesEndpoint.getProcessInstanceDiagram(processInstanceId);
    }

    async waitForProcessInstanceDiagram(processInstanceId: string): Promise<string> {
        return QueryService.waitFor(
            () => this.getProcessInstanceDiagram(processInstanceId),
            (diagram) => isDiagramShown(diagram),
            'querySync',
            `process instance diagram for ${processInstanceId}`
        );
    }

    async getProcessModel(processDefinitionId: string): Promise<string> {
        return this.processInstancesEndpoint.getProcessModel(processDefinitionId);
    }

    async getSwaggerSpecification(group: string = 'Query'): Promise<string> {
        return this.openApiSpecEndpoint.getSwaggerSpecification(group);
    }

    async getProcessInstanceVariables(processInstanceId: string): Promise<CloudVariableInstance[]> {
        return this.processInstancesEndpoint.getProcessInstanceVariables(processInstanceId);
    }

    async waitForVariable(
        processInstanceId: string,
        variableName: string,
        predicate: (variable: CloudVariableInstance) => boolean = () => true
    ): Promise<CloudVariableInstance> {
        const variables = await QueryService.waitFor(
            () => this.getProcessInstanceVariables(processInstanceId),
            (list) => {
                const found = list.find((v) => v.name === variableName);
                return found !== undefined && predicate(found);
            },
            'querySync',
            `variable ${variableName} on process ${processInstanceId}`
        );
        return variables.find((v) => v.name === variableName)!;
    }

    async waitForProcessInstanceVariableValue(
        processInstanceId: string,
        variableName: string,
        expectedValue: string
    ): Promise<CloudVariableInstance> {
        return this.waitForVariable(
            processInstanceId,
            variableName,
            (v) => v.value !== undefined && String(v.value) === expectedValue
        );
    }

    async waitForProcessInstanceVariablesIncluding(
        processInstanceId: string,
        variableNames: readonly string[]
    ): Promise<CloudVariableInstance[]> {
        return QueryService.waitFor(
            () => this.getProcessInstanceVariables(processInstanceId),
            (variables) => {
                const names = new Set(variables.map((variable) => variable.name));
                return variableNames.every((name) => names.has(name));
            },
            'querySync',
            `query process ${processInstanceId} variables to include [${variableNames.join(',')}]`
        );
    }

    async waitForProcessInstanceStatus(
        processInstanceId: string,
        expectedStatus: ProcessInstanceStatus,
        profile: PollProfile = 'querySync'
    ): Promise<CloudProcessInstance> {
        const instance = await QueryService.waitFor(
            () => this.getProcessInstanceWhenSynced(processInstanceId),
            (value) => value?.status === expectedStatus,
            profile,
            `process ${processInstanceId} to reach status ${expectedStatus}`
        );
        return instance!;
    }

    async waitForProcessInstanceSynced(processInstanceId: string): Promise<CloudProcessInstance> {
        const instance = await QueryService.waitFor(
            () => this.getProcessInstanceWhenSynced(processInstanceId),
            (value) => value !== undefined,
            'querySync',
            `process ${processInstanceId} to be synced to query`
        );
        return instance!;
    }

    async getProcessInstancesByName(namePattern: string): Promise<CloudProcessInstance[]> {
        return this.getProcessInstances({ name: namePattern });
    }

    async waitForProcessInstanceName(processInstanceId: string, expectedName: string): Promise<CloudProcessInstance> {
        const instance = await QueryService.waitFor(
            () => this.getProcessInstanceWhenSynced(processInstanceId),
            (value) => value?.name === expectedName,
            'querySync',
            `process ${processInstanceId} to have name ${expectedName}`
        );
        return instance!;
    }

    async waitForProcessInstanceGone(processInstanceId: string): Promise<void> {
        await QueryService.waitFor(
            () => this.getProcessInstanceWhenGone(processInstanceId),
            (value) => value === undefined,
            'querySync',
            `process ${processInstanceId} to be gone from query`
        );
    }

    async waitForProcessInstanceByNamePrefix(namePrefix: string): Promise<CloudProcessInstance[]> {
        return QueryService.waitFor(
            () => this.getProcessInstancesByName(namePrefix),
            (instances) => instances.some((instance) => instance.name?.includes(namePrefix)),
            'querySync',
            `process instances with name containing ${namePrefix}`
        );
    }

    async getTaskById(taskId: string): Promise<CloudTask | undefined> {
        return this.tasksEndpoint.getTaskById(taskId);
    }

    async getTask(taskId: string): Promise<CloudTask> {
        return this.tasksEndpoint.getTask(taskId);
    }

    async getSubprocesses(processInstanceId: string): Promise<CloudProcessInstance[]> {
        return this.processInstancesEndpoint.getSubprocesses(processInstanceId);
    }

    async waitForSubprocesses(processInstanceId: string): Promise<CloudProcessInstance[]> {
        return QueryService.waitFor(
            () => this.getSubprocesses(processInstanceId),
            (subprocesses) => subprocesses.length > 0,
            'querySync',
            `query subprocesses of process ${processInstanceId}`
        );
    }

    async getTasksByProcessInstanceId(processInstanceId: string): Promise<CloudTask[]> {
        return this.processInstancesEndpoint.getTasksByProcessInstanceId(processInstanceId);
    }

    async waitForTaskByName(processInstanceId: string, taskName: string): Promise<CloudTask> {
        const tasks = await QueryService.waitFor(
            () => this.getTasksByProcessInstanceId(processInstanceId),
            (list) => list.some((t) => t.name === taskName),
            'querySync',
            `task named ${taskName} on process ${processInstanceId}`
        );
        return tasks.find((t) => t.name === taskName)!;
    }

    async findTasksByProcessInstanceId(processInstanceId: string): Promise<CloudTask[] | undefined> {
        try {
            return await this.getTasksByProcessInstanceId(processInstanceId);
        } catch {
            return undefined;
        }
    }

    async getStandaloneTasks(): Promise<CloudTask[]> {
        return this.tasksEndpoint.getStandaloneTasks();
    }

    async waitForStandaloneTask(taskId: string): Promise<CloudTask[]> {
        return QueryService.waitFor(
            () => this.getStandaloneTasks(),
            (tasks) => tasks.some((task) => task.id === taskId),
            'querySync',
            `standalone task ${taskId} to appear in query`
        );
    }

    async getTasksByNameAndDescription(namePrefix: string, descriptionPrefix: string): Promise<CloudTask[]> {
        return this.tasksEndpoint.getTasksByNameAndDescription(namePrefix, descriptionPrefix);
    }

    async waitForQueriedTaskByNameAndDescription(
        namePrefix: string,
        descriptionPrefix: string,
        taskId: string
    ): Promise<CloudTask[]> {
        return QueryService.waitFor(
            () => this.getTasksByNameAndDescription(namePrefix, descriptionPrefix),
            (tasks) => tasks.some((task) => task.id === taskId),
            'querySync',
            `task ${taskId} to appear by name=${namePrefix} description=${descriptionPrefix}`
        );
    }

    async getCandidateUsers(taskId: string): Promise<string[]> {
        return this.tasksEndpoint.getCandidateUsers(taskId);
    }

    async getCandidateGroups(taskId: string): Promise<string[]> {
        return this.tasksEndpoint.getCandidateGroups(taskId);
    }

    async getTaskVariables(taskId: string): Promise<CloudVariableInstance[]> {
        return this.tasksEndpoint.getTaskVariables(taskId);
    }

    async waitForTaskVariable(
        taskId: string,
        variableName: string,
        predicate: (variable: CloudVariableInstance) => boolean = () => true
    ): Promise<CloudVariableInstance> {
        const variables = await QueryService.waitFor(
            () => this.getTaskVariables(taskId),
            (list) => {
                const found = list.find((v) => v.name === variableName);
                return found !== undefined && predicate(found);
            },
            'querySync',
            `task variable ${variableName} on task ${taskId}`
        );
        return variables.find((v) => v.name === variableName)!;
    }

    async waitForTaskVariableValues(taskId: string, expected: Record<string, unknown>): Promise<CloudVariableInstance[]> {
        return QueryService.waitFor(
            () => this.getTaskVariables(taskId),
            (variables) => {
                const map = Object.fromEntries(variables.map((v) => [v.name, v.value]));
                return (
                    Object.keys(expected).every((name) => map[name] === expected[name]) &&
                    Object.keys(map).length === Object.keys(expected).length
                );
            },
            'querySync',
            `query task ${taskId} variables to match ${JSON.stringify(expected)}`
        );
    }

    async waitForTaskById(taskId: string, predicate: (task: CloudTask) => boolean): Promise<CloudTask> {
        const task = await QueryService.waitFor(
            () => this.getTaskById(taskId),
            (value) => value !== undefined && predicate(value),
            'querySync',
            `task ${taskId} matching predicate`
        );
        return task!;
    }

    async waitForTaskStatus(taskId: string, expectedStatus: TaskStatus): Promise<CloudTask> {
        return this.waitForTaskById(taskId, (task) => task.status === expectedStatus);
    }

    async waitForTaskName(taskId: string, expectedName: string): Promise<CloudTask> {
        return this.waitForTaskById(taskId, (task) => task.name === expectedName);
    }

    async waitForTaskCompleted(taskId: string): Promise<CloudTask> {
        return this.waitForTaskById(taskId, (task) => task.status === TaskStatus.COMPLETED && Boolean(task.completedDate));
    }

    async waitForTaskInProcessInstances(
        processInstanceIdsFetcher: () => Promise<string[]>,
        predicate: (task: CloudTask) => boolean,
        description: string
    ): Promise<CloudTask> {
        const tasks = await QueryService.waitFor(
            async () => {
                const ids = await processInstanceIdsFetcher();
                const allTasks: CloudTask[] = [];
                for (const id of ids) {
                    const list = await this.findTasksByProcessInstanceId(id);
                    if (list) {
                        allTasks.push(...list);
                    }
                }
                return allTasks;
            },
            (list) => list.some(predicate),
            'querySync',
            description
        );
        return tasks.find(predicate)!;
    }

    async waitForCandidateGroups(taskId: string, expectedGroups: string[]): Promise<string[]> {
        return QueryService.waitFor(
            () => this.getCandidateGroups(taskId),
            (groups) => expectedGroups.every((g) => groups.includes(g)),
            'querySync',
            `candidate groups [${expectedGroups.join(',')}] on task ${taskId}`
        );
    }

    async waitForCandidateUser(taskId: string, expectedUser: string): Promise<string[]> {
        return QueryService.waitFor(
            () => this.getCandidateUsers(taskId),
            (users) => users.includes(expectedUser),
            'querySync',
            `candidate user ${expectedUser} on task ${taskId}`
        );
    }

    async waitForRootTasks(processInstanceId: string): Promise<CloudTask[]> {
        return QueryService.waitFor(
            () => this.getRootTasksByProcessInstance(processInstanceId),
            (tasks) => tasks.length > 0 && tasks.every((t) => !t.parentTaskId),
            'querySync',
            `root tasks on process ${processInstanceId}`
        );
    }

    async getRootTasksByProcessInstance(processInstanceId: string): Promise<CloudTask[]> {
        return this.tasksEndpoint.getRootTasksByProcessInstance(processInstanceId);
    }

    async getApplications(): Promise<{ name: string; [key: string]: unknown }[]> {
        return this.applicationsEndpoint.getApplications();
    }

    async searchTasks(searchRequest: TaskSearchRequest = {}, page?: SearchPageParams): Promise<CloudTask[]> {
        return this.tasksEndpoint.searchTasks(searchRequest, page);
    }

    async countTasks(searchRequest: TaskSearchRequest = {}, page?: SearchPageParams): Promise<number> {
        return this.tasksEndpoint.countTasks(searchRequest, page);
    }

    async searchProcessInstances(
        searchRequest: ProcessInstanceSearchRequest = {},
        page?: SearchPageParams
    ): Promise<CloudProcessInstance[]> {
        return this.processInstancesEndpoint.searchProcessInstances(searchRequest, page);
    }

    async countProcessInstances(searchRequest: ProcessInstanceSearchRequest = {}, page?: SearchPageParams): Promise<number> {
        return this.processInstancesEndpoint.countProcessInstances(searchRequest, page);
    }

    async linkProcessInstances(
        mainProcessInstanceId: string,
        processInstanceIds: string[],
        linkProcessInstanceType: string
    ): Promise<void> {
        await this.processInstancesEndpoint.linkProcessInstances(mainProcessInstanceId, processInstanceIds, linkProcessInstanceType);
    }

    async checkServicesHealth(): Promise<void> {
        const response = await this.get('/query/actuator/health');
        const status = (response as { status?: string }).status;
        if (status !== 'UP') {
            throw new Error(`Query service health check failed: status=${status}`);
        }
    }

    buildUnauthenticatedGetStatusChecks(fakeResourceId: string): readonly HttpStatusCheck<QueryService>[] {
        return [
            BaseService.getStatusCheck<QueryService>('tasks list', `${QUERY_V1_BASE}/tasks`),
            BaseService.getStatusCheck<QueryService>('task by id', `${QUERY_V1_BASE}/tasks/${encodeURIComponent(fakeResourceId)}`),
            BaseService.getStatusCheck<QueryService>(
                'process instance subprocesses',
                `${QUERY_V1_BASE}/process-instances/${fakeResourceId}/subprocesses`
            ),
        ];
    }

    buildUnauthenticatedPostStatusChecks(
        fakeResourceId: string,
        linkType: string
    ): readonly HttpStatusCheck<QueryService>[] {
        const taskSearchBody = { onlyStandalone: false, onlyRoot: false, id: [fakeResourceId] };
        return [
            BaseService.postStatusCheck<QueryService>('task search', `${QUERY_V1_BASE}/tasks/search`, taskSearchBody),
            BaseService.postStatusCheck<QueryService>('task count', `${QUERY_V1_BASE}/tasks/count`, taskSearchBody),
            BaseService.postStatusCheck<QueryService>('process instance search', `${QUERY_V1_BASE}/process-instances/search`, {
                id: [fakeResourceId],
            }),
            BaseService.postStatusCheck<QueryService>('process instance count', `${QUERY_V1_BASE}/process-instances/count`, {
                id: [fakeResourceId],
            }),
            BaseService.postStatusCheck<QueryService>(
                'process instance link',
                `${QUERY_V1_BASE}/process-instances/${fakeResourceId}/link`,
                { processInstanceIds: [fakeResourceId], linkProcessInstanceType: linkType }
            ),
        ];
    }

    buildNotFoundGetStatusChecks(fakeResourceId: string): readonly HttpStatusCheck<QueryService>[] {
        return [
            BaseService.getStatusCheck<QueryService>('task by id', `${QUERY_V1_BASE}/tasks/${encodeURIComponent(fakeResourceId)}`),
            BaseService.getStatusCheck<QueryService>(
                'process instance subprocesses',
                `${QUERY_V1_BASE}/process-instances/${fakeResourceId}/subprocesses`
            ),
        ];
    }

    buildNotFoundPostStatusChecks(fakeResourceId: string, linkType: string): readonly HttpStatusCheck<QueryService>[] {
        return [
            BaseService.postStatusCheck<QueryService>('process instance link', `${QUERY_V1_BASE}/process-instances/${fakeResourceId}/link`, {
                processInstanceIds: [fakeResourceId],
                linkProcessInstanceType: linkType,
            }),
        ];
    }

    buildBadRequestPostStatusChecks(fakeResourceId: string): readonly HttpStatusCheck<QueryService>[] {
        return [
            BaseService.postStatusCheck<QueryService>('task search', `${QUERY_V1_BASE}/tasks/search`, { id: [fakeResourceId] }),
            BaseService.postStatusCheck<QueryService>('task count', `${QUERY_V1_BASE}/tasks/count`, { id: [fakeResourceId] }),
            BaseService.postStatusCheck<QueryService>('process instance search', `${QUERY_V1_BASE}/process-instances/search`, {
                id: 'not-an-array',
            }),
        ];
    }

    buildForbiddenGetStatusChecks(taskId: string): readonly HttpStatusCheck<QueryService>[] {
        return [
            BaseService.getStatusCheck<QueryService>('task by id', `${QUERY_V1_BASE}/tasks/${encodeURIComponent(taskId)}`),
            BaseService.getStatusCheck<QueryService>(
                'task candidate users',
                `${QUERY_V1_BASE}/tasks/${encodeURIComponent(taskId)}/candidate-users`
            ),
            BaseService.getStatusCheck<QueryService>(
                'task candidate groups',
                `${QUERY_V1_BASE}/tasks/${encodeURIComponent(taskId)}/candidate-groups`
            ),
        ];
    }

    buildBadRequestLinkStatusChecks(
        mainProcessInstanceId: string,
        linkProcessInstanceType: string
    ): readonly HttpStatusCheck<QueryService>[] {
        return [
            BaseService.postStatusCheck<QueryService>(
                'process instance link',
                `${QUERY_V1_BASE}/process-instances/${mainProcessInstanceId}/link`,
                { processInstanceIds: 'invalid', linkProcessInstanceType }
            ),
        ];
    }
}
