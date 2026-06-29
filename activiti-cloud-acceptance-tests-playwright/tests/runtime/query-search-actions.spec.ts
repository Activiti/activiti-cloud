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

activiti.describe('Runtime — Query Search Actions', () => {
    activiti('should search and count process instances and tasks via POST', async ({
        runtimeBundleServiceTestUser,
        taskServiceTestUser,
        queryServiceTestUser,
    }) => {
        let processInstanceId = '';
        let taskId = '';

        await activiti.step('Given a running process instance with a task synced to query', async () => {
            const { processInstance, task } = await startCatalogProcessWithFirstTask(
                runtimeBundleServiceTestUser,
                taskServiceTestUser,
                'PROCESS_INSTANCE_WITH_SINGLE_TASK_ASSIGNED'
            );
            processInstanceId = processInstance.id;
            taskId = task.id;
            await queryServiceTestUser.waitForProcessInstanceSynced(processInstanceId);
            await queryServiceTestUser.waitForTaskById(taskId, () => true);
        });

        await activiti.step('When the user searches process instances by id', async () => {
            const processInstances = await queryServiceTestUser.searchProcessInstances({
                id: [processInstanceId],
            });
            expect(processInstances.map((instance) => instance.id)).toContain(processInstanceId);
        });

        await activiti.step('Then the process instance count matches', async () => {
            const count = await queryServiceTestUser.countProcessInstances({ id: [processInstanceId] });
            expect(count).toBeGreaterThanOrEqual(1);
        });

        await activiti.step('When the user searches tasks by id', async () => {
            const tasks = await queryServiceTestUser.searchTasks({
                id: [taskId],
            });
            expect(tasks.map((task) => task.id)).toContain(taskId);
        });

        await activiti.step('Then the task count matches', async () => {
            const count = await queryServiceTestUser.countTasks({
                id: [taskId],
            });
            expect(count).toBe(1);
        });
    });
});
