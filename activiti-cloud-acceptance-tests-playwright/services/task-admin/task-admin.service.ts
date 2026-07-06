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

import { CloudTask } from '../../models/task.models';
import { BaseService, RequestResponse } from '../base.service';
import { CustomAPIRequest } from '../../fixtures/context.models';
import { CloudVariableInstance } from '../../models/process-variable.models';
import { RbAdminTasksEndpoint } from './endpoints/index';

export class TaskAdminService extends BaseService {
    readonly tasks: RbAdminTasksEndpoint;

    constructor(context: CustomAPIRequest) {
        super(context);
        this.tasks = new RbAdminTasksEndpoint(context);
    }

    async getAllTasks(): Promise<CloudTask[]> {
        return this.tasks.getAllTasks();
    }

    async getTaskById(taskId: string): Promise<CloudTask> {
        return this.tasks.getTaskById(taskId);
    }

    async assignTask(taskId: string, assignee: string): Promise<CloudTask> {
        return this.tasks.assignTask(taskId, assignee);
    }

    async assignTasks(taskIds: string[], assignee: string): Promise<CloudTask[]> {
        return this.tasks.assignTasks(taskIds, assignee);
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

    async completeTask(taskId: string): Promise<void> {
        return this.tasks.completeTask(taskId);
    }

    async deleteTask(taskId: string): Promise<void> {
        return this.tasks.deleteTask(taskId);
    }

    async updateTask(
        taskId: string,
        fields: { name?: string; formKey?: string; priority?: number; dueDate?: string }
    ): Promise<void> {
        return this.tasks.updateTask(taskId, fields);
    }

    async createTaskVariable(taskId: string, name: string, value: unknown): Promise<void> {
        return this.tasks.createTaskVariable(taskId, name, value);
    }

    async updateTaskVariable(taskId: string, name: string, value: unknown): Promise<void> {
        return this.tasks.updateTaskVariable(taskId, name, value);
    }

    async getTaskVariables(taskId: string): Promise<CloudVariableInstance[]> {
        return this.tasks.getTaskVariables(taskId);
    }
}
