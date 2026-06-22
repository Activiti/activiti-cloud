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

import { CloudProcessInstance, ProcessInstanceStatus, ProcessQueryParams } from '../models/runtime-bundle.models';
import { CloudProcessDefinition } from '../models/process-definition.models';
import { CloudVariableInstance } from '../models/process-variable.models';
import { CloudTask, TaskQueryParams, TaskStatus } from '../models/task.models';
import { BaseService } from './base.service';
import { RuntimeBundleService } from './runtime-bundle.service';
import { CustomAPIRequest } from '../fixtures/context.models';
import { isDiagramShown } from '../helpers/diagram-utils';
import { PollProfile } from '../config/runtime/timeouts';

export class QueryService extends BaseService {
    private readonly basePath = '/query/v1';

    constructor(context: CustomAPIRequest) {
        super(context);
    }

    async getAllProcessInstances(): Promise<CloudProcessInstance[]> {
        const response = await this.get(`${this.basePath}/process-instances`);
        return this.unwrapList<CloudProcessInstance>(response, 'processInstances');
    }

    async getProcessInstance(processInstanceId: string): Promise<CloudProcessInstance> {
        const response = await this.get(`${this.basePath}/process-instances/${processInstanceId}`);
        return this.unwrapEntity<CloudProcessInstance>(response);
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
        return (
            message.includes('Unable to find process instance') ||
            message.includes('Operation not permitted')
        );
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

        return this.unwrapList<CloudProcessInstance>(response, 'processInstances');
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

        const response = await this.get(
            `${this.basePath}/tasks?${searchParams.toString()}`
        );

        return this.unwrapList<CloudTask>(response, 'tasks');
    }

    async getProcessDefinitions(): Promise<CloudProcessDefinition[]> {
        const response = await this.get(`${this.basePath}/process-definitions`);
        return this.unwrapList<CloudProcessDefinition>(response, 'processDefinitions');
    }

    async getProcessDefinitionByKey(processDefinitionKey: string): Promise<CloudProcessDefinition> {
        const definitions = await this.getProcessDefinitions();
        return RuntimeBundleService.pickHighestVersionByKey(definitions, processDefinitionKey);
    }

    async getProcessInstanceDiagram(processInstanceId: string): Promise<string> {
        return this.getText(`${this.basePath}/process-instances/${processInstanceId}/diagram`, {
            Accept: 'image/svg+xml',
        });
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
        return this.getText(`${this.basePath}/process-definitions/${processDefinitionId}/model`);
    }

    async getSwaggerSpecification(group: string = 'Query'): Promise<string> {
        const root = this.basePath.replace(/\/v1$/, '');
        return this.getText(`${root}/v3/api-docs/${encodeURIComponent(group)}`);
    }

    async getProcessInstanceVariables(processInstanceId: string): Promise<CloudVariableInstance[]> {
        const response = await this.get(`${this.basePath}/process-instances/${processInstanceId}/variables`);
        return this.unwrapList<CloudVariableInstance>(response, 'variables');
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
        const response = await this.get(`${this.basePath}/tasks?id=${encodeURIComponent(taskId)}`);
        const tasks = this.unwrapList<CloudTask>(response, 'tasks');
        return tasks[0];
    }

    async getTasksByProcessInstanceId(processInstanceId: string): Promise<CloudTask[]> {
        const response = await this.get(`${this.basePath}/process-instances/${processInstanceId}/tasks`);
        return this.unwrapList<CloudTask>(response, 'tasks');
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
        const response = await this.get(
            `${this.basePath}/tasks?standalone=true&sort=createdDate,desc&sort=id,desc`
        );
        return this.unwrapList<CloudTask>(response, 'tasks');
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
        const response = await this.get(
            `${this.basePath}/tasks?name=${encodeURIComponent(namePrefix)}&description=${encodeURIComponent(descriptionPrefix)}`
        );
        return this.unwrapList<CloudTask>(response, 'tasks');
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
        const response = await this.get(`${this.basePath}/tasks/${taskId}/candidate-users`);
        if (Array.isArray(response)) {
            return response as string[];
        }
        if (Array.isArray(response.body)) {
            return response.body as string[];
        }
        return [];
    }

    async getCandidateGroups(taskId: string): Promise<string[]> {
        const response = await this.get(`${this.basePath}/tasks/${taskId}/candidate-groups`);
        if (Array.isArray(response)) {
            return response as string[];
        }
        if (Array.isArray(response.body)) {
            return response.body as string[];
        }
        return [];
    }

    async getTaskVariables(taskId: string): Promise<CloudVariableInstance[]> {
        const response = await this.get(`${this.basePath}/tasks/${taskId}/variables`);
        return this.unwrapList<CloudVariableInstance>(response, 'variables');
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

    async waitForTaskVariableValues(
        taskId: string,
        expected: Record<string, unknown>
    ): Promise<CloudVariableInstance[]> {
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

    async waitForTaskById(
        taskId: string,
        predicate: (task: CloudTask) => boolean
    ): Promise<CloudTask> {
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
        return this.waitForTaskById(
            taskId,
            (task) => task.status === TaskStatus.COMPLETED && Boolean(task.completedDate)
        );
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
        const response = await this.get(
            `${this.basePath}/tasks?rootTasksOnly=true&processInstanceId=${encodeURIComponent(processInstanceId)}&sort=createdDate,desc&sort=id,desc`
        );
        return this.unwrapList<CloudTask>(response, 'tasks');
    }

    async getApplications(): Promise<{ name: string; [key: string]: unknown }[]> {
        const response = await this.get(`${this.basePath}/applications`);
        return this.unwrapList<{ name: string; [key: string]: unknown }>(response, 'applications');
    }

    async checkServicesHealth(): Promise<void> {
        const response = await this.get('/query/actuator/health');
        const status = (response as { status?: string }).status;
        if (status !== 'UP') {
            throw new Error(`Query service health check failed: status=${status}`);
        }
    }
}
