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
import { startCatalogProcess } from '../../flows/start-catalog-process';
import { startCatalogProcessWithFirstTask } from '../../flows/start-process-with-first-task';

const TASK_VARIABLES: Record<string, unknown> = {
    var1: 'one',
    var2: 2,
};

activiti.describe('Runtime — Query Admin Task Actions', () => {
    activiti('should query admin tasks via POST search, count, and GET detail endpoints', async ({
        runtimeBundleServiceTestUser,
        taskServiceTestUser,
        queryAdminServiceTestAdmin,
    }) => {
        let groupTaskId = '';
        let userTaskId = '';
        let variableTaskId = '';

        await activiti.step('Given process tasks with candidates synced to query admin', async () => {
            const groupProcess = await startCatalogProcessWithFirstTask(
                runtimeBundleServiceTestUser,
                taskServiceTestUser,
                'PROCESS_INSTANCE_WITH_SINGLE_TASK_AND_GROUP_CANDIDATES'
            );
            groupTaskId = groupProcess.task.id;

            const userProcess = await startCatalogProcessWithFirstTask(
                runtimeBundleServiceTestUser,
                taskServiceTestUser,
                'PROCESS_INSTANCE_WITH_SINGLE_TASK_AND_USER_CANDIDATES'
            );
            userTaskId = userProcess.task.id;

            const variableProcess = await startCatalogProcess(
                runtimeBundleServiceTestUser,
                'PROCESS_INSTANCE_WITH_VARIABLES',
                { variables: { start1: 'value1', start2: 'value2' } }
            );
            const variableProcessTask = await taskServiceTestUser.getFirstTaskByProcessInstanceId(
                variableProcess.id
            );
            variableTaskId = variableProcessTask.id;

            await queryAdminServiceTestAdmin.waitForTaskAdminSynced(groupTaskId);
            await queryAdminServiceTestAdmin.waitForTaskAdminSynced(userTaskId);
            await queryAdminServiceTestAdmin.waitForTaskAdminSynced(variableTaskId);
        });

        await activiti.step('When the admin lists tasks via GET', async () => {
            const tasks = await queryAdminServiceTestAdmin.getAllTasksAdmin();
            expect(tasks.length).toBeGreaterThan(0);
        });

        await activiti.step('And lists tasks with process variable keys via GET', async () => {
            const tasks = await queryAdminServiceTestAdmin.getTasksAdminWithVariableKeys(
                'ProcessWithVariables/start1'
            );
            expect(Array.isArray(tasks)).toBe(true);
        });

        await activiti.step('And searches tasks by id via POST', async () => {
            const tasks = await queryAdminServiceTestAdmin.searchTasksAdmin({ id: [groupTaskId] });
            expect(tasks.map((task) => task.id)).toContain(groupTaskId);
        });

        await activiti.step('Then the admin task count matches', async () => {
            const count = await queryAdminServiceTestAdmin.countTasksAdmin({ id: [groupTaskId] });
            expect(count).toBe(1);
        });

        await activiti.step('When the admin fetches task details and candidates', async () => {
            const groupTask = await queryAdminServiceTestAdmin.getTaskAdminById(groupTaskId);
            expect(groupTask.id).toBe(groupTaskId);

            const groups = await queryAdminServiceTestAdmin.getTaskCandidateGroupsAdmin(groupTaskId);
            expect(groups.length).toBeGreaterThan(0);

            const users = await queryAdminServiceTestAdmin.getTaskCandidateUsersAdmin(userTaskId);
            expect(users.length).toBeGreaterThan(0);
        });

        await activiti.step('Then process-scoped task variables are readable via admin API', async () => {
            const variables = await queryAdminServiceTestAdmin.getTaskVariablesAdmin(variableTaskId);
            expect(Array.isArray(variables)).toBe(true);
        });
    });

    activiti('should read standalone task variables via admin API', async ({
        taskServiceTestUser,
        queryAdminServiceTestAdmin,
    }) => {
        let taskId = '';

        await activiti.step('Given a standalone task with variables synced to query', async () => {
            const task = await taskServiceTestUser.createStandaloneTask();
            taskId = task.id;
            for (const [name, value] of Object.entries(TASK_VARIABLES)) {
                await taskServiceTestUser.createTaskVariable(taskId, name, value);
            }
            await queryAdminServiceTestAdmin.waitForTaskAdminSynced(taskId);
        });

        await activiti.step('When the admin reads task variables', async () => {
            const variables = await queryAdminServiceTestAdmin.waitForTaskVariablesAdmin(taskId, TASK_VARIABLES);
            const values = Object.fromEntries(variables.map((variable) => [variable.name, variable.value]));
            expect(values).toEqual(TASK_VARIABLES);
        });
    });
});
