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
import { pickScenarioTest, type AcceptanceScenarioMeta } from '../../helpers/acceptance-scenario';
import { startCatalogProcess } from '../../flows/start-process-with-first-task';

const QUERY_ADMIN_DELETE_ISSUE = 'https://hyland.atlassian.net/browse/AAE-47783';

const auditScenario: AcceptanceScenarioMeta = {
    title: 'delete records in audit service',
};

const queryTasksScenario: AcceptanceScenarioMeta = {
    title: 'delete all tasks in query service',
    exclude: QUERY_ADMIN_DELETE_ISSUE,
};

const queryProcessInstancesScenario: AcceptanceScenarioMeta = {
    title: 'delete all process instances in query service',
    exclude: QUERY_ADMIN_DELETE_ISSUE,
};

activiti.describe('Runtime — Delete Actions', { tag: ['@slow', '@destructive'] }, () => {
    activiti.describe.configure({ mode: 'serial' });

    pickScenarioTest(activiti, auditScenario)(auditScenario.title, async ({
        runtimeBundleServiceTestUser,
        auditAdminServiceTestAdmin,
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

        await activiti.step('And audit service has synced events for the process', async () => {
            const events = await auditAdminServiceTestAdmin.waitForAllEventsAdminCountGreaterThan(0);
            expect(events.length).toBeGreaterThan(0);
        });

        await activiti.step(
            'And another user is authenticated as testadmin ' +
                'Then the user is able to delete all events in audit service',
            async () => {
                const before = await auditAdminServiceTestAdmin.adminEvents.getAllEvents();
                expect(before.length).toBeGreaterThan(0);
                await auditAdminServiceTestAdmin.adminEvents.deleteAllEvents();
                const after = await auditAdminServiceTestAdmin.waitForAllEventsAdminCount(0);
                expect(after.length).toBe(0);
            }
        );
    });

    pickScenarioTest(activiti, queryTasksScenario)(queryTasksScenario.title, async ({
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

        await activiti.step('And query service has synced tasks', async () => {
            const tasks = await queryAdminServiceTestAdmin.waitForAllTasksAdminCountGreaterThan(0);
            expect(tasks.length).toBeGreaterThan(0);
        });

        await activiti.step(
            'And another user is authenticated as testadmin ' +
                'Then the user is able to delete all tasks in query service',
            async () => {
                const before = await queryAdminServiceTestAdmin.adminTasks.getAllTasks();
                expect(before.length).toBeGreaterThan(0);
                await queryAdminServiceTestAdmin.adminTasks.deleteAllTasks();
                const after = await queryAdminServiceTestAdmin.waitForAllTasksAdminCount(0);
                expect(after.length).toBe(0);
            }
        );
    });

    pickScenarioTest(activiti, queryProcessInstancesScenario)(queryProcessInstancesScenario.title, async ({
        runtimeBundleServiceTestUser,
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

        await activiti.step('And query service has synced process instances', async () => {
            const instances = await queryAdminServiceTestAdmin.waitForAllProcessInstancesAdminCountGreaterThan(0);
            expect(instances.length).toBeGreaterThan(0);
        });

        await activiti.step(
            'And another user is authenticated as testadmin ' +
                'Then the user is able to delete all process instances in query service',
            async () => {
                const before = await queryAdminServiceTestAdmin.adminProcessInstances.getAllProcessInstances();
                expect(before.length).toBeGreaterThan(0);
                await queryAdminServiceTestAdmin.adminProcessInstances.deleteAllProcessInstances();
                const after = await queryAdminServiceTestAdmin.waitForAllProcessInstancesAdminCount(0);
                expect(after.length).toBe(0);
            }
        );
    });
});
