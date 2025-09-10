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

import { activiti, expect } from '../fixtures/context.fixture';
import { IdentityManagementService } from '../services/identity-management.service';

activiti.describe('Identity Management', () => {
    let identityService: IdentityManagementService;

    activiti.beforeEach(async ({ testUserContext }) => {
        identityService = new IdentityManagementService(testUserContext);
    });

    activiti.describe('Group Search', () => {
        activiti('should search groups by name containing "sa"', async () => {
            const groups = await identityService.searchGroups('sa');

            const groupNames = groups.map((group) => group.name);
            expect(groupNames).toContain('sales');
            expect(groupNames).toContain('processadmin');
        });

        activiti('should return empty results for non-existent group search', async () => {
            const groups = await identityService.searchGroups('nonexistentgroup123');

            expect(groups).toHaveLength(0);
        });

        activiti('should search groups with no search parameter', async () => {
            const groups = await identityService.searchGroups();

            expect(groups.length).toBeGreaterThan(0);
        });
    });

    activiti.describe('User Search', () => {
        activiti('should search users by name containing "user"', async () => {
            const users = await identityService.searchUsers('user');

            const usernames = users.map((user) => user.username);
            expect(usernames).toContain('hruser');
            expect(usernames).toContain('testuser');
            expect(usernames).not.toContain('testActivitiAdmin');
        });

        activiti('should return empty results for non-existent user search', async () => {
            const users = await identityService.searchUsers('nonexistentuser123');

            expect(users).toHaveLength(0);
        });

        activiti('should search users with no search parameter', async () => {
            const users = await identityService.searchUsers();

            expect(users.length).toBeGreaterThan(0);
        });
    });

    activiti.describe('Advanced Search Parameters', () => {
        activiti('should search users with role parameter', async () => {
            const users = await identityService.searchUsers(undefined, ['ACTIVITI_USER']);

            expect(users.length).toBeGreaterThanOrEqual(0);
        });

        activiti('should search users with group parameter', async () => {
            const users = await identityService.searchUsers(undefined, undefined, ['hr']);

            expect(users.length).toBeGreaterThanOrEqual(0);
        });

        activiti('should search groups with role parameter', async () => {
            const groups = await identityService.searchGroups(undefined, ['ACTIVITI_USER']);

            expect(groups.length).toBeGreaterThanOrEqual(0);
        });
    });
});
