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
import { SearchPageParams } from '../../../models/base-service.models';
import { BaseService } from '../../base.service';
import { CustomAPIRequest } from '../../../fixtures/context.models';

export const AUDIT_V1_BASE = '/audit/v1';

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

export class AuditEventsEndpoint extends BaseService {
    constructor(context: CustomAPIRequest) {
        super(context);
    }

    async getAllEvents(): Promise<CloudRuntimeEvent[]> {
        const response = await this.get(`${AUDIT_V1_BASE}/events?sort=timestamp,desc&sort=id,desc`);
        return this.unwrapList<CloudRuntimeEvent>(response, 'events');
    }

    async getEvents(params?: EventQueryParams, page?: SearchPageParams): Promise<CloudRuntimeEvent[]> {
        const searchParams = new URLSearchParams();
        searchParams.append('sort', 'timestamp,desc');
        searchParams.append('sort', 'id,desc');

        const search = buildAuditSearch(params);
        if (search) {
            searchParams.append('search', search);
        }
        if (page?.skipCount !== undefined) {
            searchParams.set('skipCount', String(page.skipCount));
        }
        if (page?.maxItems !== undefined) {
            searchParams.set('maxItems', String(page.maxItems));
        }

        const response = await this.get(`${AUDIT_V1_BASE}/events?${searchParams.toString()}`);
        return this.unwrapList<CloudRuntimeEvent>(response, 'events');
    }

    async getEventById(eventId: string): Promise<CloudRuntimeEvent> {
        const response = await this.get(`${AUDIT_V1_BASE}/events/${encodeURIComponent(eventId)}`);
        return this.unwrapEntity<CloudRuntimeEvent>(response);
    }

    async getSwaggerSpecification(group: string = 'Audit'): Promise<string> {
        const root = AUDIT_V1_BASE.replace(/\/v1$/, '');
        return this.getText(`${root}/v3/api-docs/${encodeURIComponent(group)}`);
    }
}
