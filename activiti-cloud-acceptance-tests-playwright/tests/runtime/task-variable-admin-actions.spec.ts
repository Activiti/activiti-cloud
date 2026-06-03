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

activiti.describe('Runtime — Task Variable Admin Actions', () => {
    activiti('create task variable as admin', async ({ taskServiceHradmin, taskAdminServiceHradmin }) => {
        let taskId: string;

        await activiti.step('Given the user creates a standalone task', async () => {
            const task = await taskServiceHradmin.createStandaloneTask();
            expect(task.id).toBeTruthy();
            taskId = task.id;
        });

        await activiti.step('When the user creates, using admin endpoint, a task variable named title with value Mr.', async () => {
            await taskAdminServiceHradmin.createTaskVariable(taskId, 'title', 'Mr.');
        });

        await activiti.step('Then the user is able to retrieve, using the admin endpoint, a variable named title with value Mr.', async () => {
            const variables = await taskAdminServiceHradmin.getTaskVariables(taskId);
            expect(variables.some(v => v.name === 'title' && v.value === 'Mr.')).toBe(true);
        });
    });

    activiti('update task variable as admin', async ({ taskServiceHradmin, taskAdminServiceHradmin }) => {
        let taskId: string;

        await activiti.step('Given the user creates a standalone task', async () => {
            const task = await taskServiceHradmin.createStandaloneTask();
            expect(task.id).toBeTruthy();
            taskId = task.id;
        });

        await activiti.step('And the user creates, using admin endpoint, a task variable named title with value Mr.', async () => {
            await taskAdminServiceHradmin.createTaskVariable(taskId, 'title', 'Mr.');
        });

        await activiti.step('When the user updates, using admin endpoint, the task variable named title with value Dr.', async () => {
            await taskAdminServiceHradmin.updateTaskVariable(taskId, 'title', 'Dr.');
        });

        await activiti.step('Then the user is able to retrieve, using the admin endpoint, a variable named title with value Dr.', async () => {
            const variables = await taskAdminServiceHradmin.getTaskVariables(taskId);
            expect(variables.some(v => v.name === 'title' && v.value === 'Dr.')).toBe(true);
        });
    });
});
