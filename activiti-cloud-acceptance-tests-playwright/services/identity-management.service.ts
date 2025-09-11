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
import { User, Group, SearchUsersParams, SearchGroupsParams } from '../models/identity.models';

export class IdentityManagementService {
    private readonly basePath = '/identity-adapter-service/v1';

    constructor(private readonly context: APIRequestContext) {}

    async searchUsers(options: SearchUsersParams = {}): Promise<User[]> {
        const params = new URLSearchParams();

        if (options.search) params.append('search', options.search);
        if (options.role) options.role.forEach(r => params.append('role', r));
        if (options.group) options.group.forEach(g => params.append('group', g));
        if (options.application) params.append('application', options.application);

        const response: APIResponse = await this.context.get(`${this.basePath}/users?${params.toString()}`);

        if (!response.ok()) {
            throw new Error(`Failed to search users: ${response.status()} ${response.statusText()}`);
        }

        return await response.json();
    }

    async searchGroups(options: SearchGroupsParams = {}): Promise<Group[]> {
        const params = new URLSearchParams();

        if (options.search) params.append('search', options.search);
        if (options.role) options.role.forEach(r => params.append('role', r));
        if (options.application) params.append('application', options.application);

        const response: APIResponse = await this.context.get(`${this.basePath}/groups?${params.toString()}`);

        if (!response.ok()) {
            throw new Error(`Failed to search groups: ${response.status()} ${response.statusText()}`);
        }

        return await response.json();
    }
}
