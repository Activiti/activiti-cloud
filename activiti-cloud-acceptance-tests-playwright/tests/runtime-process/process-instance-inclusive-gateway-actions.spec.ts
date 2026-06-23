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
import { TaskStatus } from '../../models/task.models';

const PROCESS_INSTANCE_WITH_INCLUSIVE_GATEWAY = 'basicInclusiveGateway';

activiti.describe('Process Instance Inclusive Gateway Actions', { tag: '@slow' }, () => {
    activiti('complete a process instance with inclusive gateway', async ({
        runtimeBundleServiceHrUser,
        taskServiceHrUser,
        queryServiceHrUser,
        auditServiceHrUser,
    }) => {
        let processInstanceId: string;

        await activiti.step(
            'When the user starts a process with inclusive gateway PROCESS_INSTANCE_WITH_INCLUSIVE_GATEWAY and set input variable value to 1',
            async () => {
                const processInstance = await runtimeBundleServiceHrUser.startProcessWithVariables(
                    PROCESS_INSTANCE_WITH_INCLUSIVE_GATEWAY,
                    { input: 1 }
                );
                processInstanceId = processInstance.id;
                expect(processInstanceId).toBeTruthy();
            }
        );

        await activiti.step('Then the task is created Start Process', async () => {
            const task = await taskServiceHrUser.waitForTaskByName(processInstanceId, 'Start Process');
            expect(task.id).toBeTruthy();
        });

        await activiti.step('When the user claims and completes the task Start Process', async () => {
            const found = await taskServiceHrUser.waitForTaskByName(processInstanceId, 'Start Process');
            const taskId = found.id;

            await taskServiceHrUser.claimTask(taskId);
            const claimed = await taskServiceHrUser.waitForTaskStatus(taskId, TaskStatus.ASSIGNED);
            expect(claimed.status).toBe(TaskStatus.ASSIGNED);
            const querClaimed = await queryServiceHrUser.waitForTaskStatus(taskId, TaskStatus.ASSIGNED);
            expect(querClaimed.status).toBe(TaskStatus.ASSIGNED);
            await taskServiceHrUser.completeTask(taskId);
            const completed = await queryServiceHrUser.waitForTaskStatus(taskId, TaskStatus.COMPLETED);
            expect(completed.status).toBe(TaskStatus.COMPLETED);
        });

        await activiti.step(
            'Then events are emitted for the inclusive gateway inclusiveGateway',
            async () => {
                const events = await auditServiceHrUser.waitForActivityEventsForEntity(
                    processInstanceId,
                    'inclusiveGateway',
                    'inclusiveGateway',
                    [EventType.ACTIVITY_STARTED, EventType.ACTIVITY_COMPLETED]
                );
                expect(events.map((event) => event.eventType).sort()).toEqual(
                    expect.arrayContaining([
                        EventType.ACTIVITY_STARTED,
                        EventType.ACTIVITY_COMPLETED,
                    ])
                );
            }
        );

        await activiti.step('Then the user will see 2 tasks', async () => {
            const activeTasks = await taskServiceHrUser.waitForActiveTasksCount(processInstanceId, 2);
            expect(activeTasks).toHaveLength(2);
        });

        await activiti.step('And the task is created Send e-mail', async () => {
            const task = await taskServiceHrUser.waitForTaskByName(processInstanceId, 'Send e-mail');
            expect(task.id).toBeTruthy();
        });

        await activiti.step('And the task is created Check account', async () => {
            const task = await taskServiceHrUser.waitForTaskByName(processInstanceId, 'Check account');
            expect(task.id).toBeTruthy();
        });

        await activiti.step('When the user claims and completes the task Send e-mail', async () => {
            const found = await taskServiceHrUser.waitForTaskByName(processInstanceId, 'Send e-mail');
            const taskId = found.id;

            await taskServiceHrUser.claimTask(taskId);
            const claimed = await taskServiceHrUser.waitForTaskStatus(taskId, TaskStatus.ASSIGNED);
            expect(claimed.status).toBe(TaskStatus.ASSIGNED);
            const querClaimed = await queryServiceHrUser.waitForTaskStatus(taskId, TaskStatus.ASSIGNED);
            expect(querClaimed.status).toBe(TaskStatus.ASSIGNED);
            await taskServiceHrUser.completeTask(taskId);
            const completed = await queryServiceHrUser.waitForTaskStatus(taskId, TaskStatus.COMPLETED);
            expect(completed.status).toBe(TaskStatus.COMPLETED);
        });

        await activiti.step('Then the user will see 1 tasks', async () => {
            const activeTasks = await taskServiceHrUser.waitForActiveTasksCount(processInstanceId, 1);
            expect(activeTasks).toHaveLength(1);
        });

        await activiti.step('And the task is created Check account', async () => {
            const task = await taskServiceHrUser.waitForTaskByName(processInstanceId, 'Check account');
            expect(task.id).toBeTruthy();
        });

        await activiti.step('When the user claims and completes the task Check account', async () => {
            const found = await taskServiceHrUser.waitForTaskByName(processInstanceId, 'Check account');
            const taskId = found.id;

            await taskServiceHrUser.claimTask(taskId);
            const claimed = await taskServiceHrUser.waitForTaskStatus(taskId, TaskStatus.ASSIGNED);
            expect(claimed.status).toBe(TaskStatus.ASSIGNED);
            const querClaimed = await queryServiceHrUser.waitForTaskStatus(taskId, TaskStatus.ASSIGNED);
            expect(querClaimed.status).toBe(TaskStatus.ASSIGNED);
            await taskServiceHrUser.completeTask(taskId);
            const completed = await queryServiceHrUser.waitForTaskStatus(taskId, TaskStatus.COMPLETED);
            expect(completed.status).toBe(TaskStatus.COMPLETED);
        });

        await activiti.step(
            'Then events are emitted for the inclusive gateway inclusiveGatewayEnd',
            async () => {
                const events = await auditServiceHrUser.waitForActivityEventsForEntity(
                    processInstanceId,
                    'inclusiveGatewayEnd',
                    'inclusiveGateway',
                    [EventType.ACTIVITY_STARTED, EventType.ACTIVITY_COMPLETED]
                );
                expect(events.map((event) => event.eventType).sort()).toEqual(
                    expect.arrayContaining([
                        EventType.ACTIVITY_STARTED,
                        EventType.ACTIVITY_COMPLETED,
                    ])
                );
            }
        );

        await activiti.step('Then the process with inclusive gateway is completed', async () => {
            const instance = await queryServiceHrUser.waitForProcessInstanceStatus(
                processInstanceId,
                ProcessInstanceStatus.COMPLETED
            );
            expect(instance.status).toBe(ProcessInstanceStatus.COMPLETED);
        });
    });
});
