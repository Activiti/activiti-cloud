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

import { CloudTask, TaskQueryParams, TaskStatus } from '../models/task.models';
import { CloudVariableInstance } from '../models/process-variable.models';
import { BaseService, RequestResponse } from './base.service';
import { CustomAPIRequest } from '../fixtures/context.models';

export interface CreateTaskPayload {
    payloadType: 'CreateTaskPayload';
    name: string;
    description?: string;
    assignee?: string;
    parentTaskId?: string;
    /** Required by RB Jackson (primitive int — must not be null in JSON). */
    priority?: number;
}

/** Matches Serenity TaskPayloadBuilder defaults; strips nulls so Jackson never sees null for int fields. */
function buildCreateTaskPayload(
    fields: Omit<CreateTaskPayload, 'payloadType'>
): Record<string, string | number> {
    const raw: Record<string, unknown> = {
        payloadType: 'CreateTaskPayload',
        priority: 50,
        ...fields,
    };
    return Object.fromEntries(
        Object.entries(raw).filter(([, value]) => value !== null && value !== undefined)
    ) as Record<string, string | number>;
}

export class TaskService extends BaseService {
    private readonly basePath = '/rb/v1';

    constructor(context: CustomAPIRequest) {
        super(context);
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

        const response = await this.get(`${this.basePath}/tasks?${searchParams.toString()}`);
        return this.unwrapList<CloudTask>(response, 'tasks');
    }

    async getTasksByProcessInstanceId(processInstanceId: string): Promise<CloudTask[]> {
        const response = await this.get(`${this.basePath}/process-instances/${processInstanceId}/tasks`);
        return this.unwrapList<CloudTask>(response, 'tasks');
    }

    async getTaskById(taskId: string): Promise<CloudTask> {
        const response = await this.get(`${this.basePath}/tasks/${taskId}`);
        return this.unwrapEntity<CloudTask>(response);
    }

    async tryGetTaskById(taskId: string): Promise<CloudTask | undefined> {
        const response = await this.get(`${this.basePath}/tasks/${taskId}`);
        if (response.httpStatus === 404) {
            return undefined;
        }
        return this.unwrapEntity<CloudTask>(response);
    }

    private assertCreateTaskSuccess(response: RequestResponse, action: string): void {
        if (response.httpStatus && response.httpStatus >= 400) {
            const detail =
                (typeof response.message === 'string' && response.message) ||
                (typeof response.body === 'string' && response.body) ||
                JSON.stringify(response);
            throw new Error(`${action} failed (${response.httpStatus}): ${detail}`);
        }
    }

    async createStandaloneTask(options?: {
        name?: string;
        description?: string;
        assignee?: string;
    }): Promise<CloudTask> {
        const payload = buildCreateTaskPayload({
            name: options?.name ?? 'new-task',
            description: options?.description ?? 'task-description',
            assignee: options?.assignee ?? 'testuser',
        });
        const response = await this.post(`${this.basePath}/tasks`, { data: payload });
        this.assertCreateTaskSuccess(response, 'Create standalone task');
        const task = this.unwrapEntity<CloudTask>(response);
        if (task.id) {
            this.trackCreatedResource(`${this.basePath}/tasks/${task.id}`);
        }
        return task;
    }

    async createUnassignedStandaloneTask(): Promise<CloudTask> {
        const payload = buildCreateTaskPayload({
            name: 'unassigned-task',
            description: 'unassigned-task-description',
        });
        const response = await this.post(`${this.basePath}/tasks`, { data: payload });
        this.assertCreateTaskSuccess(response, 'Create unassigned standalone task');
        const task = this.unwrapEntity<CloudTask>(response);
        if (task.id) {
            this.trackCreatedResource(`${this.basePath}/tasks/${task.id}`);
        }
        return task;
    }

    async createSubtask(parentTaskId: string): Promise<CloudTask> {
        const payload = buildCreateTaskPayload({
            name: 'subtask',
            description: 'subtask-description',
            assignee: 'testuser',
            parentTaskId,
        });
        const response = await this.post(`${this.basePath}/tasks`, { data: payload });
        this.assertCreateTaskSuccess(response, 'Create subtask');
        const task = this.unwrapEntity<CloudTask>(response);
        if (task.id) {
            this.trackCreatedResource(`${this.basePath}/tasks/${task.id}`);
        }
        return task;
    }

    async getSubtasks(parentTaskId: string): Promise<CloudTask[]> {
        const response = await this.get(`${this.basePath}/tasks/${parentTaskId}/subtasks`);
        return this.unwrapList<CloudTask>(response, 'tasks');
    }

    async deleteTask(taskId: string): Promise<void> {
        await this.delete(`${this.basePath}/tasks/${taskId}`);
    }

    async claimTask(taskId: string): Promise<RequestResponse> {
        return this.post(`${this.basePath}/tasks/${taskId}/claim`, { data: {} });
    }

    async completeTask(taskId: string): Promise<RequestResponse> {
        return this.post(`${this.basePath}/tasks/${taskId}/complete`, {
            data: {
                payloadType: 'CompleteTaskPayload',
                taskId,
            },
        });
    }

    async releaseTask(taskId: string): Promise<void> {
        await this.post(`${this.basePath}/tasks/${taskId}/release`, { data: {} });
    }

    async saveTask(taskId: string, variables: Record<string, unknown>): Promise<void> {
        await this.post(`${this.basePath}/tasks/${taskId}/save`, {
            data: {
                payloadType: 'SaveTaskPayload',
                taskId,
                variables,
            },
        });
    }

    async updateTask(
        taskId: string,
        fields: { name?: string; formKey?: string; priority?: number; dueDate?: string }
    ): Promise<CloudTask> {
        const response = await this.put(`${this.basePath}/tasks/${taskId}`, {
            data: {
                payloadType: 'UpdateTaskPayload',
                ...fields,
            },
        });
        return this.unwrapEntity<CloudTask>(response);
    }

    async assignTask(taskId: string, assignee: string): Promise<RequestResponse> {
        return this.post(`${this.basePath}/tasks/${taskId}/assign`, {
            data: {
                payloadType: 'AssignTaskPayload',
                taskId,
                assignee,
            },
        });
    }

    async getTaskVariables(taskId: string): Promise<CloudVariableInstance[]> {
        const response = await this.get(`${this.basePath}/tasks/${taskId}/variables`, {
            headers: { 'Content-Type': 'application/json' },
        });
        return this.unwrapList<CloudVariableInstance>(response, 'variables');
    }

    async updateTaskVariable(taskId: string, name: string, value: unknown): Promise<void> {
        await this.put(`${this.basePath}/tasks/${taskId}/variables/${encodeURIComponent(name)}`, {
            data: {
                payloadType: 'UpdateTaskVariablePayload',
                taskId,
                name,
                value,
            },
        });
    }

    async createTaskVariable(taskId: string, name: string, value: unknown): Promise<void> {
        await this.post(`${this.basePath}/tasks/${taskId}/variables`, {
            data: {
                payloadType: 'CreateTaskVariablePayload',
                taskId,
                name,
                value,
            },
        });
    }

    async completeTaskWithVariables(taskId: string, variables: Record<string, unknown>): Promise<RequestResponse> {
        return this.post(`${this.basePath}/tasks/${taskId}/complete`, {
            data: {
                payloadType: 'CompleteTaskPayload',
                taskId,
                variables,
            },
        });
    }

    filterByStandalone(tasks: CloudTask[], standalone: boolean): CloudTask[] {
        return tasks.filter((task) => Boolean(task.standalone) === standalone);
    }

    async isTaskNotFoundInRuntime(taskId: string): Promise<boolean> {
        const task = await this.tryGetTaskById(taskId);
        return task === undefined;
    }
}
