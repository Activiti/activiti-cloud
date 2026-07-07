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
import { pickScenarioTest, type AcceptanceScenarioMeta } from '../../helpers/acceptance-scenario';
import { startCatalogProcessWithFirstTask } from '../../flows/start-process-with-first-task';

const QUERY_ADMIN_POST_ISSUE = 'https://hyland.atlassian.net/browse/AAE-47783';

const postTasksScenario: AcceptanceScenarioMeta = {
    title: 'POST /query/admin/v1/tasks list endpoint',
    exclude: QUERY_ADMIN_POST_ISSUE,
};

const postProcessInstancesScenario: AcceptanceScenarioMeta = {
    title: 'POST /query/admin/v1/process-instances list endpoint',
    exclude: QUERY_ADMIN_POST_ISSUE,
};

activiti.describe('Runtime — Query Admin Remaining Actions', () => {
    activiti('should cover query admin process instance tasks GET', async ({
        runtimeBundleServiceTestUser,
        taskServiceTestUser,
        queryAdminServiceTestAdmin,
    }) => {
        let processInstanceId = '';
        let taskId = '';

        await activiti.step('Given a process instance with a task synced to query', async () => {
            const { processInstance, task } = await startCatalogProcessWithFirstTask(
                runtimeBundleServiceTestUser,
                taskServiceTestUser,
                'PROCESS_INSTANCE_WITH_SINGLE_TASK_ASSIGNED'
            );
            processInstanceId = processInstance.id;
            taskId = task.id;
            await queryAdminServiceTestAdmin.waitForProcessInstanceAdminSynced(processInstanceId);
            await queryAdminServiceTestAdmin.waitForTaskAdminSynced(taskId);
        });

        await activiti.step('Then the admin lists tasks for the process instance', async () => {
            const tasks = await queryAdminServiceTestAdmin.adminProcessInstances.getTasksByProcessInstanceId(processInstanceId);
            expect(tasks.map((task) => task.id)).toContain(taskId);
        });
    });

    pickScenarioTest(activiti, postTasksScenario)(postTasksScenario.title, async ({
        runtimeBundleServiceTestUser,
        taskServiceTestUser,
        queryAdminServiceTestAdmin,
    }) => {
        let taskId = '';

        await activiti.step('Given a process instance with a task synced to query', async () => {
            const { processInstance, task } = await startCatalogProcessWithFirstTask(
                runtimeBundleServiceTestUser,
                taskServiceTestUser,
                'PROCESS_INSTANCE_WITH_SINGLE_TASK_ASSIGNED'
            );
            await queryAdminServiceTestAdmin.waitForProcessInstanceAdminSynced(processInstance.id);
            await queryAdminServiceTestAdmin.waitForTaskAdminSynced(task.id);
            taskId = task.id;
        });

        await activiti.step('When the admin lists tasks via POST /query/admin/v1/tasks', async () => {
            const tasks = await queryAdminServiceTestAdmin.adminTasks.postTasksListQuery({
                standalone: false,
                rootTasksOnly: false,
            });
            expect(tasks.map((task) => task.id)).toContain(taskId);
        });
    });

    pickScenarioTest(activiti, postProcessInstancesScenario)(postProcessInstancesScenario.title, async ({
        runtimeBundleServiceTestUser,
        taskServiceTestUser,
        queryAdminServiceTestAdmin,
    }) => {
        let processInstanceId = '';

        await activiti.step('Given a process instance with a task synced to query', async () => {
            const { processInstance, task } = await startCatalogProcessWithFirstTask(
                runtimeBundleServiceTestUser,
                taskServiceTestUser,
                'PROCESS_INSTANCE_WITH_SINGLE_TASK_ASSIGNED'
            );
            processInstanceId = processInstance.id;
            await queryAdminServiceTestAdmin.waitForProcessInstanceAdminSynced(processInstanceId);
            await queryAdminServiceTestAdmin.waitForTaskAdminSynced(task.id);
        });

        await activiti.step('When the admin lists process instances via POST /query/admin/v1/process-instances', async () => {
            const instances = await queryAdminServiceTestAdmin.adminProcessInstances.postProcessInstancesListQuery();
            expect(instances.map((instance) => instance.id)).toContain(processInstanceId);
        });
    });
});

activiti.describe('Runtime — Process Instance User Message Actions', () => {
    activiti('should cover RB user start and receive message endpoints', async ({
        runtimeBundleServiceTestUser,
    }) => {
        const businessId = randomUUID();
        let processInstanceId = '';

        await activiti.step('When the user sends a start message', async () => {
            const processInstance = await runtimeBundleServiceTestUser.processInstances.sendStartMessage({
                name: 'startMessage',
                businessKey: businessId,
            });
            processInstanceId = processInstance.id;
            expect(processInstanceId).toBeTruthy();
        });

        await activiti.step('Then the user can deliver boundary and catch messages', async () => {
            const boundaryResponse = await runtimeBundleServiceTestUser.processInstances.sendReceiveMessage({
                name: 'boundaryMessage',
                correlationKey: businessId,
            });
            expect(boundaryResponse.httpStatus).toBeLessThan(300);

            const catchResponse = await runtimeBundleServiceTestUser.processInstances.sendReceiveMessage({
                name: 'catchMessage',
                correlationKey: businessId,
            });
            expect(catchResponse.httpStatus).toBeLessThan(300);
        });
    });
});
