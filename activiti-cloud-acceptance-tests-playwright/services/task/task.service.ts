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

import { CloudTask, TaskQueryParams, TaskStatus } from '../../models/task.models';
import { CloudVariableInstance } from '../../models/process-variable.models';
import { BaseService, RequestResponse } from '../base.service';
import { CustomAPIRequest } from '../../fixtures/context.models';
import { DirtyContextRegistry } from '../../helpers/dirty-context';
import { TestScope } from '../../helpers/test-isolation';
import { buildCreateTaskPayload, RbTasksEndpoint } from './endpoints/index';

export class TaskService extends BaseService {
    readonly tasks: RbTasksEndpoint;

    constructor(context: CustomAPIRequest) {
        super(context);
        this.tasks = new RbTasksEndpoint(context);
    }

    attachIsolation(dirtyRegistry?: DirtyContextRegistry, testScope?: TestScope, basePath?: string): void {
        super.attachIsolation(dirtyRegistry, testScope, basePath);
        this.tasks.attachIsolation(dirtyRegistry, testScope, basePath);
    }

    async getAllTasks(): Promise<CloudTask[]> {
        return this.tasks.getAllTasks();
    }

    async getTasks(params?: TaskQueryParams): Promise<CloudTask[]> {
        return this.tasks.getTasks(params);
    }

    async getTasksByProcessInstanceId(processInstanceId: string): Promise<CloudTask[]> {
        return this.tasks.getTasksByProcessInstanceId(processInstanceId);
    }

    async getFirstTaskByProcessInstanceId(processInstanceId: string): Promise<CloudTask> {
        const tasks = await this.getTasksByProcessInstanceId(processInstanceId);
        if (tasks.length === 0) {
            throw new Error(`No tasks found for process instance ${processInstanceId}`);
        }
        return tasks[0];
    }

    async waitForOpenTaskByProcessInstanceId(processInstanceId: string): Promise<CloudTask> {
        const tasks = await TaskService.waitFor(
            () => this.getTasksByProcessInstanceId(processInstanceId),
            (list) => list.some((t) => t.status !== TaskStatus.COMPLETED),
            'querySync',
            `open task on process ${processInstanceId}`
        );
        return tasks.find((t) => t.status !== TaskStatus.COMPLETED)!;
    }

    async findTaskByName(processInstanceId: string, taskName: string): Promise<CloudTask | undefined> {
        const tasks = await this.getTasksByProcessInstanceId(processInstanceId);
        return tasks.find((task) => task.name === taskName);
    }

    async waitForTaskByName(processInstanceId: string, taskName: string): Promise<CloudTask> {
        const tasks = await TaskService.waitFor(
            () => this.getTasksByProcessInstanceId(processInstanceId),
            (list) => list.some((task) => task.name === taskName),
            'querySync',
            `task named ${taskName} on process ${processInstanceId}`
        );
        return tasks.find((task) => task.name === taskName)!;
    }

    async waitForTaskWithNameAndStatus(
        processInstanceId: string,
        taskName: string,
        status: TaskStatus
    ): Promise<CloudTask> {
        const tasks = await TaskService.waitFor(
            () => this.getTasksByProcessInstanceId(processInstanceId),
            (list) => list.some((task) => task.name === taskName && task.status === status),
            'querySync',
            `task ${taskName} with status ${status} on process ${processInstanceId}`
        );
        return tasks.find((task) => task.name === taskName && task.status === status)!;
    }

    async waitForTaskStatus(taskId: string, expectedStatus: TaskStatus): Promise<CloudTask> {
        return TaskService.waitFor(
            () => this.getTaskById(taskId),
            (task) => task.status === expectedStatus,
            'querySync',
            `task ${taskId} to reach status ${expectedStatus}`
        );
    }

    async waitForTaskAssignee(taskId: string, expectedAssignee: string): Promise<CloudTask> {
        const task = await TaskService.waitFor(
            () => this.tryGetTaskById(taskId),
            (value) => value?.assignee === expectedAssignee,
            'querySync',
            `task ${taskId} to be assigned to ${expectedAssignee}`
        );
        return task!;
    }

    async getTaskById(taskId: string): Promise<CloudTask> {
        return this.tasks.getTaskById(taskId);
    }

    async tryGetTaskById(taskId: string): Promise<CloudTask | undefined> {
        return this.tasks.tryGetTaskById(taskId);
    }

    async createStandaloneTask(options?: {
        name?: string;
        description?: string;
        assignee?: string;
    }): Promise<CloudTask> {
        return this.tasks.createTask(
            buildCreateTaskPayload({
                name: options?.name ?? 'new-task',
                description: options?.description ?? 'task-description',
                assignee: options?.assignee ?? 'testuser',
            }),
            'Create standalone task'
        );
    }

    async createUnassignedStandaloneTask(): Promise<CloudTask> {
        return this.tasks.createTask(
            buildCreateTaskPayload({
                name: 'unassigned-task',
                description: 'unassigned-task-description',
            }),
            'Create unassigned standalone task'
        );
    }

    async createSubtask(parentTaskId: string): Promise<CloudTask> {
        return this.tasks.createTask(
            buildCreateTaskPayload({
                name: 'subtask',
                description: 'subtask-description',
                assignee: 'testuser',
                parentTaskId,
            }),
            'Create subtask'
        );
    }

    async getSubtasks(parentTaskId: string): Promise<CloudTask[]> {
        return this.tasks.getSubtasks(parentTaskId);
    }

    async deleteTask(taskId: string): Promise<void> {
        return this.tasks.deleteTask(taskId);
    }

    async claimTask(taskId: string): Promise<RequestResponse> {
        return this.tasks.claimTask(taskId);
    }

    async completeTask(taskId: string): Promise<RequestResponse> {
        return this.tasks.completeTask(taskId);
    }

    async releaseTask(taskId: string): Promise<void> {
        return this.tasks.releaseTask(taskId);
    }

    async saveTask(taskId: string, variables: Record<string, unknown>): Promise<void> {
        return this.tasks.saveTask(taskId, variables);
    }

    async updateTask(
        taskId: string,
        fields: { name?: string; formKey?: string; priority?: number; dueDate?: string }
    ): Promise<CloudTask> {
        return this.tasks.updateTask(taskId, fields);
    }

    async assignTask(taskId: string, assignee: string): Promise<RequestResponse> {
        return this.tasks.assignTask(taskId, assignee);
    }

    async getCandidateUsers(taskId: string): Promise<string[]> {
        return this.tasks.getCandidateUsers(taskId);
    }

    async getCandidateGroups(taskId: string): Promise<string[]> {
        return this.tasks.getCandidateGroups(taskId);
    }

    async addCandidateUsers(taskId: string, candidateUsers: string[]): Promise<RequestResponse> {
        return this.tasks.addCandidateUsers(taskId, candidateUsers);
    }

    async deleteCandidateUsers(taskId: string, candidateUsers: string[]): Promise<RequestResponse> {
        return this.tasks.deleteCandidateUsers(taskId, candidateUsers);
    }

    async addCandidateGroups(taskId: string, candidateGroups: string[]): Promise<RequestResponse> {
        return this.tasks.addCandidateGroups(taskId, candidateGroups);
    }

    async deleteCandidateGroups(taskId: string, candidateGroups: string[]): Promise<RequestResponse> {
        return this.tasks.deleteCandidateGroups(taskId, candidateGroups);
    }

    async getNextTask(): Promise<CloudTask | undefined> {
        return this.tasks.getNextTask();
    }

    async getTaskVariables(taskId: string): Promise<CloudVariableInstance[]> {
        return this.tasks.getTaskVariables(taskId);
    }

    async waitForTaskVariablesIncluding(
        taskId: string,
        variableNames: readonly string[]
    ): Promise<CloudVariableInstance[]> {
        return TaskService.waitFor(
            () => this.getTaskVariables(taskId),
            (variables) => {
                const names = new Set(variables.map((variable) => variable.name));
                return variableNames.every((name) => names.has(name));
            },
            'querySync',
            `task ${taskId} variables to include [${variableNames.join(',')}]`
        );
    }

    async waitForTaskVariableValues(
        taskId: string,
        expected: Record<string, unknown>
    ): Promise<CloudVariableInstance[]> {
        return TaskService.waitFor(
            () => this.getTaskVariables(taskId),
            (variables) => {
                const map = Object.fromEntries(variables.map((v) => [v.name, v.value]));
                return (
                    Object.keys(expected).every((name) => map[name] === expected[name]) &&
                    Object.keys(map).length === Object.keys(expected).length
                );
            },
            'querySync',
            `task ${taskId} variables to match ${JSON.stringify(expected)}`
        );
    }

    async waitForTasksCount(processInstanceId: string, expectedCount: number): Promise<CloudTask[]> {
        return TaskService.waitFor(
            () => this.getTasksByProcessInstanceId(processInstanceId),
            (tasks) => tasks.length === expectedCount,
            'querySync',
            `process ${processInstanceId} to have ${expectedCount} tasks`
        );
    }

    async waitForActiveTasksCount(processInstanceId: string, expectedCount: number): Promise<CloudTask[]> {
        const tasks = await TaskService.waitFor(
            () => this.getTasksByProcessInstanceId(processInstanceId),
            (list) => list.filter((task) => task.status !== TaskStatus.COMPLETED).length === expectedCount,
            'querySync',
            `process ${processInstanceId} to have ${expectedCount} active tasks`
        );
        return tasks.filter((task) => task.status !== TaskStatus.COMPLETED);
    }

    async updateTaskVariable(taskId: string, name: string, value: unknown): Promise<void> {
        return this.tasks.updateTaskVariable(taskId, name, value);
    }

    async createTaskVariable(taskId: string, name: string, value: unknown): Promise<void> {
        return this.tasks.createTaskVariable(taskId, name, value);
    }

    async completeTaskWithVariables(taskId: string, variables: Record<string, unknown>): Promise<RequestResponse> {
        return this.tasks.completeTask(taskId, variables);
    }

    filterByStandalone(tasks: CloudTask[], standalone: boolean): CloudTask[] {
        return tasks.filter((task) => Boolean(task.standalone) === standalone);
    }

    async isTaskNotFoundInRuntime(taskId: string): Promise<boolean> {
        const task = await this.tryGetTaskById(taskId);
        return task === undefined;
    }
}
