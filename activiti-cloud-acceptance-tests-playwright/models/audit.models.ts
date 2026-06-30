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

export enum AuditEventsDeletionStatus {
    IDLE = 'IDLE',
    RUNNING = 'RUNNING',
    COMPLETED = 'COMPLETED',
    CANCELLED = 'CANCELLED',
    FAILED = 'FAILED',
}

export interface AuditEventsDeletionStatusResponse {
    [key: string]: unknown;
    status: AuditEventsDeletionStatus;
    deletedCount: number;
    remainingCount: number;
    totalCount: number;
    percentComplete: number;
}

export interface AuditEventsDeletionStartResponse extends AuditEventsDeletionStatusResponse {
    message: string;
}

export interface AuditEventsDeletionCancelResponse extends AuditEventsDeletionStatusResponse {
    message: string;
}

export enum EventType {
    PROCESS_STARTED = 'PROCESS_STARTED',
    PROCESS_COMPLETED = 'PROCESS_COMPLETED',
    PROCESS_CANCELLED = 'PROCESS_CANCELLED',
    ACTIVITY_STARTED = 'ACTIVITY_STARTED',
    ACTIVITY_COMPLETED = 'ACTIVITY_COMPLETED',
    TASK_CREATED = 'TASK_CREATED',
    TASK_ASSIGNED = 'TASK_ASSIGNED',
    TASK_COMPLETED = 'TASK_COMPLETED',
    INTEGRATION_ERROR_RECEIVED = 'INTEGRATION_ERROR_RECEIVED',
    INTEGRATION_REQUESTED = 'INTEGRATION_REQUESTED',
    INTEGRATION_RESULT_RECEIVED = 'INTEGRATION_RESULT_RECEIVED',
    ERROR_RECEIVED = 'ERROR_RECEIVED',
    MESSAGE_RECEIVED = 'MESSAGE_RECEIVED',
    MESSAGE_WAITING = 'MESSAGE_WAITING',
    MESSAGE_SENT = 'MESSAGE_SENT',
    SIGNAL_RECEIVED = 'SIGNAL_RECEIVED',
    TIMER_SCHEDULED = 'TIMER_SCHEDULED',
    TIMER_FIRED = 'TIMER_FIRED',
    TIMER_EXECUTED = 'TIMER_EXECUTED',
    TASK_UPDATED = 'TASK_UPDATED',
    APPLICATION_DEPLOYED = 'APPLICATION_DEPLOYED',
    VARIABLE_CREATED = 'VARIABLE_CREATED',
    VARIABLE_UPDATED = 'VARIABLE_UPDATED',
    VARIABLE_DELETED = 'VARIABLE_DELETED'
}
