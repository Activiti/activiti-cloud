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

export interface CloudProcessInstance {
    id: string;
    [key: string]: unknown;
    name?: string;
    processDefinitionId: string;
    processDefinitionKey: string;
    processDefinitionVersion?: number;
    status: ProcessInstanceStatus;
    businessKey?: string;
    startDate: string;
    endDate?: string;
    serviceName?: string;
    serviceFullName?: string;
    initiator?: string;
    appName?: string;
    appVersion?: string;
    linkedProcessInstanceId?: string;
    linkedProcessInstanceType?: string;
}

export enum ProcessInstanceStatus {
    CREATED = 'CREATED',
    RUNNING = 'RUNNING',
    SUSPENDED = 'SUSPENDED',
    CANCELLED = 'CANCELLED',
    COMPLETED = 'COMPLETED'
}

export interface StartProcessPayload {
    payloadType: 'StartProcessPayload';
    processDefinitionKey?: string;
    processDefinitionId?: string;
    businessKey?: string;
    name?: string;
    variables?: Record<string, unknown>;
    linkedProcessInstanceId?: string;
    linkedProcessInstanceType?: string;
}

export interface CreateProcessInstancePayload {
    payloadType: 'CreateProcessInstancePayload';
    processDefinitionKey?: string;
    processDefinitionId?: string;
    businessKey?: string;
    name?: string;
    variables?: Record<string, unknown>;
}

export interface SignalPayload {
    payloadType: 'SignalPayload';
    name: string;
    variables?: Record<string, unknown>;
}

export interface UpdateProcessPayload {
    payloadType: 'UpdateProcessPayload';
    name?: string;
}

export interface ProcessInstanceResponse {
    content: CloudProcessInstance;
}

export interface ProcessQueryParams {
    status?: ProcessInstanceStatus;
    processDefinitionKey?: string;
    businessKey?: string;
    name?: string;
}

export interface ProcessInstanceSearchRequest {
    id?: string[];
}

export enum ServiceTaskStatus {
    SCHEDULED = 'SCHEDULED',
    STARTED = 'STARTED',
    COMPLETED = 'COMPLETED',
    ERROR = 'ERROR',
    CANCELLED = 'CANCELLED'
}

export interface CloudServiceTask {
    id: string;
    activityType?: string;
    status?: ServiceTaskStatus;
    integrationContextCounter?: number;
    processDefinitionId?: string;
    processDefinitionKey?: string;
    processInstanceId?: string;
    elementId?: string;
    [key: string]: unknown;
}

export enum IntegrationContextStatus {
    INTEGRATION_REQUESTED = 'INTEGRATION_REQUESTED',
    INTEGRATION_RESULT_RECEIVED = 'INTEGRATION_RESULT_RECEIVED',
    INTEGRATION_ERROR_RECEIVED = 'INTEGRATION_ERROR_RECEIVED'
}

export interface CloudIntegrationContext {
    id?: string;
    clientId?: string;
    clientName?: string;
    clientType?: string;
    status?: IntegrationContextStatus;
    executionId?: string;
    processDefinitionId?: string;
    processInstanceId?: string;
    [key: string]: unknown;
}
