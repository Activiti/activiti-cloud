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

export interface CloudTask {
    id: string;
    name?: string;
    description?: string;
    assignee?: string;
    owner?: string;
    status: TaskStatus;
    priority?: number;
    createdDate: string;
    dueDate?: string;
    completedDate?: string;
    processInstanceId?: string;
    processDefinitionId?: string;
    processDefinitionKey?: string;
    serviceName?: string;
    serviceFullName?: string;
    appName?: string;
    appVersion?: string;
}

export enum TaskStatus {
    CREATED = 'CREATED',
    ASSIGNED = 'ASSIGNED',
    SUSPENDED = 'SUSPENDED',
    COMPLETED = 'COMPLETED',
    CANCELLED = 'CANCELLED'
}

export interface TaskQueryParams {
    status?: TaskStatus;
    assignee?: string;
    owner?: string;
    processInstanceId?: string;
    processDefinitionKey?: string;
    name?: string;
}

export interface TaskResponse {
    content: CloudTask[];
}

export interface SingleTaskResponse {
    content: CloudTask;
}
