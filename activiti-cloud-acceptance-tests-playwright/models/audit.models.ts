/*
 * Copyright 2017-2026 Alfresco Software, Ltd.
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

export interface CloudRuntimeEvent {
    id: string;
    timestamp: string;
    eventType: string;
    entityId?: string;
    processInstanceId?: string;
    processDefinitionId?: string;
    processDefinitionKey?: string;
    businessKey?: string;
    serviceName?: string;
    serviceFullName?: string;
    appName?: string;
    appVersion?: string;
    entity?: unknown;
    sequenceNumber?: number;
    messageId?: string;
    [key: string]: unknown;
}

export interface EventsResponse {
    content: CloudRuntimeEvent[];
}

export interface EventQueryParams {
    entityId?: string;
    processInstanceId?: string;
    processDefinitionKey?: string;
    eventType?: string;
}

export enum EventType {
    PROCESS_STARTED = 'PROCESS_STARTED',
    PROCESS_COMPLETED = 'PROCESS_COMPLETED',
    PROCESS_CANCELLED = 'PROCESS_CANCELLED',
    ACTIVITY_STARTED = 'ACTIVITY_STARTED',
    ACTIVITY_COMPLETED = 'ACTIVITY_COMPLETED',
    TASK_CREATED = 'TASK_CREATED',
    TASK_ASSIGNED = 'TASK_ASSIGNED',
    TASK_COMPLETED = 'TASK_COMPLETED'
}
