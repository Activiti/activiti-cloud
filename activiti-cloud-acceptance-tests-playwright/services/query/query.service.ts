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
} from '../../models/runtime-bundle.models';
import { CloudProcessDefinition } from '../../models/process-definition.models';
import { CloudVariableInstance } from '../../models/process-variable.models';
import { CloudTask, TaskStatus } from '../../models/task.models';
import { BaseService } from '../base.service';
import { RuntimeBundleService } from '../runtime-bundle.service';
import { CustomAPIRequest } from '../../fixtures/context.models';
import { isDiagramShown } from '../../helpers/diagram-utils';
import { PollProfile } from '../../config/runtime/timeouts';
import { HttpStatusCheck } from '../../models/base-service.models';
import {
    QUERY_ADMIN_V1_BASE,
    QUERY_V1_BASE,
    QueryApplicationsEndpoint,
    QueryIntegrationContextsEndpoint,
    QueryOpenApiSpecEndpoint,
    QueryProcessDefinitionsEndpoint,
    QueryProcessInstancesEndpoint,
    QueryServiceTasksEndpoint,
    QueryTasksEndpoint,
} from './endpoints/index';

export class QueryService extends BaseService {
    public readonly tasks: QueryTasksEndpoint;
    public readonly adminTasks: QueryTasksEndpoint;
    public readonly processInstances: QueryProcessInstancesEndpoint;
    public readonly adminProcessInstances: QueryProcessInstancesEndpoint;
    public readonly adminServiceTasks: QueryServiceTasksEndpoint;
    public readonly adminIntegrationContexts: QueryIntegrationContextsEndpoint;
    public readonly applications: QueryApplicationsEndpoint;
    public readonly adminApplications: QueryApplicationsEndpoint;
    public readonly processDefinitions: QueryProcessDefinitionsEndpoint;
    public readonly adminProcessDefinitions: QueryProcessDefinitionsEndpoint;
    public readonly openApiSpec: QueryOpenApiSpecEndpoint;

    private readonly adminMode: boolean;

    constructor(context: CustomAPIRequest, adminMode = false) {
        super(context);
        this.adminMode = adminMode;
        this.tasks = new QueryTasksEndpoint(context, false);
        this.adminTasks = new QueryTasksEndpoint(context, true);
        this.processInstances = new QueryProcessInstancesEndpoint(context, false);
        this.adminProcessInstances = new QueryProcessInstancesEndpoint(context, true);
        this.adminServiceTasks = new QueryServiceTasksEndpoint(context);
        this.adminIntegrationContexts = new QueryIntegrationContextsEndpoint(context);
        this.applications = new QueryApplicationsEndpoint(context, false);
        this.adminApplications = new QueryApplicationsEndpoint(context, true);
        this.processDefinitions = new QueryProcessDefinitionsEndpoint(context, false);
        this.adminProcessDefinitions = new QueryProcessDefinitionsEndpoint(context, true);
        this.openApiSpec = new QueryOpenApiSpecEndpoint(context);
    }

    async getProcessInstanceWhenSynced(processInstanceId: string): Promise<CloudProcessInstance | undefined> {
        try {
            return await this.processInstances.getProcessInstance(processInstanceId);
        } catch (error) {
            if (QueryService.isProcessInstanceNotFoundError(error)) {
                return undefined;
            }
            throw error;
        }
    }

    async getProcessInstanceWhenGone(processInstanceId: string): Promise<CloudProcessInstance | undefined> {
        try {
            return await this.processInstances.getProcessInstance(processInstanceId);
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

    async getProcessDefinitionByKey(processDefinitionKey: string): Promise<CloudProcessDefinition> {
        const definitions = this.adminMode
            ? await this.adminProcessDefinitions.getProcessDefinitions()
            : await this.processDefinitions.getProcessDefinitions();
        return RuntimeBundleService.pickHighestVersionByKey(definitions, processDefinitionKey);
    }

    async waitForProcessInstanceDiagram(processInstanceId: string): Promise<string> {
        const endpoint = this.adminMode ? this.adminProcessInstances : this.processInstances;
        return QueryService.waitFor(
            () => endpoint.getProcessInstanceDiagram(processInstanceId),
            (diagram) => isDiagramShown(diagram),
            'querySync',
            `process instance diagram for ${processInstanceId}`
        );
    }

    async waitForVariable(
        processInstanceId: string,
        variableName: string,
        predicate: (variable: CloudVariableInstance) => boolean = () => true
    ): Promise<CloudVariableInstance> {
        const variables = await QueryService.waitFor(
            () => this.processInstances.getProcessInstanceVariables(processInstanceId),
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
            () => this.processInstances.getProcessInstanceVariables(processInstanceId),
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
            () => this.processInstances.getProcessInstances({ name: namePrefix }),
            (instances) => instances.some((instance) => instance.name?.includes(namePrefix)),
            'querySync',
            `process instances with name containing ${namePrefix}`
        );
    }

    async waitForSubprocesses(processInstanceId: string): Promise<CloudProcessInstance[]> {
        return QueryService.waitFor(
            () => this.processInstances.getSubprocesses(processInstanceId),
            (subprocesses) => subprocesses.length > 0,
            'querySync',
            `query subprocesses of process ${processInstanceId}`
        );
    }

    async waitForTaskByName(processInstanceId: string, taskName: string): Promise<CloudTask> {
        const tasks = await QueryService.waitFor(
            () => this.processInstances.getTasksByProcessInstanceId(processInstanceId),
            (list) => list.some((t) => t.name === taskName),
            'querySync',
            `task named ${taskName} on process ${processInstanceId}`
        );
        return tasks.find((t) => t.name === taskName)!;
    }

    async findTasksByProcessInstanceId(processInstanceId: string): Promise<CloudTask[] | undefined> {
        try {
            return await this.processInstances.getTasksByProcessInstanceId(processInstanceId);
        } catch {
            return undefined;
        }
    }

    async waitForStandaloneTask(taskId: string): Promise<CloudTask[]> {
        return QueryService.waitFor(
            () => this.tasks.getStandaloneTasks(),
            (tasks) => tasks.some((task) => task.id === taskId),
            'querySync',
            `standalone task ${taskId} to appear in query`
        );
    }

    async waitForQueriedTaskByNameAndDescription(
        namePrefix: string,
        descriptionPrefix: string,
        taskId: string
    ): Promise<CloudTask[]> {
        return QueryService.waitFor(
            () => this.tasks.getTasksByNameAndDescription(namePrefix, descriptionPrefix),
            (tasks) => tasks.some((task) => task.id === taskId),
            'querySync',
            `task ${taskId} to appear by name=${namePrefix} description=${descriptionPrefix}`
        );
    }

    async waitForTaskVariable(
        taskId: string,
        variableName: string,
        predicate: (variable: CloudVariableInstance) => boolean = () => true
    ): Promise<CloudVariableInstance> {
        const variables = await QueryService.waitFor(
            () => this.tasks.getTaskVariables(taskId),
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
            () => this.tasks.getTaskVariables(taskId),
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
            () => this.tasks.getTaskById(taskId),
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
            () => this.tasks.getCandidateGroups(taskId),
            (groups) => expectedGroups.every((g) => groups.includes(g)),
            'querySync',
            `candidate groups [${expectedGroups.join(',')}] on task ${taskId}`
        );
    }

    async waitForCandidateUser(taskId: string, expectedUser: string): Promise<string[]> {
        return QueryService.waitFor(
            () => this.tasks.getCandidateUsers(taskId),
            (users) => users.includes(expectedUser),
            'querySync',
            `candidate user ${expectedUser} on task ${taskId}`
        );
    }

    async waitForRootTasks(processInstanceId: string): Promise<CloudTask[]> {
        return QueryService.waitFor(
            () => this.tasks.getRootTasksByProcessInstance(processInstanceId),
            (tasks) => tasks.length > 0 && tasks.every((t) => !t.parentTaskId),
            'querySync',
            `root tasks on process ${processInstanceId}`
        );
    }

    async getLinkedProcesses(mainProcessInstanceId: string): Promise<CloudProcessInstance[]> {
        const results = await this.processInstances.searchProcessInstances({ id: [mainProcessInstanceId] });
        const mainProcess = results.find((instance) => instance.id === mainProcessInstanceId);
        return (mainProcess?.linkedProcesses as CloudProcessInstance[] | undefined) ?? [];
    }

    async waitForLinkedProcess(
        mainProcessInstanceId: string,
        linkedProcessInstanceId: string
    ): Promise<CloudProcessInstance[]> {
        return QueryService.waitFor(
            () => this.getLinkedProcesses(mainProcessInstanceId),
            (instances) => instances.some((instance) => instance.id === linkedProcessInstanceId),
            'querySync',
            `linked process ${linkedProcessInstanceId} under ${mainProcessInstanceId}`
        );
    }

    async checkServicesHealth(): Promise<void> {
        const response = await this.get('/query/actuator/health');
        const status = (response as { status?: string }).status;
        if (status !== 'UP') {
            throw new Error(`Query service health check failed: status=${status}`);
        }
    }

    async getProcessInstanceAdminWhenSynced(processInstanceId: string): Promise<CloudProcessInstance | undefined> {
        try {
            return await this.adminProcessInstances.getProcessInstance(processInstanceId);
        } catch (error) {
            if (QueryService.isProcessInstanceNotFoundError(error)) {
                return undefined;
            }
            throw error;
        }
    }

    async waitForLinkedProcessAdmin(
        mainProcessInstanceId: string,
        linkedProcessInstanceId: string
    ): Promise<CloudProcessInstance[]> {
        return QueryService.waitFor(
            () => this.adminProcessInstances.getLinkedProcesses(mainProcessInstanceId),
            (instances) => instances.some((instance) => instance.id === linkedProcessInstanceId),
            'querySync',
            `linked process ${linkedProcessInstanceId} under ${mainProcessInstanceId}`
        );
    }

    async waitForProcessInstancesAdminCountGreaterThan(
        params: ProcessQueryParams,
        minCount: number
    ): Promise<CloudProcessInstance[]> {
        return QueryService.waitFor(
            () => this.adminProcessInstances.getProcessInstances(params),
            (instances) => instances.length > minCount,
            'querySync',
            `admin process instances ${JSON.stringify(params)} count > ${minCount}`
        );
    }

    async getProcessInstanceStatusesByBusinessKey(
        processDefinitionKey: string,
        businessKey: string
    ): Promise<ProcessInstanceStatus[]> {
        const instances = await this.adminProcessInstances.getProcessInstances({ processDefinitionKey });
        return instances.filter((instance) => instance.businessKey === businessKey).map((instance) => instance.status);
    }

    async waitForProcessInstanceStatusByBusinessKey(
        processDefinitionKey: string,
        businessKey: string,
        expectedStatus: ProcessInstanceStatus
    ): Promise<ProcessInstanceStatus[]> {
        return QueryService.waitFor(
            () => this.getProcessInstanceStatusesByBusinessKey(processDefinitionKey, businessKey),
            (statuses) => statuses.includes(expectedStatus),
            'querySync',
            `${processDefinitionKey} businessKey ${businessKey} to reach status ${expectedStatus}`
        );
    }

    async waitForServiceTasksForProcessInstance(
        processInstanceId: string,
        predicate: (tasks: CloudServiceTask[]) => boolean,
        description: string
    ): Promise<CloudServiceTask[]> {
        return QueryService.waitFor(
            () => this.adminServiceTasks.getServiceTasksForProcessInstance(processInstanceId),
            predicate,
            'querySync',
            description
        );
    }

    async waitForServiceTasksByStatusForProcessInstance(
        processInstanceId: string,
        status: ServiceTaskStatus | string,
        predicate: (tasks: CloudServiceTask[]) => boolean = (tasks) => tasks.length > 0
    ): Promise<CloudServiceTask[]> {
        return QueryService.waitFor(
            () => this.adminServiceTasks.getServiceTasksByStatusForProcessInstance(processInstanceId, status),
            predicate,
            'querySync',
            `service tasks status ${status} for process ${processInstanceId}`
        );
    }

    async findServiceTaskIntegrationContext(serviceTaskId: string): Promise<CloudIntegrationContext | undefined> {
        try {
            return await this.adminServiceTasks.getServiceTaskIntegrationContext(serviceTaskId);
        } catch {
            return undefined;
        }
    }

    async findServiceTaskIntegrationContexts(serviceTaskId: string): Promise<CloudIntegrationContext[] | undefined> {
        try {
            return await this.adminServiceTasks.getServiceTaskIntegrationContexts(serviceTaskId);
        } catch {
            return undefined;
        }
    }

    async waitForServiceTasksByQuery(
        params: { processDefinitionKey?: string; status?: ServiceTaskStatus | string },
        predicate: (tasks: CloudServiceTask[]) => boolean = (tasks) => tasks.length > 0
    ): Promise<CloudServiceTask[]> {
        return QueryService.waitFor(
            () => this.adminServiceTasks.getServiceTasksByQuery(params),
            predicate,
            'querySync',
            `service tasks query ${JSON.stringify(params)}`
        );
    }

    async waitForProcessInstanceAdminSynced(processInstanceId: string): Promise<CloudProcessInstance> {
        const instance = await QueryService.waitFor(
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
        const instance = await QueryService.waitFor(
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
        const context = await QueryService.waitFor(
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
        const contexts = await QueryService.waitFor(
            () => this.findServiceTaskIntegrationContexts(serviceTaskId),
            (value) => value !== undefined && predicate(value),
            'querySync',
            `service task ${serviceTaskId} integration contexts`
        );
        return contexts!;
    }

    async getTaskAdminByIdWhenSynced(taskId: string): Promise<CloudTask | undefined> {
        try {
            return await this.adminTasks.getTask(taskId);
        } catch {
            return undefined;
        }
    }

    async waitForTaskAdminSynced(taskId: string): Promise<CloudTask> {
        const task = await QueryService.waitFor(
            () => this.getTaskAdminByIdWhenSynced(taskId),
            (value) => value !== undefined,
            'querySync',
            `admin task ${taskId} to be synced to query`
        );
        return task!;
    }

    async waitForTaskVariablesAdmin(taskId: string, expected: Record<string, unknown>): Promise<CloudVariableInstance[]> {
        return QueryService.waitFor(
            () => this.adminTasks.getTaskVariables(taskId),
            (variables) => {
                const map = Object.fromEntries(variables.map((variable) => [variable.name, variable.value]));
                return Object.keys(expected).every((name) => map[name] === expected[name]);
            },
            'querySync',
            `admin task ${taskId} variables to match ${JSON.stringify(expected)}`
        );
    }

    async waitForAllProcessInstancesAdminCount(expectedCount: number): Promise<CloudProcessInstance[]> {
        return QueryService.waitFor(
            () => this.adminProcessInstances.getAllProcessInstances(),
            (instances) => instances.length === expectedCount,
            'querySync',
            `admin process instances count to equal ${expectedCount}`
        );
    }

    async waitForAllProcessInstancesAdminCountGreaterThan(minCount: number): Promise<CloudProcessInstance[]> {
        return QueryService.waitFor(
            () => this.adminProcessInstances.getAllProcessInstances(),
            (instances) => instances.length > minCount,
            'querySync',
            `admin process instances count > ${minCount}`
        );
    }

    async waitForAllTasksAdminCount(expectedCount: number): Promise<CloudTask[]> {
        return QueryService.waitFor(
            () => this.adminTasks.getAllTasks(),
            (tasks) => tasks.length === expectedCount,
            'querySync',
            `admin tasks count to equal ${expectedCount}`
        );
    }

    async waitForAllTasksAdminCountGreaterThan(minCount: number): Promise<CloudTask[]> {
        return QueryService.waitFor(
            () => this.adminTasks.getAllTasks(),
            (tasks) => tasks.length > minCount,
            'querySync',
            `admin tasks count > ${minCount}`
        );
    }

    buildUnauthenticatedGetStatusChecks(fakeResourceId: string): readonly HttpStatusCheck<QueryService>[] {
        if (this.adminMode) {
            return [
                BaseService.getStatusCheck<QueryService>('tasks list', `${QUERY_ADMIN_V1_BASE}/tasks`),
                BaseService.getStatusCheck<QueryService>('process instances list', `${QUERY_ADMIN_V1_BASE}/process-instances`),
                BaseService.getStatusCheck<QueryService>(
                    'process instances with variable keys',
                    `${QUERY_ADMIN_V1_BASE}/process-instances?variableKeys=start1`
                ),
                BaseService.getStatusCheck<QueryService>(
                    'process instance by id',
                    `${QUERY_ADMIN_V1_BASE}/process-instances/${fakeResourceId}`
                ),
                BaseService.getStatusCheck<QueryService>('task by id', `${QUERY_ADMIN_V1_BASE}/tasks/${encodeURIComponent(fakeResourceId)}`),
                BaseService.getStatusCheck<QueryService>(
                    'task candidate groups',
                    `${QUERY_ADMIN_V1_BASE}/tasks/${encodeURIComponent(fakeResourceId)}/candidate-groups`
                ),
                BaseService.getStatusCheck<QueryService>(
                    'task candidate users',
                    `${QUERY_ADMIN_V1_BASE}/tasks/${encodeURIComponent(fakeResourceId)}/candidate-users`
                ),
                BaseService.getStatusCheck<QueryService>(
                    'task variables',
                    `${QUERY_ADMIN_V1_BASE}/tasks/${encodeURIComponent(fakeResourceId)}/variables`
                ),
                BaseService.getStatusCheck<QueryService>('applications list', `${QUERY_ADMIN_V1_BASE}/applications`),
                BaseService.getStatusCheck<QueryService>(
                    'integration context by id',
                    `${QUERY_ADMIN_V1_BASE}/integration-contexts/${encodeURIComponent(fakeResourceId)}`
                ),
            ];
        }
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
        linkType?: string
    ): readonly HttpStatusCheck<QueryService>[] {
        if (this.adminMode) {
            const taskSearchBody = { onlyStandalone: false, onlyRoot: false, id: [fakeResourceId] };
            return [
                BaseService.postStatusCheck<QueryService>('process instance search', `${QUERY_ADMIN_V1_BASE}/process-instances/search`, {
                    id: [fakeResourceId],
                }),
                BaseService.postStatusCheck<QueryService>('process instance count', `${QUERY_ADMIN_V1_BASE}/process-instances/count`, {
                    id: [fakeResourceId],
                }),
                BaseService.postStatusCheck<QueryService>('task search', `${QUERY_ADMIN_V1_BASE}/tasks/search`, taskSearchBody),
                BaseService.postStatusCheck<QueryService>('task count', `${QUERY_ADMIN_V1_BASE}/tasks/count`, taskSearchBody),
            ];
        }
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
                { processInstanceIds: [fakeResourceId], linkProcessInstanceType: linkType! }
            ),
        ];
    }

    buildNotFoundGetStatusChecks(fakeResourceId: string): readonly HttpStatusCheck<QueryService>[] {
        if (this.adminMode) {
            return [
                BaseService.getStatusCheck<QueryService>(
                    'process instance by id',
                    `${QUERY_ADMIN_V1_BASE}/process-instances/${fakeResourceId}`
                ),
                BaseService.getStatusCheck<QueryService>('task by id', `${QUERY_ADMIN_V1_BASE}/tasks/${encodeURIComponent(fakeResourceId)}`),
                BaseService.getStatusCheck<QueryService>(
                    'task candidate groups',
                    `${QUERY_ADMIN_V1_BASE}/tasks/${encodeURIComponent(fakeResourceId)}/candidate-groups`
                ),
                BaseService.getStatusCheck<QueryService>(
                    'task candidate users',
                    `${QUERY_ADMIN_V1_BASE}/tasks/${encodeURIComponent(fakeResourceId)}/candidate-users`
                ),
                BaseService.getStatusCheck<QueryService>(
                    'integration context by id',
                    `${QUERY_ADMIN_V1_BASE}/integration-contexts/${encodeURIComponent(fakeResourceId)}`
                ),
            ];
        }
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
        const base = this.adminMode ? QUERY_ADMIN_V1_BASE : QUERY_V1_BASE;
        return [
            BaseService.postStatusCheck<QueryService>('task search', `${base}/tasks/search`, { id: [fakeResourceId] }),
            BaseService.postStatusCheck<QueryService>('task count', `${base}/tasks/count`, { id: [fakeResourceId] }),
            BaseService.postStatusCheck<QueryService>('process instance search', `${base}/process-instances/search`, {
                id: 'not-an-array',
            }),
        ];
    }

    buildForbiddenGetStatusChecks(
        taskId: string,
        processInstanceId?: string
    ): readonly HttpStatusCheck<QueryService>[] {
        if (processInstanceId !== undefined) {
            return [
                BaseService.getStatusCheck<QueryService>('applications list', `${QUERY_ADMIN_V1_BASE}/applications`),
                BaseService.getStatusCheck<QueryService>('tasks list', `${QUERY_ADMIN_V1_BASE}/tasks`),
                BaseService.getStatusCheck<QueryService>('process instances list', `${QUERY_ADMIN_V1_BASE}/process-instances`),
                BaseService.getStatusCheck<QueryService>(
                    'process instances with variable keys',
                    `${QUERY_ADMIN_V1_BASE}/process-instances?variableKeys=start1`
                ),
                BaseService.getStatusCheck<QueryService>('task by id', `${QUERY_ADMIN_V1_BASE}/tasks/${encodeURIComponent(taskId)}`),
                BaseService.getStatusCheck<QueryService>(
                    'task variables',
                    `${QUERY_ADMIN_V1_BASE}/tasks/${encodeURIComponent(taskId)}/variables`
                ),
                BaseService.getStatusCheck<QueryService>(
                    'process instance variables',
                    `${QUERY_ADMIN_V1_BASE}/process-instances/${processInstanceId}/variables`
                ),
                BaseService.getStatusCheck<QueryService>(
                    'process instance sequence flows',
                    `${QUERY_ADMIN_V1_BASE}/process-instances/${processInstanceId}/sequence-flows`
                ),
                BaseService.getStatusCheck<QueryService>(
                    'process instance subprocesses',
                    `${QUERY_ADMIN_V1_BASE}/process-instances/${processInstanceId}/subprocesses`
                ),
            ];
        }
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

    buildForbiddenPostStatusChecks(taskId: string, fakeResourceId: string): readonly HttpStatusCheck<QueryService>[] {
        const taskSearchBody = { onlyStandalone: false, onlyRoot: false, id: [taskId] };
        return [
            BaseService.postStatusCheck<QueryService>('task search', `${QUERY_ADMIN_V1_BASE}/tasks/search`, taskSearchBody),
            BaseService.postStatusCheck<QueryService>('task count', `${QUERY_ADMIN_V1_BASE}/tasks/count`, taskSearchBody),
            BaseService.postStatusCheck<QueryService>('process instance search', `${QUERY_ADMIN_V1_BASE}/process-instances/search`, {
                id: [fakeResourceId],
            }),
            BaseService.postStatusCheck<QueryService>('process instance count', `${QUERY_ADMIN_V1_BASE}/process-instances/count`, {
                id: [fakeResourceId],
            }),
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
