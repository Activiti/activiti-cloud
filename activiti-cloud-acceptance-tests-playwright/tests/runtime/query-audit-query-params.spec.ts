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
import { EventType } from '../../models/audit.models';
import { ProcessInstanceStatus } from '../../models/runtime-bundle.models';
import { TaskStatus } from '../../models/task.models';

const TASK_PAGE = { skipCount: 0, maxItems: 10, sort: ['createdDate,desc', 'id,desc'] };
const PROCESS_PAGE = { skipCount: 0, maxItems: 10, sort: ['startDate,desc', 'id,desc'] };

activiti.describe('Runtime — Query and Audit Query Parameters', () => {
    activiti('should exercise pagination and filter query parameters on query and audit APIs', async ({
        runtimeBundleServiceTestUser,
        taskServiceTestUser,
        queryServiceTestUser,
        queryAdminServiceTestAdmin,
        auditServiceTestUser,
        auditAdminServiceTestAdmin,
    }) => {
        let processInstanceId = '';
        let taskId = '';

        await activiti.step('Given a synced process instance with a task and audit events', async () => {
            const { processInstance, task } = await startCatalogProcessWithFirstTask(
                runtimeBundleServiceTestUser,
                taskServiceTestUser,
                'PROCESS_INSTANCE_WITH_SINGLE_TASK_ASSIGNED'
            );
            processInstanceId = processInstance.id;
            taskId = task.id;
            await queryServiceTestUser.waitForProcessInstanceSynced(processInstanceId);
            await queryServiceTestUser.waitForTaskById(taskId, () => true);
            await auditServiceTestUser.waitForEventOfTypeForProcessInstance(
                processInstanceId,
                EventType.PROCESS_STARTED
            );
        });

        await activiti.step('When the user searches tasks with pagination query parameters', async () => {
            const tasks = await queryServiceTestUser.searchTasks({ id: [taskId] }, TASK_PAGE);
            expect(tasks.map((task) => task.id)).toContain(taskId);
            const count = await queryServiceTestUser.countTasks({ id: [taskId] }, TASK_PAGE);
            expect(count).toBe(1);
        });

        await activiti.step('And searches process instances with pagination query parameters', async () => {
            const instances = await queryServiceTestUser.searchProcessInstances(
                { id: [processInstanceId] },
                PROCESS_PAGE
            );
            expect(instances.map((instance) => instance.id)).toContain(processInstanceId);
            const count = await queryServiceTestUser.countProcessInstances(
                { id: [processInstanceId] },
                PROCESS_PAGE
            );
            expect(count).toBeGreaterThanOrEqual(1);
        });

        await activiti.step('When the user lists tasks filtered by process instance id', async () => {
            const tasks = await queryServiceTestUser.getTasks({ processInstanceId });
            expect(tasks.map((task) => task.id)).toContain(taskId);
        });

        await activiti.step('When the admin lists and searches tasks with query parameters', async () => {
            const listed = await queryAdminServiceTestAdmin.getTasksAdminFiltered({
                processInstanceId,
                status: TaskStatus.ASSIGNED,
                id: taskId,
                skipCount: 0,
                maxItems: 10,
                sort: ['createdDate,desc'],
            });
            expect(listed.map((task) => task.id)).toContain(taskId);

            const searched = await queryAdminServiceTestAdmin.searchTasksAdmin({ id: [taskId] }, TASK_PAGE);
            expect(searched.map((task) => task.id)).toContain(taskId);
            const count = await queryAdminServiceTestAdmin.countTasksAdmin({ id: [taskId] }, TASK_PAGE);
            expect(count).toBe(1);
        });

        await activiti.step('And the admin lists and searches process instances with query parameters', async () => {
            const listed = await queryAdminServiceTestAdmin.getProcessInstancesAdminFiltered({
                status: ProcessInstanceStatus.RUNNING,
                skipCount: 0,
                maxItems: 10,
            });
            expect(listed.map((instance) => instance.id)).toContain(processInstanceId);

            const searched = await queryAdminServiceTestAdmin.searchProcessInstancesAdmin(
                { id: [processInstanceId] },
                PROCESS_PAGE
            );
            expect(searched.map((instance) => instance.id)).toContain(processInstanceId);
            const count = await queryAdminServiceTestAdmin.countProcessInstancesAdmin(
                { id: [processInstanceId] },
                PROCESS_PAGE
            );
            expect(count).toBeGreaterThanOrEqual(1);
        });

        await activiti.step('Then audit events can be listed with filters and pagination', async () => {
            const userEvents = await auditServiceTestUser.getEvents(
                { processInstanceId, eventType: EventType.PROCESS_STARTED },
                { skipCount: 0, maxItems: 5, sort: ['timestamp,desc'] }
            );
            expect(userEvents.length).toBeGreaterThan(0);
            expect(userEvents.every((event) => event.processInstanceId === processInstanceId)).toBe(true);

            const adminEvents = await auditAdminServiceTestAdmin.getEventsAdmin(
                { processInstanceId, eventType: EventType.PROCESS_STARTED },
                { skipCount: 0, maxItems: 5, sort: ['timestamp,desc'] }
            );
            expect(adminEvents.length).toBeGreaterThan(0);
        });
    });
});
