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
import { EventType } from '../../models/audit.models';
import { ProcessInstanceStatus } from '../../models/runtime-bundle.models';

const MI_PARALLEL_USER_TASKS_PROCESS = 'miParallelUserTasksAllOutputCollection';
const MI_SERVICE_TASK_PROCESS = 'Process_B-f96qb1';
const MI_SERVICE_TASK_ELEMENT_ID = 'ServiceTask_0qp1237';

activiti.describe('Runtime — Multi-Instance Actions', () => {
    activiti('collect all the variables from a multi-instantiated user task', async ({
        runtimeBundleServiceTestUser,
        taskServiceTestUser,
        queryServiceTestUser,
    }) => {
        let processInstanceId: string;

        await activiti.step(
            `When the user starts an instance of the process with key ${MI_PARALLEL_USER_TASKS_PROCESS}`,
            async () => {
                const instance = await runtimeBundleServiceTestUser.startProcess({
                    processDefinitionKey: MI_PARALLEL_USER_TASKS_PROCESS,
                });
                processInstanceId = instance.id;
                expect(processInstanceId).toBeTruthy();
            }
        );

        await activiti.step(
            'And the user completes the task available in the current process instance passing the following variables: meal=pizza, size=large',
            async () => {
                const task = await taskServiceTestUser.waitForOpenTaskByProcessInstanceId(processInstanceId);
                await taskServiceTestUser.completeTaskWithVariables(task.id, { meal: 'pizza', size: 'large' });
            }
        );

        await activiti.step(
            'And the user completes the task available in the current process instance passing the following variables: meal=pasta, size=medium',
            async () => {
                const task = await taskServiceTestUser.waitForOpenTaskByProcessInstanceId(processInstanceId);
                await taskServiceTestUser.completeTaskWithVariables(task.id, { meal: 'pasta', size: 'medium' });
            }
        );

        await activiti.step('Then the process instance reaches a task named Wait', async () => {
            const task = await queryServiceTestUser.waitForTaskByName(processInstanceId, 'Wait');
            expect(task.name).toBe('Wait');
        });

        await activiti.step(
            'And the process instance has a resultCollection named miResult with entries of size 3 as following: meal=pizza/large/testuser, meal=pasta/medium/testuser',
            async () => {
                const miResult = await queryServiceTestUser.waitForVariable(
                    processInstanceId,
                    'miResult',
                    (v) => Array.isArray(v.value) && v.value.length >= 2
                );
                expect(miResult.value).toEqual(
                    expect.arrayContaining([
                        expect.objectContaining({ meal: 'pizza', size: 'large', sys_task_assignee: 'testuser' }),
                        expect.objectContaining({ meal: 'pasta', size: 'medium', sys_task_assignee: 'testuser' }),
                    ])
                );
            }
        );
    });

    activiti('execute multi-instance service task and validate its status in the audit log', async ({
        runtimeBundleServiceTestUser,
        queryServiceTestUser,
        auditServiceTestUser,
    }) => {
        let processInstanceId: string;

        await activiti.step(
            `When the user starts an instance of the process with key ${MI_SERVICE_TASK_PROCESS}`,
            async () => {
                const instance = await runtimeBundleServiceTestUser.startProcess({
                    processDefinitionKey: MI_SERVICE_TASK_PROCESS,
                });
                processInstanceId = instance.id;
                expect(processInstanceId).toBeTruthy();
            }
        );

        await activiti.step(
            `Then the generated ACTIVITY_COMPLETED events for activity ${MI_SERVICE_TASK_ELEMENT_ID} have the expected count of 3`,
            async () => {
                const events = await auditServiceTestUser.waitForEventsCount(
                    processInstanceId,
                    (e) =>
                        e.eventType === EventType.ACTIVITY_COMPLETED &&
                        (e.entity as { elementId?: string })?.elementId === MI_SERVICE_TASK_ELEMENT_ID,
                    3,
                    `3 ACTIVITY_COMPLETED events for ${MI_SERVICE_TASK_ELEMENT_ID}`
                );
                expect(events.length).toBeGreaterThanOrEqual(3);
            }
        );

        await activiti.step('And the process with service tasks is completed', async () => {
            const instance = await queryServiceTestUser.waitForProcessInstanceStatus(
                processInstanceId,
                ProcessInstanceStatus.COMPLETED
            );
            expect(instance.status).toBe(ProcessInstanceStatus.COMPLETED);
        });
    });
});
