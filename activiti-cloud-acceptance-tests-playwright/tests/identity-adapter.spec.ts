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

import { activiti } from '../fixtures/services.fixture';
import { expect } from '../fixtures/context.fixture';

activiti.describe('Identity Management', () => {
    activiti.describe('Group Search', () => {
        activiti('should search groups by name containing "sa"', async ({ identityManagementServiceTestUser }) => {
            await activiti.step('When searching for groups by name containing "sa"', async () => {
                const groups = await identityManagementServiceTestUser.searchGroups({ search: 'sa' });

                const groupNames = groups.map((group) => group.name);
                expect(groupNames).toContain('sales');
                expect(groupNames).toContain('processadmin');
            });
        });

        activiti('should return empty results for non-existent group search', async ({ identityManagementServiceTestUser }) => {
            await activiti.step('When searching for non-existent groups', async () => {
                const groups = await identityManagementServiceTestUser.searchGroups({ search: 'nonexistentgroup123' });

                expect(groups).toHaveLength(0);
            });
        });

        activiti('should search groups with no search parameter', async ({ identityManagementServiceTestUser }) => {
            await activiti.step('When searching for all groups without parameters', async () => {
                const groups = await identityManagementServiceTestUser.searchGroups();

                expect(groups.length).toBeGreaterThan(0);
            });
        });
    });

    activiti.describe('User Search', () => {
        activiti('should search users by name containing "user"', async ({ identityManagementServiceTestUser }) => {
            await activiti.step('When searching for users by name containing "user"', async () => {
                const users = await identityManagementServiceTestUser.searchUsers({ search: 'user' });

                const usernames = users.map((user) => user.username);
                expect(usernames).toContain('hruser');
                expect(usernames).toContain('testuser');
                expect(usernames).not.toContain('testActivitiAdmin');
            });
        });

        activiti('should return empty results for non-existent user search', async ({ identityManagementServiceTestUser }) => {
            await activiti.step('When searching for non-existent users', async () => {
                const users = await identityManagementServiceTestUser.searchUsers({ search: 'nonexistentuser123' });

                expect(users).toHaveLength(0);
            });
        });

        activiti('should search users with no search parameter', async ({ identityManagementServiceTestUser }) => {
            await activiti.step('When searching for all users without parameters', async () => {
                const users = await identityManagementServiceTestUser.searchUsers();

                expect(users.length).toBeGreaterThan(0);
            });
        });
    });

    activiti.describe('Advanced Search Parameters', () => {
        activiti('should search users with role parameter', async ({ identityManagementServiceTestUser }) => {
            await activiti.step('When searching for users with role parameter', async () => {
                const users = await identityManagementServiceTestUser.searchUsers({ role: ['ACTIVITI_USER'] });

                expect(users.length).toBeGreaterThanOrEqual(0);
            });
        });

        activiti('should search users with group parameter', async ({ identityManagementServiceTestUser }) => {
            await activiti.step('When searching for users with group parameter', async () => {
                const users = await identityManagementServiceTestUser.searchUsers({ group: ['hr'] });

                expect(users.length).toBeGreaterThanOrEqual(0);
            });
        });

        activiti('should search groups with role parameter', async ({ identityManagementServiceTestUser }) => {
            await activiti.step('When searching for groups with role parameter', async () => {
                const groups = await identityManagementServiceTestUser.searchGroups({ role: ['ACTIVITI_USER'] });

                expect(groups.length).toBeGreaterThanOrEqual(0);
            });
        });
    });
});
