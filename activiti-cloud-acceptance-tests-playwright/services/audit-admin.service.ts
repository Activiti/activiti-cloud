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

import { CloudRuntimeEvent, EventQueryParams } from '../models/audit.models';
import { BaseService } from './base.service';
import { CustomAPIRequest } from '../fixtures/context.models';
import { SearchPageParams, HttpStatusCheck } from '../models/base-service.models';

export class AuditAdminService extends BaseService {
    private readonly basePath = '/audit/admin/v1';

    constructor(context: CustomAPIRequest) {
        super(context);
    }

    async getAllEventsAdmin(): Promise<CloudRuntimeEvent[]> {
        const response = await this.get(`${this.basePath}/events`);
        return this.unwrapList<CloudRuntimeEvent>(response, 'events');
    }

    async getEventsByEntityIdAdmin(entityId: string): Promise<CloudRuntimeEvent[]> {
        const byProcessInstance = await this.getEventsAdmin({ processInstanceId: entityId });
        if (byProcessInstance.length > 0) {
            return byProcessInstance;
        }

        const searchParams = new URLSearchParams();
        searchParams.append('search', `entityId:${entityId},processInstanceId:${entityId}`);

        const response = await this.get(`${this.basePath}/events?${searchParams.toString()}`);

        return this.unwrapList<CloudRuntimeEvent>(response, 'events');
    }

    async getEventsAdmin(params?: EventQueryParams, page?: SearchPageParams): Promise<CloudRuntimeEvent[]> {
        const searchParams = new URLSearchParams();

        if (params?.entityId) searchParams.append('entityId', params.entityId);
        if (params?.processInstanceId) searchParams.append('processInstanceId', params.processInstanceId);
        if (params?.processDefinitionKey) searchParams.append('processDefinitionKey', params.processDefinitionKey);
        if (params?.eventType) searchParams.append('eventType', params.eventType);
        if (page?.skipCount !== undefined) {
            searchParams.set('skipCount', String(page.skipCount));
        }
        if (page?.maxItems !== undefined) {
            searchParams.set('maxItems', String(page.maxItems));
        }
        for (const sort of page?.sort ?? []) {
            searchParams.append('sort', sort);
        }

        const response = await this.get(
            `${this.basePath}/events?${searchParams.toString()}`
        );

        return this.unwrapList<CloudRuntimeEvent>(response, 'events');
    }

    async exportEvents(fileName: string, from: string, to: string): Promise<string> {
        const searchParams = new URLSearchParams({ from, to });
        return this.getText(`${this.basePath}/events/export/${encodeURIComponent(fileName)}?${searchParams}`);
    }

    async deleteAllEventsAdmin(): Promise<CloudRuntimeEvent[]> {
        const response = await this.delete(`${this.basePath}/events`);
        return this.unwrapList<CloudRuntimeEvent>(response, 'events');
    }

    async waitForAllEventsAdminCount(expectedCount: number): Promise<CloudRuntimeEvent[]> {
        return AuditAdminService.waitFor(
            () => this.getAllEventsAdmin(),
            (events) => events.length === expectedCount,
            'auditEvents',
            `admin events count to equal ${expectedCount}`
        );
    }

    async waitForAllEventsAdminCountGreaterThan(minCount: number): Promise<CloudRuntimeEvent[]> {
        return AuditAdminService.waitFor(
            () => this.getAllEventsAdmin(),
            (events) => events.length > minCount,
            'querySync',
            `admin events count > ${minCount}`
        );
    }

    async getAllEventsAdminHttpStatus(): Promise<number> {
        return this.getHttpStatus(`${this.basePath}/events`);
    }

    async getEventsExportHttpStatus(fileName: string, from: string, to: string): Promise<number> {
        const searchParams = new URLSearchParams({ from, to });
        return this.getHttpStatus(
            `${this.basePath}/events/export/${encodeURIComponent(fileName)}?${searchParams}`
        );
    }

    async getEventsExportInvalidDatesHttpStatus(): Promise<number> {
        return this.getEventsExportHttpStatus('pw-invalid-export.csv', 'not-a-date', 'also-invalid');
    }

    async getEventsExportUnauthenticatedHttpStatus(): Promise<number> {
        return this.getEventsExportHttpStatus('pw-unauth-export.csv', '2020-01-01', '2020-01-31');
    }

    async getEventsExportForbiddenHttpStatus(): Promise<number> {
        return this.getEventsExportHttpStatus('pw-forbidden-export.csv', '2020-01-01', '2020-01-31');
    }
}

export type AuditAdminHttpStatusCheck = HttpStatusCheck<AuditAdminService>;

export function buildAuditAdminUnauthenticatedGetStatusChecks(): readonly AuditAdminHttpStatusCheck[] {
    return [
        { label: 'events list', run: (service) => service.getAllEventsAdminHttpStatus() },
        {
            label: 'events export',
            run: (service) => service.getEventsExportUnauthenticatedHttpStatus(),
        },
    ];
}

export function buildAuditAdminBadRequestGetStatusChecks(): readonly AuditAdminHttpStatusCheck[] {
    return [
        {
            label: 'events export with invalid dates',
            run: (service) => service.getEventsExportInvalidDatesHttpStatus(),
        },
    ];
}

export function buildAuditAdminForbiddenGetStatusChecks(): readonly AuditAdminHttpStatusCheck[] {
    return [
        { label: 'events list', run: (service) => service.getAllEventsAdminHttpStatus() },
        {
            label: 'events export',
            run: (service) => service.getEventsExportForbiddenHttpStatus(),
        },
    ];
}
