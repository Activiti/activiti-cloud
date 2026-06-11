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
 * Tagged @destructive — runs last (project destructive-last, after notifications).
 * Serenity story file remains until a separate retirement ticket removes it.
 */

import { activiti, expect } from '../../fixtures/services.fixture';
import { pickScenarioTest, type AcceptanceScenarioMeta } from '../../helpers/acceptance-scenario';
import { pollOptions } from '../../config/runtime/timeouts';
import { startCatalogProcess } from '../../flows/start-catalog-process';

type DeleteScenario = AcceptanceScenarioMeta & { id: 'audit' | 'query' };

const scenarios: DeleteScenario[] = [
    { id: 'audit', title: 'delete records in audit service' },
    {
        id: 'query',
        title: 'delete records in query service',
        // FIXME upstream: DELETE /query/admin/v1/tasks → 500 when Jackson serializes TaskEntity
        // lazy collections after the persistence context closes. Re-enable when query-service fixes bulk delete.
        exclude:
            'upstream DELETE /query/admin/v1/tasks returns 500 (Cannot lazily initialize collection on JSON response)',
    },
];

activiti.describe('Runtime — Delete Actions', { tag: ['@slow', '@destructive'] }, () => {
    activiti.describe.configure({ mode: 'serial' });

    for (const scenario of scenarios) {
        pickScenarioTest(activiti, scenario)(scenario.title, async ({
            runtimeBundleServiceTestUser,
            taskServiceTestUser,
            auditAdminServiceTestAdmin,
            queryAdminServiceTestAdmin,
        }) => {
            if (scenario.id === 'audit') {
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
                        await expect
                            .poll(
                                async () => (await auditAdminServiceTestAdmin.getAllEventsAdmin()).length,
                                pollOptions('auditEvents')
                            )
                            .toBe(0);
                    }
                );
                return;
            }

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
                    await expect
                        .poll(
                            async () => (await queryAdminServiceTestAdmin.getAllTasksAdmin()).length,
                            pollOptions('querySync')
                        )
                        .toBe(0);
                }
            );

            await activiti.step('And the user is able to delete all process instances in query service', async () => {
                const before = await queryAdminServiceTestAdmin.getAllProcessInstancesAdmin();
                expect(before.length).toBeGreaterThan(0);
                await queryAdminServiceTestAdmin.deleteAllProcessInstancesAdmin();
                await expect
                    .poll(
                        async () => (await queryAdminServiceTestAdmin.getAllProcessInstancesAdmin()).length,
                        pollOptions('querySync')
                    )
                    .toBe(0);
            });
        });
    }
});
