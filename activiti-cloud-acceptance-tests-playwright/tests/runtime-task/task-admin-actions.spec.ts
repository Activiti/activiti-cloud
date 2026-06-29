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

activiti.describe('Runtime — Task Admin Actions', () => {
    activiti('should list, fetch, and assign tasks via RB admin endpoints', async ({
        runtimeBundleServiceTestUser,
        taskServiceTestUser,
        taskAdminServiceTestAdmin,
    }) => {
        let taskId = '';

        await activiti.step('Given a running process with an unassigned task', async () => {
            const { task } = await startCatalogProcessWithFirstTask(
                runtimeBundleServiceTestUser,
                taskServiceTestUser,
                'PROCESS_INSTANCE_WITH_SINGLE_TASK_AND_USER_CANDIDATES'
            );
            taskId = task.id;
            expect(taskId).toBeTruthy();
        });

        await activiti.step('When the admin lists tasks', async () => {
            const tasks = await taskAdminServiceTestAdmin.getAllTasks();
            expect(tasks.map((task) => task.id)).toContain(taskId);
        });

        await activiti.step('And fetches the task by id', async () => {
            const task = await taskAdminServiceTestAdmin.getTaskById(taskId);
            expect(task.id).toBe(taskId);
        });

        await activiti.step('Then the admin assigns the task to hruser', async () => {
            const assigned = await taskAdminServiceTestAdmin.assignTask(taskId, 'hruser');
            expect(assigned.assignee).toBe('hruser');
        });

        await activiti.step('And bulk-assigns the same task via POST /tasks/assign', async () => {
            const assignedTasks = await taskAdminServiceTestAdmin.assignTasks([taskId], 'testuser');
            expect(assignedTasks.map((task) => task.id)).toContain(taskId);
            expect(assignedTasks.find((task) => task.id === taskId)?.assignee).toBe('testuser');
        });
    });
});
