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
import { CustomAPIRequest } from '../context.models';

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
}
