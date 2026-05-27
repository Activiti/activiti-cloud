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

    async getEventsAdmin(params?: EventQueryParams): Promise<CloudRuntimeEvent[]> {
        const searchParams = new URLSearchParams();

        if (params?.entityId) searchParams.append('entityId', params.entityId);
        if (params?.processInstanceId) searchParams.append('processInstanceId', params.processInstanceId);
        if (params?.processDefinitionKey) searchParams.append('processDefinitionKey', params.processDefinitionKey);
        if (params?.eventType) searchParams.append('eventType', params.eventType);

        const response = await this.get(
            `${this.basePath}/events?${searchParams.toString()}`
        );

        return this.unwrapList<CloudRuntimeEvent>(response, 'events');
    }
}
