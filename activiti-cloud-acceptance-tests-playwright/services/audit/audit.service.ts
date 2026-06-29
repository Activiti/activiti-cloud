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

import { CloudRuntimeEvent, EventType } from '../../models/audit.models';
import { BaseService } from '../base.service';
import { CustomAPIRequest } from '../../fixtures/context.models';
import { HttpStatusCheck } from '../../models/base-service.models';
import { AUDIT_ADMIN_V1_BASE, AUDIT_V1_BASE, AuditEventsEndpoint } from './endpoints/index';

const INTEGRATION_EVENT_TYPES: readonly string[] = [
    EventType.INTEGRATION_REQUESTED,
    EventType.INTEGRATION_RESULT_RECEIVED,
    EventType.INTEGRATION_ERROR_RECEIVED,
];

export class AuditService extends BaseService {
    readonly events: AuditEventsEndpoint;
    readonly adminEvents: AuditEventsEndpoint;

    constructor(context: CustomAPIRequest) {
        super(context);
        this.events = new AuditEventsEndpoint(context, false);
        this.adminEvents = new AuditEventsEndpoint(context, true);
    }

    async getEventsByEntityId(entityId: string): Promise<CloudRuntimeEvent[]> {
        return this.events.getEvents({ entityId });
    }

    async getEventsByEntityIdAdmin(entityId: string): Promise<CloudRuntimeEvent[]> {
        const byProcessInstance = await this.adminEvents.getEvents({ processInstanceId: entityId });
        if (byProcessInstance.length > 0) {
            return byProcessInstance;
        }

        const searchParams = new URLSearchParams();
        searchParams.append('search', `entityId:${entityId},processInstanceId:${entityId}`);
        const response = await this.get(`${AUDIT_ADMIN_V1_BASE}/events?${searchParams.toString()}`);
        return this.unwrapList<CloudRuntimeEvent>(response, 'events');
    }

    async waitForEventOfTypeForEntity(entityId: string, eventType: EventType): Promise<CloudRuntimeEvent> {
        const events = await AuditService.waitFor(
            () => this.getEventsByEntityId(entityId),
            (list) => list.some((e) => e.eventType === eventType),
            'auditEvents',
            `event ${eventType} for entity ${entityId}`
        );
        return events.find((e) => e.eventType === eventType)!;
    }

    async waitForEventOfTypeForProcessInstance(processInstanceId: string, eventType: EventType): Promise<CloudRuntimeEvent> {
        const events = await AuditService.waitFor(
            () => this.events.getEvents({ processInstanceId, eventType }),
            (list) => list.some((event) => event.eventType === eventType && event.processInstanceId === processInstanceId),
            'auditEvents',
            `event ${eventType} for process ${processInstanceId}`
        );
        return events.find((event) => event.eventType === eventType && event.processInstanceId === processInstanceId)!;
    }

    async getEventsByProcessInstanceId(processInstanceId: string): Promise<CloudRuntimeEvent[]> {
        return this.events.getEvents({ processInstanceId });
    }

    async waitForEventsCount(
        processInstanceId: string,
        predicate: (event: CloudRuntimeEvent) => boolean,
        expectedCount: number,
        description?: string
    ): Promise<CloudRuntimeEvent[]> {
        const events = await AuditService.waitFor(
            () => this.getEventsByProcessInstanceId(processInstanceId),
            (list) => list.filter(predicate).length >= expectedCount,
            'auditEvents',
            description ?? `${expectedCount} audit event(s) on process ${processInstanceId}`
        );
        return events.filter(predicate);
    }

    async getIntegrationContextEvents(processInstanceId: string): Promise<CloudRuntimeEvent[]> {
        const events = await this.getEventsByProcessInstanceId(processInstanceId);
        return events.filter((event) => INTEGRATION_EVENT_TYPES.includes(event.eventType));
    }

    async waitForIntegrationContextEventTypes(
        processInstanceId: string,
        expectedTypes: readonly EventType[]
    ): Promise<CloudRuntimeEvent[]> {
        const sortedExpected = [...expectedTypes].sort();
        return AuditService.waitFor(
            () => this.getIntegrationContextEvents(processInstanceId),
            (events) => {
                const actual = events.map((event) => event.eventType).sort();
                if (actual.length !== sortedExpected.length) {
                    return false;
                }
                return actual.every((type, index) => type === sortedExpected[index]);
            },
            'auditEvents',
            `integration context events [${expectedTypes.join(',')}] on process ${processInstanceId}`
        );
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
            const entity = event.entity as { activityType?: string; processInstanceId?: string } | undefined;
            return entity?.activityType === activityType && entity?.processInstanceId === processInstanceId;
        });
    }

    async getActivityEventsByType(processInstanceId: string, activityType: string): Promise<CloudRuntimeEvent[]> {
        const events = await this.getEventsByProcessInstanceId(processInstanceId);
        return events.filter((event) => {
            const entity = event.entity as { activityType?: string; processInstanceId?: string } | undefined;
            return entity?.activityType === activityType && entity?.processInstanceId === processInstanceId;
        });
    }

    async waitForActivityEventsByType(
        processInstanceId: string,
        activityType: string,
        requiredEventTypes: readonly EventType[]
    ): Promise<CloudRuntimeEvent[]> {
        return AuditService.waitFor(
            () => this.getActivityEventsByType(processInstanceId, activityType),
            (events) => {
                const types = new Set(events.map((event) => event.eventType));
                return requiredEventTypes.every((eventType) => types.has(eventType));
            },
            'auditEvents',
            `${activityType} events [${requiredEventTypes.join(',')}] on process ${processInstanceId}`
        );
    }

    async waitForActivityEventsForEntity(
        processInstanceId: string,
        entityId: string,
        activityType: string,
        requiredEventTypes: readonly EventType[]
    ): Promise<CloudRuntimeEvent[]> {
        return AuditService.waitFor(
            () => this.getActivityEventsForEntity(processInstanceId, entityId, activityType),
            (events) => {
                const types = new Set(events.map((event) => event.eventType));
                return requiredEventTypes.every((eventType) => types.has(eventType));
            },
            'auditEvents',
            `${activityType} events for entity ${entityId} [${requiredEventTypes.join(',')}] on process ${processInstanceId}`
        );
    }

    async getEventsByEntityAndType(
        processInstanceId: string,
        entityId: string,
        eventType: EventType
    ): Promise<CloudRuntimeEvent[]> {
        const events = await this.events.getEvents({ processInstanceId, entityId });
        return events.filter(
            (event) =>
                event.eventType === eventType &&
                event.entityId === entityId &&
                event.processInstanceId === processInstanceId
        );
    }

    async getEventTypesByEntityAndDefinitionKey(entityId: string, processDefinitionKey: string): Promise<string[]> {
        const events = await this.getEventsByEntityId(entityId);
        return events
            .filter((event) => (event.processDefinitionId ?? '').startsWith(processDefinitionKey))
            .map((event) => event.eventType);
    }

    async waitForEventsByEntityAndType(
        processInstanceId: string,
        entityId: string,
        eventType: EventType
    ): Promise<CloudRuntimeEvent[]> {
        return AuditService.waitFor(
            () => this.getEventsByEntityAndType(processInstanceId, entityId, eventType),
            (events) => events.length > 0,
            'auditEvents',
            `${eventType} events for entity ${entityId} on process ${processInstanceId}`
        );
    }

    async waitForEventTypesByEntityAndDefinitionKey(
        entityId: string,
        processDefinitionKey: string,
        requiredEventTypes: readonly EventType[]
    ): Promise<string[]> {
        return AuditService.waitFor(
            () => this.getEventTypesByEntityAndDefinitionKey(entityId, processDefinitionKey),
            (types) => requiredEventTypes.every((eventType) => types.includes(eventType)),
            'auditEvents',
            `event types [${requiredEventTypes.join(',')}] for entity ${entityId} on ${processDefinitionKey}`
        );
    }

    async waitForVariableCreatedEvents(
        processInstanceId: string,
        variableNames: readonly string[]
    ): Promise<CloudRuntimeEvent[]> {
        return AuditService.waitFor(
            () => this.events.getEvents({ processInstanceId, eventType: EventType.VARIABLE_CREATED }),
            (events) => {
                const names = new Set(
                    events
                        .map((event) => (event.entity as { name?: string } | undefined)?.name)
                        .filter((name): name is string => typeof name === 'string')
                );
                return variableNames.every((name) => names.has(name));
            },
            'auditEvents',
            `VARIABLE_CREATED events on process ${processInstanceId} for [${variableNames.join(',')}]`
        );
    }

    async waitForEventMatching(
        processInstanceId: string,
        predicate: (event: CloudRuntimeEvent) => boolean,
        description: string
    ): Promise<CloudRuntimeEvent> {
        const events = await AuditService.waitFor(
            () => this.getEventsByProcessInstanceId(processInstanceId),
            (list) => list.some(predicate),
            'auditEvents',
            description
        );
        return events.find(predicate)!;
    }

    async waitForEventsByEntityIdMatching(
        entityId: string,
        predicate: (events: CloudRuntimeEvent[]) => boolean,
        description: string
    ): Promise<CloudRuntimeEvent[]> {
        return AuditService.waitFor(() => this.getEventsByEntityId(entityId), predicate, 'auditEvents', description);
    }

    async waitForEventsByProcessInstanceMatching(
        processInstanceId: string,
        predicate: (events: CloudRuntimeEvent[]) => boolean,
        description: string
    ): Promise<CloudRuntimeEvent[]> {
        return AuditService.waitFor(
            () => this.getEventsByProcessInstanceId(processInstanceId),
            predicate,
            'auditEvents',
            description
        );
    }

    async getMessageEventsForProcessInstance(
        processInstanceId: string,
        eventType: EventType,
        messageName: string
    ): Promise<CloudRuntimeEvent[]> {
        const events = await this.events.getEvents({ processInstanceId, eventType });
        return events.filter((event) => {
            const entity = event.entity as { messagePayload?: { name?: string } } | undefined;
            return (
                event.eventType === eventType &&
                event.processInstanceId === processInstanceId &&
                entity?.messagePayload?.name === messageName
            );
        });
    }

    async waitForMessageEventsForProcessInstance(
        processInstanceId: string,
        eventType: EventType,
        messageName: string
    ): Promise<CloudRuntimeEvent[]> {
        return AuditService.waitFor(
            () => this.getMessageEventsForProcessInstance(processInstanceId, eventType, messageName),
            (events) => events.length > 0,
            'auditEvents',
            `${eventType} events for message ${messageName} on process ${processInstanceId}`
        );
    }

    async getMessageEventsByDefinitionAndBusinessKey(
        processDefinitionKey: string,
        businessKey: string,
        eventType: EventType,
        messageName: string
    ): Promise<CloudRuntimeEvent[]> {
        const events = await this.events.getEvents({ processDefinitionKey });
        return events.filter((event) => {
            if (event.businessKey !== businessKey || event.eventType !== eventType) {
                return false;
            }
            const entity = event.entity as { messagePayload?: { name?: string } } | undefined;
            return entity?.messagePayload?.name === messageName;
        });
    }

    async waitForMessageEventsByDefinitionAndBusinessKey(
        processDefinitionKey: string,
        businessKey: string,
        eventType: EventType,
        messageName: string
    ): Promise<CloudRuntimeEvent[]> {
        return AuditService.waitFor(
            () => this.getMessageEventsByDefinitionAndBusinessKey(processDefinitionKey, businessKey, eventType, messageName),
            (events) => events.length > 0,
            'auditEvents',
            `${eventType} events for message ${messageName} on ${processDefinitionKey} businessKey ${businessKey}`
        );
    }

    async waitForAllEventsAdminCount(expectedCount: number): Promise<CloudRuntimeEvent[]> {
        return AuditService.waitFor(
            () => this.adminEvents.getAllEvents(),
            (events) => events.length === expectedCount,
            'auditEvents',
            `admin events count to equal ${expectedCount}`
        );
    }

    async waitForAllEventsAdminCountGreaterThan(minCount: number): Promise<CloudRuntimeEvent[]> {
        return AuditService.waitFor(
            () => this.adminEvents.getAllEvents(),
            (events) => events.length > minCount,
            'auditEvents',
            `admin events count > ${minCount}`
        );
    }

    async checkServicesHealth(): Promise<void> {
        const response = await this.get('/audit/actuator/health');
        const status = (response as { status?: string }).status;
        if (status !== 'UP') {
            throw new Error(`Audit service health check failed: status=${status}`);
        }
    }

    buildUnauthenticatedGetStatusChecks(): readonly HttpStatusCheck<AuditService>[];
    buildUnauthenticatedGetStatusChecks(fakeResourceId: string): readonly HttpStatusCheck<AuditService>[];
    buildUnauthenticatedGetStatusChecks(fakeResourceId?: string): readonly HttpStatusCheck<AuditService>[] {
        if (fakeResourceId === undefined) {
            return [
                BaseService.getStatusCheck<AuditService>('events list', `${AUDIT_ADMIN_V1_BASE}/events`),
                BaseService.getStatusCheck<AuditService>(
                    'events export',
                    `${AUDIT_ADMIN_V1_BASE}/events/export/pw-unauth-export.csv?from=2020-01-01&to=2020-01-31`
                ),
            ];
        }
        return [
            BaseService.getStatusCheck<AuditService>('events list', `${AUDIT_V1_BASE}/events`),
            BaseService.getStatusCheck<AuditService>('event by id', `${AUDIT_V1_BASE}/events/${encodeURIComponent(fakeResourceId)}`),
        ];
    }

    buildBadRequestGetStatusChecks(): readonly HttpStatusCheck<AuditService>[] {
        return [
            BaseService.getStatusCheck<AuditService>(
                'events export with invalid dates',
                `${AUDIT_ADMIN_V1_BASE}/events/export/pw-invalid-export.csv?from=not-a-date&to=also-invalid`
            ),
        ];
    }

    buildForbiddenGetStatusChecks(): readonly HttpStatusCheck<AuditService>[];
    buildForbiddenGetStatusChecks(eventId: string): readonly HttpStatusCheck<AuditService>[];
    buildForbiddenGetStatusChecks(eventId?: string): readonly HttpStatusCheck<AuditService>[] {
        if (eventId === undefined) {
            return [
                BaseService.getStatusCheck<AuditService>('events list', `${AUDIT_ADMIN_V1_BASE}/events`),
                BaseService.getStatusCheck<AuditService>(
                    'events export',
                    `${AUDIT_ADMIN_V1_BASE}/events/export/pw-forbidden-export.csv?from=2020-01-01&to=2020-01-31`
                ),
            ];
        }
        return [BaseService.getStatusCheck<AuditService>('event by id', `${AUDIT_V1_BASE}/events/${encodeURIComponent(eventId)}`)];
    }
}
