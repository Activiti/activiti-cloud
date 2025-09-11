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

import { APIRequestContext, APIResponse } from '@playwright/test';
import { CloudRuntimeEvent, EventQueryParams, EventsResponse } from '../models/audit.models';

export class AuditService {
    private readonly basePath = '/audit/v1';

    constructor(private readonly context: APIRequestContext) {}

    async getAllEvents(): Promise<CloudRuntimeEvent[]> {
        const response: APIResponse = await this.context.get(`${this.basePath}/events`);

        if (!response.ok()) {
            throw new Error(`Failed to get events: ${response.status()} ${response.statusText()}`);
        }

        const result: EventsResponse = await response.json();
        return result.content || [];
    }

    async getEvents(params?: EventQueryParams): Promise<CloudRuntimeEvent[]> {
        const searchParams = new URLSearchParams();

        if (params?.entityId) searchParams.append('entityId', params.entityId);
        if (params?.processInstanceId) searchParams.append('processInstanceId', params.processInstanceId);
        if (params?.processDefinitionKey) searchParams.append('processDefinitionKey', params.processDefinitionKey);
        if (params?.eventType) searchParams.append('eventType', params.eventType);

        const response: APIResponse = await this.context.get(
            `${this.basePath}/events?${searchParams.toString()}`
        );

        if (!response.ok()) {
            throw new Error(`Failed to get events: ${response.status()} ${response.statusText()}`);
        }

        const result: EventsResponse = await response.json();
        return result.content || [];
    }

    async getEventsByEntityId(entityId: string): Promise<CloudRuntimeEvent[]> {
        return this.getEvents({ entityId });
    }

    async getEventsByProcessInstanceId(processInstanceId: string): Promise<CloudRuntimeEvent[]> {
        return this.getEvents({ processInstanceId });
    }
}

export class AuditAdminService {
    private readonly basePath = '/audit/admin/v1';

    constructor(private readonly context: APIRequestContext) {}

    async getAllEventsAdmin(): Promise<CloudRuntimeEvent[]> {
        const response: APIResponse = await this.context.get(`${this.basePath}/events`);

        if (!response.ok()) {
            throw new Error(`Failed to get admin events: ${response.status()} ${response.statusText()}`);
        }

        const result: EventsResponse = await response.json();
        return result.content || [];
    }

    async getEventsByEntityIdAdmin(entityId: string): Promise<CloudRuntimeEvent[]> {
        const searchParams = new URLSearchParams();
        searchParams.append('search', `entityId:${entityId}`);

        const response: APIResponse = await this.context.get(
            `${this.basePath}/events?${searchParams.toString()}`
        );

        if (!response.ok()) {
            throw new Error(`Failed to get admin events by entity ID: ${response.status()} ${response.statusText()}`);
        }

        const result: EventsResponse = await response.json();
        return result.content || [];
    }

    async getEventsAdmin(params?: EventQueryParams): Promise<CloudRuntimeEvent[]> {
        const searchParams = new URLSearchParams();

        if (params?.entityId) searchParams.append('entityId', params.entityId);
        if (params?.processInstanceId) searchParams.append('processInstanceId', params.processInstanceId);
        if (params?.processDefinitionKey) searchParams.append('processDefinitionKey', params.processDefinitionKey);
        if (params?.eventType) searchParams.append('eventType', params.eventType);

        const response: APIResponse = await this.context.get(
            `${this.basePath}/events?${searchParams.toString()}`
        );

        if (!response.ok()) {
            throw new Error(`Failed to get admin events: ${response.status()} ${response.statusText()}`);
        }

        const result: EventsResponse = await response.json();
        return result.content || [];
    }
}
