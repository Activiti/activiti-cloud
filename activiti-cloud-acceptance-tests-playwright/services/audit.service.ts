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

import { CloudRuntimeEvent, EventQueryParams, EventType } from '../models/audit.models';
import { BaseService } from './base.service';
import { CustomAPIRequest } from '../fixtures/context.models';

const INTEGRATION_EVENT_TYPES: readonly string[] = [
    EventType.INTEGRATION_REQUESTED,
    EventType.INTEGRATION_RESULT_RECEIVED,
    EventType.INTEGRATION_ERROR_RECEIVED,
];

function buildAuditSearch(params?: EventQueryParams): string | undefined {
    const parts: string[] = [];
    if (params?.entityId) {
        parts.push(`entityId:${params.entityId}`);
    }
    if (params?.processInstanceId) {
        parts.push(`processInstanceId:${params.processInstanceId}`);
    }
    if (params?.processDefinitionKey) {
        parts.push(`processDefinitionKey:${params.processDefinitionKey}`);
    }
    if (params?.eventType) {
        parts.push(`eventType:${params.eventType}`);
    }
    return parts.length > 0 ? parts.join(',') : undefined;
}

export class AuditService extends BaseService {
    private readonly basePath = '/audit/v1';

    constructor(context: CustomAPIRequest) {
        super(context);
    }

    async getAllEvents(): Promise<CloudRuntimeEvent[]> {
        const response = await this.get(`${this.basePath}/events?sort=timestamp,desc&sort=id,desc`);
        return this.unwrapList<CloudRuntimeEvent>(response, 'events');
    }

    async getEvents(params?: EventQueryParams): Promise<CloudRuntimeEvent[]> {
        const searchParams = new URLSearchParams();
        searchParams.append('sort', 'timestamp,desc');
        searchParams.append('sort', 'id,desc');

        const search = buildAuditSearch(params);
        if (search) {
            searchParams.append('search', search);
        }

        const response = await this.get(`${this.basePath}/events?${searchParams.toString()}`);

        return this.unwrapList<CloudRuntimeEvent>(response, 'events');
    }

    async getEventsByEntityId(entityId: string): Promise<CloudRuntimeEvent[]> {
        return this.getEvents({ entityId });
    }

    async getEventsByProcessInstanceId(processInstanceId: string): Promise<CloudRuntimeEvent[]> {
        return this.getEvents({ processInstanceId });
    }

    async getIntegrationContextEvents(processInstanceId: string): Promise<CloudRuntimeEvent[]> {
        const events = await this.getEventsByProcessInstanceId(processInstanceId);
        return events.filter((event) => INTEGRATION_EVENT_TYPES.includes(event.eventType));
    }

    async getActivityEventsForEntity(
        processInstanceId: string,
        entityId: string,
        activityType: string
    ): Promise<CloudRuntimeEvent[]> {
        const events = await this.getEventsByProcessInstanceId(processInstanceId);
        return events.filter((event) => {
            if (event.entityId !== entityId) {
                return false;
            }
            const entity = event.entity as
                | { activityType?: string; processInstanceId?: string }
                | undefined;
            return (
                entity?.activityType === activityType &&
                entity?.processInstanceId === processInstanceId
            );
        });
    }

    async getActivityEventsByType(
        processInstanceId: string,
        activityType: string
    ): Promise<CloudRuntimeEvent[]> {
        const events = await this.getEventsByProcessInstanceId(processInstanceId);
        return events.filter((event) => {
            const entity = event.entity as
                | { activityType?: string; processInstanceId?: string }
                | undefined;
            return (
                entity?.activityType === activityType &&
                entity?.processInstanceId === processInstanceId
            );
        });
    }

    async getEventsByEntityAndType(
        processInstanceId: string,
        entityId: string,
        eventType: EventType
    ): Promise<CloudRuntimeEvent[]> {
        const events = await this.getEvents({ processInstanceId, entityId });
        return events.filter(
            (event) =>
                event.eventType === eventType &&
                event.entityId === entityId &&
                event.processInstanceId === processInstanceId
        );
    }

    async getEventTypesByEntityAndDefinitionKey(
        entityId: string,
        processDefinitionKey: string
    ): Promise<string[]> {
        const events = await this.getEventsByEntityId(entityId);
        return events
            .filter((event) =>
                (event.processDefinitionId ?? '').startsWith(processDefinitionKey)
            )
            .map((event) => event.eventType);
    }

    async getMessageEventsForProcessInstance(
        processInstanceId: string,
        eventType: EventType,
        messageName: string
    ): Promise<CloudRuntimeEvent[]> {
        const events = await this.getEvents({ processInstanceId, eventType });
        return events.filter((event) => {
            const entity = event.entity as
                | { messagePayload?: { name?: string } }
                | undefined;
            return (
                event.eventType === eventType &&
                event.processInstanceId === processInstanceId &&
                entity?.messagePayload?.name === messageName
            );
        });
    }

    async getMessageEventsByDefinitionAndBusinessKey(
        processDefinitionKey: string,
        businessKey: string,
        eventType: EventType,
        messageName: string
    ): Promise<CloudRuntimeEvent[]> {
        const events = await this.getEvents({ processDefinitionKey });
        return events.filter((event) => {
            if (event.businessKey !== businessKey || event.eventType !== eventType) {
                return false;
            }
            const entity = event.entity as
                | { messagePayload?: { name?: string } }
                | undefined;
            return entity?.messagePayload?.name === messageName;
        });
    }

    async checkServicesHealth(): Promise<void> {
        const response = await this.get('/audit/actuator/health');
        const status = (response as { status?: string }).status;
        if (status !== 'UP') {
            throw new Error(`Audit service health check failed: status=${status}`);
        }
    }

    async getSwaggerSpecification(group: string = 'Audit'): Promise<string> {
        const root = this.basePath.replace(/\/v1$/, '');
        return this.getText(`${root}/v3/api-docs/${encodeURIComponent(group)}`);
    }
}
