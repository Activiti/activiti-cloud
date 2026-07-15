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

import { randomUUID } from 'node:crypto';

import { activiti, expect } from '../../fixtures/services.fixture';
import { startCatalogProcess, startCatalogProcessWithFirstTask } from '../../flows/start-process-with-first-task';

activiti.describe('Runtime — Delete Actions', { tag: ['@slow', '@destructive'] }, () => {
    activiti.describe.configure({ mode: 'serial' });

    activiti('delete records in audit service', async ({
        runtimeBundleServiceTestUser,
        auditAdminServiceTestAdmin,
    }) => {
        await activiti.step('When the testuser starts an instance of the process called PROCESS_INSTANCE_WITH_SINGLE_TASK_ASSIGNED',
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
            'And another user is authenticated as testadmin the user is able to delete all events in audit service',
            async () => {
                const before = await auditAdminServiceTestAdmin.adminEvents.getAllEvents();
                expect(before.length).toBeGreaterThan(0);
                await auditAdminServiceTestAdmin.adminEvents.deleteAllEvents();
                const after = await auditAdminServiceTestAdmin.waitForAllEventsAdminCount(0);
                expect(after.length).toBe(0);
            }
        );
    });

    activiti('delete all tasks in query service', async ({
        runtimeBundleServiceTestUser,
        taskServiceTestUser,
        queryAdminServiceTestAdmin,
    }) => {
        let processTaskId = '';
        let standaloneTaskId = '';

        await activiti.step(
            'Given a process task and a standalone task synced to query',
            async () => {
                const { processInstance, task } = await startCatalogProcessWithFirstTask(
                    runtimeBundleServiceTestUser,
                    taskServiceTestUser,
                    'PROCESS_INSTANCE_WITH_SINGLE_TASK_ASSIGNED'
                );
                processTaskId = task.id;
                await queryAdminServiceTestAdmin.waitForProcessInstanceAdminSynced(processInstance.id);
                await queryAdminServiceTestAdmin.waitForTaskAdminSynced(processTaskId);

                const standalone = await taskServiceTestUser.createStandaloneTask({
                    name: `delete-actions-standalone-${randomUUID()}`,
                });
                standaloneTaskId = standalone.id;
                await queryAdminServiceTestAdmin.waitForTaskAdminSynced(standaloneTaskId);
            }
        );

        await activiti.step(
            'And another user is authenticated as testadmin the user is able to delete all tasks in query service',
            async () => {
                const before = await queryAdminServiceTestAdmin.adminTasks.getAllTasks();
                expect(before.map((task) => task.id)).toEqual(
                    expect.arrayContaining([processTaskId, standaloneTaskId])
                );
                await queryAdminServiceTestAdmin.adminTasks.deleteAllTasks();
                const after = await queryAdminServiceTestAdmin.waitForAllTasksAdminCount(0);
                expect(after.length).toBe(0);
            }
        );
    });

    activiti('delete all process instances in query service', async ({
        runtimeBundleServiceTestUser,
        queryAdminServiceTestAdmin,
    }) => {
        let processInstanceId = '';

        await activiti.step('When the testuser starts an instance of the process called PROCESS_INSTANCE_WITH_SINGLE_TASK_ASSIGNED',
            async () => {
                const processInstance = await startCatalogProcess(
                    runtimeBundleServiceTestUser,
                    'PROCESS_INSTANCE_WITH_SINGLE_TASK_ASSIGNED'
                );
                processInstanceId = processInstance.id;
                expect(processInstanceId).toBeTruthy();
            }
        );

        await activiti.step('And query service has synced the process instance', async () => {
            await queryAdminServiceTestAdmin.waitForProcessInstanceAdminSynced(processInstanceId);
            const instances = await queryAdminServiceTestAdmin.adminProcessInstances.getAllProcessInstances();
            expect(instances.map((instance) => instance.id)).toContain(processInstanceId);
        });

        await activiti.step(
            'And another user is authenticated as testadmin the user is able to delete all process instances in query service',
            async () => {
                const before = await queryAdminServiceTestAdmin.adminProcessInstances.getAllProcessInstances();
                expect(before.map((instance) => instance.id)).toContain(processInstanceId);
                await queryAdminServiceTestAdmin.adminProcessInstances.deleteAllProcessInstances();
                const after = await queryAdminServiceTestAdmin.waitForAllProcessInstancesAdminCount(0);
                expect(after.length).toBe(0);
            }
        );
    });
});
