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

const TASK_VARIABLES: Record<string, unknown> = {
    var1: 'one',
    var2: 2,
};

activiti.describe('Runtime — Task Variable Actions', () => {
    activiti('task variables synchronization', async ({ taskServiceTestUser, queryServiceTestUser }) => {
        let taskId: string;

        await activiti.step('When the user creates a standalone task', async () => {
            const task = await taskServiceTestUser.createStandaloneTask();
            expect(task.id).toBeTruthy();
            taskId = task.id;
        });

        await activiti.step('And the user creates task variables', async () => {
            for (const [name, value] of Object.entries(TASK_VARIABLES)) {
                await taskServiceTestUser.createTaskVariable(taskId, name, value);
            }
        });

        await activiti.step('Then task variables are visible in rb and query', async () => {
            const rbVars = await taskServiceTestUser.waitForTaskVariableValues(taskId, TASK_VARIABLES);
            const queryVars = await queryServiceTestUser.waitForTaskVariableValues(taskId, TASK_VARIABLES);
            const toMap = (list: { name: string; value?: unknown }[]): Record<string, unknown> =>
                Object.fromEntries(list.map((v) => [v.name, v.value]));
            expect({ rb: toMap(rbVars), query: toMap(queryVars) }).toEqual({
                rb: TASK_VARIABLES,
                query: TASK_VARIABLES,
            });
        });
    });
});
