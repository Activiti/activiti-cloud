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

import { CloudRuntimeEvent, EventQueryParams } from '../../../../models/audit.models';
import { SearchPageParams } from '../../../../models/base-service.models';
import { BaseService } from '../../../base.service';
import { CustomAPIRequest } from '../../../../fixtures/context.models';

export const AUDIT_ADMIN_V1_BASE = '/audit/admin/v1';

export class AuditAdminEventsEndpoint extends BaseService {
    constructor(context: CustomAPIRequest) {
        super(context);
    }

    async getAllEventsAdmin(): Promise<CloudRuntimeEvent[]> {
        const response = await this.get(`${AUDIT_ADMIN_V1_BASE}/events`);
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

        const response = await this.get(`${AUDIT_ADMIN_V1_BASE}/events?${searchParams.toString()}`);
        return this.unwrapList<CloudRuntimeEvent>(response, 'events');
    }

    async exportEvents(fileName: string, from: string, to: string): Promise<string> {
        const searchParams = new URLSearchParams({ from, to });
        return this.getText(`${AUDIT_ADMIN_V1_BASE}/events/export/${encodeURIComponent(fileName)}?${searchParams}`);
    }

    async deleteAllEventsAdmin(): Promise<CloudRuntimeEvent[]> {
        const response = await this.delete(`${AUDIT_ADMIN_V1_BASE}/events`);
        return this.unwrapList<CloudRuntimeEvent>(response, 'events');
    }
}
