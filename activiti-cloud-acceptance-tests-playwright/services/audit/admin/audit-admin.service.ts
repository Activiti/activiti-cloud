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

import { CloudRuntimeEvent, EventQueryParams } from '../../../models/audit.models';
import { BaseService } from '../../base.service';
import { CustomAPIRequest } from '../../../fixtures/context.models';
import { HttpStatusCheck, SearchPageParams } from '../../../models/base-service.models';
import { AUDIT_ADMIN_V1_BASE, AuditAdminEventsEndpoint } from './endpoints/index';

export class AuditAdminService extends BaseService {
    private readonly eventsEndpoint: AuditAdminEventsEndpoint;

    constructor(context: CustomAPIRequest) {
        super(context);
        this.eventsEndpoint = new AuditAdminEventsEndpoint(context);
    }

    async getAllEventsAdmin(): Promise<CloudRuntimeEvent[]> {
        return this.eventsEndpoint.getAllEventsAdmin();
    }

    async getEventsByEntityIdAdmin(entityId: string): Promise<CloudRuntimeEvent[]> {
        const byProcessInstance = await this.getEventsAdmin({ processInstanceId: entityId });
        if (byProcessInstance.length > 0) {
            return byProcessInstance;
        }

        const searchParams = new URLSearchParams();
        searchParams.append('search', `entityId:${entityId},processInstanceId:${entityId}`);
        const response = await this.get(`${AUDIT_ADMIN_V1_BASE}/events?${searchParams.toString()}`);
        return this.unwrapList<CloudRuntimeEvent>(response, 'events');
    }

    async getEventsAdmin(params?: EventQueryParams, page?: SearchPageParams): Promise<CloudRuntimeEvent[]> {
        return this.eventsEndpoint.getEventsAdmin(params, page);
    }

    async exportEvents(fileName: string, from: string, to: string): Promise<string> {
        return this.eventsEndpoint.exportEvents(fileName, from, to);
    }

    async deleteAllEventsAdmin(): Promise<CloudRuntimeEvent[]> {
        return this.eventsEndpoint.deleteAllEventsAdmin();
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

    buildUnauthenticatedGetStatusChecks(): readonly HttpStatusCheck<AuditAdminService>[] {
        return [
            BaseService.getStatusCheck<AuditAdminService>('events list', `${AUDIT_ADMIN_V1_BASE}/events`),
            BaseService.getStatusCheck<AuditAdminService>(
                'events export',
                `${AUDIT_ADMIN_V1_BASE}/events/export/pw-unauth-export.csv?from=2020-01-01&to=2020-01-31`
            ),
        ];
    }

    buildBadRequestGetStatusChecks(): readonly HttpStatusCheck<AuditAdminService>[] {
        return [
            BaseService.getStatusCheck<AuditAdminService>(
                'events export with invalid dates',
                `${AUDIT_ADMIN_V1_BASE}/events/export/pw-invalid-export.csv?from=not-a-date&to=also-invalid`
            ),
        ];
    }

    buildForbiddenGetStatusChecks(): readonly HttpStatusCheck<AuditAdminService>[] {
        return [
            BaseService.getStatusCheck<AuditAdminService>('events list', `${AUDIT_ADMIN_V1_BASE}/events`),
            BaseService.getStatusCheck<AuditAdminService>(
                'events export',
                `${AUDIT_ADMIN_V1_BASE}/events/export/pw-forbidden-export.csv?from=2020-01-01&to=2020-01-31`
            ),
        ];
    }
}
