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

/**
 * Port of delete-actions.story.disabled (AAE-46640).
 * Admin bulk-delete wipes query/audit data for the whole preview namespace — not parallel-safe.
 * Tagged @destructive — runs last in the default Playwright project order (project destructive-last).
 * Query scenario stays skipped: DELETE /query/admin/v1/tasks returns 500 upstream (lazy-init in TaskDeleteController).
 * Serenity story file remains until query bulk-delete is fixed and the scenario is enabled.
 */

import { activiti, expect } from '../../fixtures/services.fixture';
import { pollOptions } from '../../config/runtime/timeouts';
import { startCatalogProcess } from '../../flows/start-catalog-process';

activiti.describe('Runtime — Delete Actions', { tag: ['@slow', '@destructive'] }, () => {
    activiti.describe.configure({ mode: 'serial' });

    activiti(
        'delete records in audit service',
        async ({ runtimeBundleServiceTestUser, auditAdminServiceTestAdmin }) => {
            await activiti.step(
                'Given the user is authenticated as testuser ' +
                    'When the user starts an instance of the process called PROCESS_INSTANCE_WITH_SINGLE_TASK_ASSIGNED',
                async () => {
                    const processInstance = await startCatalogProcess(
                        runtimeBundleServiceTestUser,
                        'PROCESS_INSTANCE_WITH_SINGLE_TASK_ASSIGNED'
                    );
                    expect(processInstance.id).toBeTruthy();
                }
            );

            await activiti.step('And audit service has synced events for the process', async () => {
                await expect
                    .poll(
                        async () => (await auditAdminServiceTestAdmin.getAllEventsAdmin()).length,
                        pollOptions('querySync')
                    )
                    .toBeGreaterThan(0);
            });

            await activiti.step(
                'And another user is authenticated as testadmin ' +
                    'Then the user is able to delete all events in audit service',
                async () => {
                    const before = await auditAdminServiceTestAdmin.getAllEventsAdmin();
                    expect(before.length).toBeGreaterThan(0);
                    await auditAdminServiceTestAdmin.deleteAllEventsAdmin();
                    const after = await auditAdminServiceTestAdmin.getAllEventsAdmin();
                    expect(after).toHaveLength(0);
                }
            );
        }
    );

    // FIXME upstream: DELETE /admin/v1/tasks → 500 "Cannot lazily initialize collection (no session)".
    // Same reason delete-actions.story stayed disabled in Serenity; enable when query service is fixed.
    activiti.skip('delete records in query service', async ({
            runtimeBundleServiceTestUser,
            taskServiceTestUser,
            queryAdminServiceTestAdmin,
        }) => {
            await activiti.step(
                'Given the user is authenticated as testuser ' +
                    'When the user starts an instance of the process called PROCESS_INSTANCE_WITH_SINGLE_TASK_ASSIGNED',
                async () => {
                    const processInstance = await startCatalogProcess(
                        runtimeBundleServiceTestUser,
                        'PROCESS_INSTANCE_WITH_SINGLE_TASK_ASSIGNED'
                    );
                    expect(processInstance.id).toBeTruthy();
                }
            );

            await activiti.step('And the user creates a standalone task', async () => {
                const task = await taskServiceTestUser.createStandaloneTask();
                expect(task.id).toBeTruthy();
            });

            await activiti.step('And query service has synced process instances and tasks', async () => {
                await expect
                    .poll(
                        async () => (await queryAdminServiceTestAdmin.getAllProcessInstancesAdmin()).length,
                        pollOptions('querySync')
                    )
                    .toBeGreaterThan(0);
                await expect
                    .poll(
                        async () => (await queryAdminServiceTestAdmin.getAllTasksAdmin()).length,
                        pollOptions('querySync')
                    )
                    .toBeGreaterThan(0);
            });

            await activiti.step(
                'And another user is authenticated as testadmin ' +
                    'Then the user is able to delete all tasks in query service',
                async () => {
                    const before = await queryAdminServiceTestAdmin.getAllTasksAdmin();
                    expect(before.length).toBeGreaterThan(0);
                    await queryAdminServiceTestAdmin.deleteAllTasksAdmin();
                    const after = await queryAdminServiceTestAdmin.getAllTasksAdmin();
                    expect(after).toHaveLength(0);
                }
            );

            await activiti.step('And the user is able to delete all process instances in query service', async () => {
                const before = await queryAdminServiceTestAdmin.getAllProcessInstancesAdmin();
                expect(before.length).toBeGreaterThan(0);
                await queryAdminServiceTestAdmin.deleteAllProcessInstancesAdmin();
                const after = await queryAdminServiceTestAdmin.getAllProcessInstancesAdmin();
                expect(after).toHaveLength(0);
            });
        }
    );
});
