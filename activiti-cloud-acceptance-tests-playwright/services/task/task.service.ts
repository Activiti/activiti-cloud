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

import { CloudTask, TaskStatus } from '../../models/task.models';
import { CloudVariableInstance } from '../../models/process-variable.models';
import { BaseService } from '../base.service';
import { CustomAPIRequest } from '../../fixtures/context.models';
import { DirtyContextRegistry } from '../../helpers/dirty-context';
import { TestScope } from '../../helpers/test-isolation';
import { buildCreateTaskPayload, RbTasksEndpoint } from './endpoints/tasks.endpoint';

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

    async getFirstTaskByProcessInstanceId(processInstanceId: string): Promise<CloudTask> {
        const tasks = await this.tasks.getTasksByProcessInstanceId(processInstanceId);
        if (tasks.length === 0) {
            throw new Error(`No tasks found for process instance ${processInstanceId}`);
        }
        return tasks[0];
    }

    async waitForOpenTaskByProcessInstanceId(processInstanceId: string): Promise<CloudTask> {
        const tasks = await TaskService.waitFor(
            () => this.tasks.getTasksByProcessInstanceId(processInstanceId),
            (list) => list.some((t) => t.status !== TaskStatus.COMPLETED),
            'querySync',
            `open task on process ${processInstanceId}`
        );
        return tasks.find((t) => t.status !== TaskStatus.COMPLETED)!;
    }

    async findTaskByName(processInstanceId: string, taskName: string): Promise<CloudTask | undefined> {
        const tasks = await this.tasks.getTasksByProcessInstanceId(processInstanceId);
        return tasks.find((task) => task.name === taskName);
    }

    async waitForTaskByName(processInstanceId: string, taskName: string): Promise<CloudTask> {
        const tasks = await TaskService.waitFor(
            () => this.tasks.getTasksByProcessInstanceId(processInstanceId),
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
            () => this.tasks.getTasksByProcessInstanceId(processInstanceId),
            (list) => list.some((task) => task.name === taskName && task.status === status),
            'querySync',
            `task ${taskName} with status ${status} on process ${processInstanceId}`
        );
        return tasks.find((task) => task.name === taskName && task.status === status)!;
    }

    async waitForTaskStatus(taskId: string, expectedStatus: TaskStatus): Promise<CloudTask> {
        return TaskService.waitFor(
            () => this.tasks.getTaskById(taskId),
            (task) => task.status === expectedStatus,
            'querySync',
            `task ${taskId} to reach status ${expectedStatus}`
        );
    }

    async waitForTaskAssignee(taskId: string, expectedAssignee: string): Promise<CloudTask> {
        const task = await TaskService.waitFor(
            () => this.tasks.tryGetTaskById(taskId),
            (value) => value?.assignee === expectedAssignee,
            'querySync',
            `task ${taskId} to be assigned to ${expectedAssignee}`
        );
        return task!;
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

    async waitForTaskVariablesIncluding(
        taskId: string,
        variableNames: readonly string[]
    ): Promise<CloudVariableInstance[]> {
        return TaskService.waitFor(
            () => this.tasks.getTaskVariables(taskId),
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
            () => this.tasks.getTaskVariables(taskId),
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
            () => this.tasks.getTasksByProcessInstanceId(processInstanceId),
            (tasks) => tasks.length === expectedCount,
            'querySync',
            `process ${processInstanceId} to have ${expectedCount} tasks`
        );
    }

    async waitForActiveTasksCount(processInstanceId: string, expectedCount: number): Promise<CloudTask[]> {
        const tasks = await TaskService.waitFor(
            () => this.tasks.getTasksByProcessInstanceId(processInstanceId),
            (list) => list.filter((task) => task.status !== TaskStatus.COMPLETED).length === expectedCount,
            'querySync',
            `process ${processInstanceId} to have ${expectedCount} active tasks`
        );
        return tasks.filter((task) => task.status !== TaskStatus.COMPLETED);
    }

    filterByStandalone(tasks: CloudTask[], standalone: boolean): CloudTask[] {
        return tasks.filter((task) => Boolean(task.standalone) === standalone);
    }

    async isTaskNotFoundInRuntime(taskId: string): Promise<boolean> {
        const task = await this.tasks.tryGetTaskById(taskId);
        return task === undefined;
    }
}
