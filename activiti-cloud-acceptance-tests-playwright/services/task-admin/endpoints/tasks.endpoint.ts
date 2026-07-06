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

import { CloudTask } from '../../../models/task.models';
import { CloudVariableInstance } from '../../../models/process-variable.models';
import { CustomAPIRequest } from '../../../fixtures/context.models';
import { BaseService, RequestResponse } from '../../base.service';
import { RB_ADMIN_V1_BASE } from '../../runtime-bundle/endpoints/rb-base-path';
import { candidateNamesFromList } from '../../runtime-bundle/shared/candidate-names';

export class RbAdminTasksEndpoint extends BaseService {
    private readonly basePath = RB_ADMIN_V1_BASE;

    constructor(context: CustomAPIRequest) {
        super(context);
    }

    async getAllTasks(): Promise<CloudTask[]> {
        const response = await this.get(`${this.basePath}/tasks`);
        return this.unwrapList<CloudTask>(response, 'tasks');
    }

    async getTaskById(taskId: string): Promise<CloudTask> {
        const response = await this.get(`${this.basePath}/tasks/${taskId}`);
        return this.unwrapEntity<CloudTask>(response);
    }

    async assignTask(taskId: string, assignee: string): Promise<CloudTask> {
        const response = await this.post(`${this.basePath}/tasks/${taskId}/assign`, {
            data: {
                payloadType: 'AssignTaskPayload',
                taskId,
                assignee,
            },
        });
        return this.unwrapEntity<CloudTask>(response);
    }

    async assignTasks(taskIds: string[], assignee: string): Promise<CloudTask[]> {
        const response = await this.post(`${this.basePath}/tasks/assign`, {
            data: {
                payloadType: 'AssignTasksPayload',
                taskIds,
                assignee,
            },
        });
        return this.unwrapList<CloudTask>(response, 'tasks');
    }

    async getCandidateUsers(taskId: string): Promise<string[]> {
        const response = await this.get(`${this.basePath}/tasks/${taskId}/candidate-users`);
        return candidateNamesFromList(this.unwrapList<Record<string, unknown>>(response, 'list'), 'user');
    }

    async getCandidateGroups(taskId: string): Promise<string[]> {
        const response = await this.get(`${this.basePath}/tasks/${taskId}/candidate-groups`);
        return candidateNamesFromList(this.unwrapList<Record<string, unknown>>(response, 'list'), 'group');
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

    async completeTask(taskId: string): Promise<void> {
        await this.post(`${this.basePath}/tasks/${taskId}/complete`, {
            data: {
                payloadType: 'CompleteTaskPayload',
                taskId,
            },
        });
    }

    async deleteTask(taskId: string): Promise<void> {
        await this.delete(`${this.basePath}/tasks/${taskId}`);
    }

    async updateTask(
        taskId: string,
        fields: { name?: string; formKey?: string; priority?: number; dueDate?: string }
    ): Promise<void> {
        await this.put(`${this.basePath}/tasks/${taskId}`, {
            data: {
                payloadType: 'UpdateTaskPayload',
                ...fields,
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

    async getTaskVariables(taskId: string): Promise<CloudVariableInstance[]> {
        const response = await this.get(`${this.basePath}/tasks/${taskId}/variables`, {
            headers: { 'Content-Type': 'application/json' },
        });
        return this.unwrapList<CloudVariableInstance>(response, 'variables');
    }
}
