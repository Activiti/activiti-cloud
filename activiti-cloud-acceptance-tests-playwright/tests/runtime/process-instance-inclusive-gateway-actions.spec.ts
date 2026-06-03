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
import { ProcessInstanceStatus } from '../../models/runtime-bundle.models';
import { TaskStatus } from '../../models/task.models';
import { TaskService } from '../../services/task.service';
import { QueryService } from '../../services/query.service';
import { AuditService } from '../../services/audit.service';

const PROCESS_INSTANCE_WITH_INCLUSIVE_GATEWAY = 'basicInclusiveGateway';

async function findTaskIdByName(
    taskService: TaskService,
    processInstanceId: string,
    taskName: string
): Promise<string> {
    let foundTaskId: string | undefined;
    await expect
        .poll(async () => {
            const tasks = await taskService.getTasksByProcessInstanceId(processInstanceId);
            const match = tasks.find((task) => task.name === taskName);
            if (match) {
                foundTaskId = match.id;
                return true;
            }
            return false;
        }, pollOptions('querySync'))
        .toBe(true);
    return foundTaskId as string;
}

async function claimAndCompleteTask(
    taskService: TaskService,
    queryService: QueryService,
    taskId: string
): Promise<void> {
    await taskService.claimTask(taskId);
    await expect
        .poll(async () => (await taskService.getTaskById(taskId)).status, pollOptions('querySync'))
        .toBe(TaskStatus.ASSIGNED);
    await expect
        .poll(async () => (await queryService.getTaskById(taskId))?.status, pollOptions('querySync'))
        .toBe(TaskStatus.ASSIGNED);

    await taskService.completeTask(taskId);
    await expect
        .poll(async () => (await queryService.getTaskById(taskId))?.status, pollOptions('querySync'))
        .toBe(TaskStatus.COMPLETED);
}

async function expectInclusiveGatewayEvents(
    auditService: AuditService,
    processInstanceId: string,
    gatewayId: string
): Promise<void> {
    await expect
        .poll(async () => {
            const events = await auditService.getEventsByProcessInstanceId(processInstanceId);
            const matches = events.filter((event) => {
                if (event.entityId !== gatewayId) {
                    return false;
                }
                const entity = event.entity as
                    | { activityType?: string; processInstanceId?: string }
                    | undefined;
                return (
                    entity?.activityType === 'inclusiveGateway' &&
                    entity?.processInstanceId === processInstanceId
                );
            });
            const hasStarted = matches.some((event) => event.eventType === 'ACTIVITY_STARTED');
            const hasCompleted = matches.some((event) => event.eventType === 'ACTIVITY_COMPLETED');
            return hasStarted && hasCompleted;
        }, pollOptions('querySync'))
        .toBe(true);
}

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
            await findTaskIdByName(taskServiceHrUser,
                processInstanceId,
                'Start Process'
            );
        });

        await activiti.step('When the user claims and completes the task Start Process', async () => {
            const taskId = await findTaskIdByName(taskServiceHrUser,
                processInstanceId,
                'Start Process'
            );
            await claimAndCompleteTask(taskServiceHrUser, queryServiceHrUser, taskId);
        });

        await activiti.step(
            'Then events are emitted for the inclusive gateway inclusiveGateway',
            async () => {
                await expectInclusiveGatewayEvents(
                    auditServiceHrUser,
                    processInstanceId,
                    'inclusiveGateway'
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
            await findTaskIdByName(taskServiceHrUser,
                processInstanceId,
                'Send e-mail'
            );
        });

        await activiti.step('And the task is created Check account', async () => {
            await findTaskIdByName(taskServiceHrUser,
                processInstanceId,
                'Check account'
            );
        });

        await activiti.step('When the user claims and completes the task Send e-mail', async () => {
            const taskId = await findTaskIdByName(taskServiceHrUser,
                processInstanceId,
                'Send e-mail'
            );
            await claimAndCompleteTask(taskServiceHrUser, queryServiceHrUser, taskId);
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
            await findTaskIdByName(taskServiceHrUser,
                processInstanceId,
                'Check account'
            );
        });

        await activiti.step('When the user claims and completes the task Check account', async () => {
            const taskId = await findTaskIdByName(taskServiceHrUser,
                processInstanceId,
                'Check account'
            );
            await claimAndCompleteTask(taskServiceHrUser, queryServiceHrUser, taskId);
        });

        await activiti.step(
            'Then events are emitted for the inclusive gateway inclusiveGatewayEnd',
            async () => {
                await expectInclusiveGatewayEvents(
                    auditServiceHrUser,
                    processInstanceId,
                    'inclusiveGatewayEnd'
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
