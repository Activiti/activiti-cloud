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

import { CloudTask, TaskQueryParams } from '../../../models/task.models';
import { CloudVariableInstance } from '../../../models/process-variable.models';
import { CustomAPIRequest } from '../../../fixtures/context.models';
import { BaseService, RequestResponse } from '../../base.service';
import { RB_V1_BASE } from '../../runtime-bundle/endpoints/rb-base-path';

export interface CreateTaskPayload {
    payloadType: 'CreateTaskPayload';
    name: string;
    description?: string;
    assignee?: string;
    parentTaskId?: string;
    priority?: number;
}

export function buildCreateTaskPayload(
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

export class RbTasksEndpoint extends BaseService {
    private readonly basePath = RB_V1_BASE;

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

    async createTask(payload: Record<string, string | number>, action = 'Create task'): Promise<CloudTask> {
        const response = await this.post(`${this.basePath}/tasks`, { data: payload });
        if (response.httpStatus && response.httpStatus >= 400) {
            const detail =
                (typeof response.message === 'string' && response.message) ||
                (typeof response.body === 'string' && response.body) ||
                JSON.stringify(response);
            throw new Error(`${action} failed (${response.httpStatus}): ${detail}`);
        }
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

    async completeTask(taskId: string, variables?: Record<string, unknown>): Promise<RequestResponse> {
        return this.post(`${this.basePath}/tasks/${taskId}/complete`, {
            data: {
                payloadType: 'CompleteTaskPayload',
                taskId,
                ...(variables ? { variables } : {}),
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

    async getCandidateUsers(taskId: string): Promise<string[]> {
        const response = await this.get(`${this.basePath}/tasks/${taskId}/candidate-users`);
        return this.unwrapCandidateNames(response, 'user');
    }

    async getCandidateGroups(taskId: string): Promise<string[]> {
        const response = await this.get(`${this.basePath}/tasks/${taskId}/candidate-groups`);
        return this.unwrapCandidateNames(response, 'group');
    }

    async addCandidateUsers(taskId: string, candidateUsers: string[]): Promise<RequestResponse> {
        return this.post(`${this.basePath}/tasks/${taskId}/candidate-users`, {
            data: {
                payloadType: 'CandidateUsersPayload',
                taskId,
                candidateUsers,
            },
        });
    }

    async deleteCandidateUsers(taskId: string, candidateUsers: string[]): Promise<RequestResponse> {
        return this.delete(`${this.basePath}/tasks/${taskId}/candidate-users`, {
            data: {
                payloadType: 'CandidateUsersPayload',
                taskId,
                candidateUsers,
            },
        });
    }

    async addCandidateGroups(taskId: string, candidateGroups: string[]): Promise<RequestResponse> {
        return this.post(`${this.basePath}/tasks/${taskId}/candidate-groups`, {
            data: {
                payloadType: 'CandidateGroupsPayload',
                taskId,
                candidateGroups,
            },
        });
    }

    async deleteCandidateGroups(taskId: string, candidateGroups: string[]): Promise<RequestResponse> {
        return this.delete(`${this.basePath}/tasks/${taskId}/candidate-groups`, {
            data: {
                payloadType: 'CandidateGroupsPayload',
                taskId,
                candidateGroups,
            },
        });
    }

    async getNextTask(): Promise<CloudTask | undefined> {
        const response = await this.post(`${this.basePath}/tasks/next`, { data: {} });
        if (response.httpStatus === 204) {
            return undefined;
        }
        return this.unwrapEntity<CloudTask>(response);
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

    private unwrapCandidateNames(response: RequestResponse, field: 'user' | 'group'): string[] {
        const items = this.unwrapList<Record<string, unknown>>(response, 'list');
        return items
            .map((item) => item[field])
            .filter((value): value is string => typeof value === 'string');
    }
}
