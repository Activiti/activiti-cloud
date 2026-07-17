/*
 * Copyright 2017-2026 Hyland Software, Inc. and its affiliates.
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

import { activiti, expect } from '../../fixtures/services.fixture';
import { startCatalogProcessWithFirstTask } from '../../flows/start-process-with-first-task';

activiti.describe('Runtime — Task Candidate Actions', () => {
    activiti('should manage RB user and admin candidate users on a process task', async ({
        runtimeBundleServiceTestUser,
        taskServiceTestUser,
        taskAdminServiceTestAdmin,
    }) => {
        let taskId = '';

        await activiti.step('Given a process task with user candidate hruser', async () => {
            const { task } = await startCatalogProcessWithFirstTask(
                runtimeBundleServiceTestUser,
                taskServiceTestUser,
                'PROCESS_INSTANCE_WITH_SINGLE_TASK_AND_USER_CANDIDATES'
            );
            taskId = task.id;
            await taskServiceTestUser.tasks.claimTask(taskId);
        });

        await activiti.step('When the user lists candidate users via RB', async () => {
            const users = await taskServiceTestUser.tasks.getCandidateUsers(taskId);
            expect(users).toContain('hruser');
        });

        await activiti.step('And adds testuser as a candidate user', async () => {
            const response = await taskServiceTestUser.tasks.addCandidateUsers(taskId, ['testuser']);
            expect(response.httpStatus).toBeLessThan(300);
        });

        await activiti.step('Then RB lists hruser and testuser as candidates', async () => {
            const users = await taskServiceTestUser.tasks.getCandidateUsers(taskId);
            expect(users).toEqual(expect.arrayContaining(['hruser', 'testuser']));
        });

        await activiti.step('When the user removes testuser from candidate users', async () => {
            const response = await taskServiceTestUser.tasks.deleteCandidateUsers(taskId, ['testuser']);
            expect(response.httpStatus).toBeLessThan(300);
        });

        await activiti.step('Then only hruser remains as candidate user', async () => {
            const users = await taskServiceTestUser.tasks.getCandidateUsers(taskId);
            expect(users).toContain('hruser');
            expect(users).not.toContain('testuser');
        });

        await activiti.step('When the admin lists and updates candidate users', async () => {
            const adminUsers = await taskAdminServiceTestAdmin.tasks.getCandidateUsers(taskId);
            expect(adminUsers).toContain('hruser');

            const addResponse = await taskAdminServiceTestAdmin.tasks.addCandidateUsers(taskId, ['testuser']);
            expect(addResponse.httpStatus).toBeLessThan(300);

            const updatedUsers = await taskAdminServiceTestAdmin.tasks.getCandidateUsers(taskId);
            expect(updatedUsers).toEqual(expect.arrayContaining(['hruser', 'testuser']));

            const deleteResponse = await taskAdminServiceTestAdmin.tasks.deleteCandidateUsers(taskId, ['testuser']);
            expect(deleteResponse.httpStatus).toBeLessThan(300);
        });

        await activiti.step('Then the admin candidate user list matches RB', async () => {
            const adminUsers = await taskAdminServiceTestAdmin.tasks.getCandidateUsers(taskId);
            const userCandidates = await taskServiceTestUser.tasks.getCandidateUsers(taskId);
            expect(adminUsers).toEqual(expect.arrayContaining(userCandidates));
        });
    });

    activiti('should manage RB user and admin candidate groups on a process task', async ({
        runtimeBundleServiceTestUser,
        taskServiceTestUser,
        taskAdminServiceTestAdmin,
    }) => {
        let taskId = '';

        await activiti.step('Given a process task with group candidates hr and testgroup', async () => {
            const { task } = await startCatalogProcessWithFirstTask(
                runtimeBundleServiceTestUser,
                taskServiceTestUser,
                'PROCESS_INSTANCE_WITH_SINGLE_TASK_AND_GROUP_CANDIDATES'
            );
            taskId = task.id;
            await taskServiceTestUser.tasks.claimTask(taskId);
        });

        await activiti.step('When the user lists candidate groups via RB', async () => {
            const groups = await taskServiceTestUser.tasks.getCandidateGroups(taskId);
            expect(groups).toEqual(expect.arrayContaining(['hr', 'testgroup']));
        });

        await activiti.step('And adds activiti as a candidate group', async () => {
            const response = await taskServiceTestUser.tasks.addCandidateGroups(taskId, ['activiti']);
            expect(response.httpStatus).toBeLessThan(300);
        });

        await activiti.step('Then RB lists activiti among candidate groups', async () => {
            const groups = await taskServiceTestUser.tasks.getCandidateGroups(taskId);
            expect(groups).toContain('activiti');
        });

        await activiti.step('When the user removes activiti from candidate groups', async () => {
            const response = await taskServiceTestUser.tasks.deleteCandidateGroups(taskId, ['activiti']);
            expect(response.httpStatus).toBeLessThan(300);
        });

        await activiti.step('Then hr and testgroup remain as candidate groups', async () => {
            const groups = await taskServiceTestUser.tasks.getCandidateGroups(taskId);
            expect(groups).toEqual(expect.arrayContaining(['hr', 'testgroup']));
            expect(groups).not.toContain('activiti');
        });

        await activiti.step('When the admin lists and updates candidate groups', async () => {
            const adminGroups = await taskAdminServiceTestAdmin.tasks.getCandidateGroups(taskId);
            expect(adminGroups).toEqual(expect.arrayContaining(['hr', 'testgroup']));

            const addResponse = await taskAdminServiceTestAdmin.tasks.addCandidateGroups(taskId, ['activiti']);
            expect(addResponse.httpStatus).toBeLessThan(300);

            const updatedGroups = await taskAdminServiceTestAdmin.tasks.getCandidateGroups(taskId);
            expect(updatedGroups).toContain('activiti');

            const deleteResponse = await taskAdminServiceTestAdmin.tasks.deleteCandidateGroups(taskId, ['activiti']);
            expect(deleteResponse.httpStatus).toBeLessThan(300);
        });

        await activiti.step('Then the admin candidate group list matches RB', async () => {
            const adminGroups = await taskAdminServiceTestAdmin.tasks.getCandidateGroups(taskId);
            const userGroups = await taskServiceTestUser.tasks.getCandidateGroups(taskId);
            expect(adminGroups).toEqual(expect.arrayContaining(userGroups));
        });
    });
});
