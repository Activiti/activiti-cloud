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
import { pollOptions } from '../../config/runtime/timeouts';
import { expectTaskStatusInRbAndQuery } from '../../helpers/task-assertions';
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
            await expect
                .poll(
                    async () =>
                        (await taskServiceHrUser.findTaskByName(processInstanceId, 'Start Process'))?.id,
                    pollOptions('querySync')
                )
                .toBeTruthy();
        });

        await activiti.step('When the user claims and completes the task Start Process', async () => {
            const task = await taskServiceHrUser.findTaskByName(processInstanceId, 'Start Process');
            await taskServiceHrUser.claimTask(task!.id);
            await expectTaskStatusInRbAndQuery(
                taskServiceHrUser,
                queryServiceHrUser,
                task!.id,
                TaskStatus.ASSIGNED
            );
            await taskServiceHrUser.completeTask(task!.id);
            await expectTaskStatusInRbAndQuery(
                taskServiceHrUser,
                queryServiceHrUser,
                task!.id,
                TaskStatus.COMPLETED
            );
        });

        await activiti.step(
            'Then events are emitted for the inclusive gateway inclusiveGateway',
            async () => {
                await expect
                    .poll(
                        async () =>
                            (
                                await auditServiceHrUser.getActivityEventsForEntity(
                                    processInstanceId,
                                    'inclusiveGateway',
                                    'inclusiveGateway'
                                )
                            )
                                .map((event) => event.eventType)
                                .sort(),
                        pollOptions('querySync')
                    )
                    .toEqual(
                        expect.arrayContaining([
                            EventType.ACTIVITY_STARTED,
                            EventType.ACTIVITY_COMPLETED,
                        ])
                    );
            }
        );

        await activiti.step('Then the user will see 2 tasks', async () => {
            await expect
                .poll(async () => {
                    const tasks = await taskServiceHrUser.getTasksByProcessInstanceId(processInstanceId);
                    return tasks.filter((task) => task.status !== TaskStatus.COMPLETED).length;
                }, pollOptions('querySync'))
                .toBe(2);
        });

        await activiti.step('And the task is created Send e-mail', async () => {
            await expect
                .poll(
                    async () =>
                        (await taskServiceHrUser.findTaskByName(processInstanceId, 'Send e-mail'))?.id,
                    pollOptions('querySync')
                )
                .toBeTruthy();
        });

        await activiti.step('And the task is created Check account', async () => {
            await expect
                .poll(
                    async () =>
                        (await taskServiceHrUser.findTaskByName(processInstanceId, 'Check account'))?.id,
                    pollOptions('querySync')
                )
                .toBeTruthy();
        });

        await activiti.step('When the user claims and completes the task Send e-mail', async () => {
            const task = await taskServiceHrUser.findTaskByName(processInstanceId, 'Send e-mail');
            await taskServiceHrUser.claimTask(task!.id);
            await expectTaskStatusInRbAndQuery(
                taskServiceHrUser,
                queryServiceHrUser,
                task!.id,
                TaskStatus.ASSIGNED
            );
            await taskServiceHrUser.completeTask(task!.id);
            await expectTaskStatusInRbAndQuery(
                taskServiceHrUser,
                queryServiceHrUser,
                task!.id,
                TaskStatus.COMPLETED
            );
        });

        await activiti.step('Then the user will see 1 tasks', async () => {
            await expect
                .poll(async () => {
                    const tasks = await taskServiceHrUser.getTasksByProcessInstanceId(processInstanceId);
                    return tasks.filter((task) => task.status !== TaskStatus.COMPLETED).length;
                }, pollOptions('querySync'))
                .toBe(1);
        });

        await activiti.step('And the task is created Check account', async () => {
            await expect
                .poll(
                    async () =>
                        (await taskServiceHrUser.findTaskByName(processInstanceId, 'Check account'))?.id,
                    pollOptions('querySync')
                )
                .toBeTruthy();
        });

        await activiti.step('When the user claims and completes the task Check account', async () => {
            const task = await taskServiceHrUser.findTaskByName(processInstanceId, 'Check account');
            await taskServiceHrUser.claimTask(task!.id);
            await expectTaskStatusInRbAndQuery(
                taskServiceHrUser,
                queryServiceHrUser,
                task!.id,
                TaskStatus.ASSIGNED
            );
            await taskServiceHrUser.completeTask(task!.id);
            await expectTaskStatusInRbAndQuery(
                taskServiceHrUser,
                queryServiceHrUser,
                task!.id,
                TaskStatus.COMPLETED
            );
        });

        await activiti.step(
            'Then events are emitted for the inclusive gateway inclusiveGatewayEnd',
            async () => {
                await expect
                    .poll(
                        async () =>
                            (
                                await auditServiceHrUser.getActivityEventsForEntity(
                                    processInstanceId,
                                    'inclusiveGatewayEnd',
                                    'inclusiveGateway'
                                )
                            )
                                .map((event) => event.eventType)
                                .sort(),
                        pollOptions('querySync')
                    )
                    .toEqual(
                        expect.arrayContaining([
                            EventType.ACTIVITY_STARTED,
                            EventType.ACTIVITY_COMPLETED,
                        ])
                    );
            }
        );

        await activiti.step('Then the process with inclusive gateway is completed', async () => {
            await expect
                .poll(async () => {
                    const instance = await queryServiceHrUser.getProcessInstance(processInstanceId);
                    return instance.status;
                }, pollOptions('querySync'))
                .toBe(ProcessInstanceStatus.COMPLETED);
        });
    });
});
