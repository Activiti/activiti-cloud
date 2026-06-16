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
import { getQueryProcessInstanceWhenSynced } from '../../helpers/query-sync';
import { EventType } from '../../models/audit.models';
import {
    CloudProcessInstance,
    ProcessInstanceStatus,
} from '../../models/runtime-bundle.models';

const PROCESS_INSTANCE_WITH_EMBEDDED_SUB_PROCESS = 'startSimpleSubProcess';
const PARENT_PROCESS = 'parentproc-843144bc-3797-40db-8edc-d23190b118e5';

activiti.describe('Process Instance SubProcess Actions', { tag: '@slow' }, () => {
    activiti('complete a process instance with a subProcess', async ({
        runtimeBundleServiceHrUser,
        taskServiceHrUser,
        queryServiceHrUser,
        auditServiceHrUser,
    }) => {
        let processInstance: CloudProcessInstance;
        let currentTaskId: string;

        await activiti.step(
            'Given the user is authenticated as hruser ' +
                'When the user starts a process with tasks and a subProcess called PROCESS_INSTANCE_WITH_EMBEDDED_SUB_PROCESS',
            async () => {
                processInstance = await runtimeBundleServiceHrUser.startProcess({
                    processDefinitionKey: PROCESS_INSTANCE_WITH_EMBEDDED_SUB_PROCESS,
                });
                expect(processInstance).toBeTruthy();
                expect(processInstance.id).toBeTruthy();

                await expect
                    .poll(async () => {
                        const tasks = await taskServiceHrUser.getTasksByProcessInstanceId(
                            processInstance.id
                        );
                        return tasks.length;
                    }, pollOptions('querySync'))
                    .toBeGreaterThan(0);

                const tasks = await taskServiceHrUser.getTasksByProcessInstanceId(processInstance.id);
                currentTaskId = tasks[0].id;
                expect(currentTaskId).toBeTruthy();
            }
        );

        await activiti.step('And the user claims the task declared in the subprocess', async () => {
            await taskServiceHrUser.claimTask(currentTaskId);
        });

        await activiti.step('And the user completes the task declared in the subprocess', async () => {
            await taskServiceHrUser.completeTask(currentTaskId);
        });

        await activiti.step('Then subProcess events are emitted', async () => {
            await expect
                .poll(
                    async () =>
                        (
                            await auditServiceHrUser.getActivityEventsByType(
                                processInstance.id,
                                'subProcess'
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
        });

        await activiti.step('And the process with embedded subprocess is completed', async () => {
            await expect
                .poll(
                    async () =>
                        (
                            await getQueryProcessInstanceWhenSynced(
                                queryServiceHrUser,
                                processInstance.id
                            )
                        )?.status,
                    pollOptions('querySync')
                )
                .toBe(ProcessInstanceStatus.COMPLETED);
        });
    });

    activiti('check variable mapping for a subprocess', async ({
        runtimeBundleServiceHrUser,
        taskServiceHrUser,
    }) => {
        let processInstance: CloudProcessInstance;
        let subprocessInstance: CloudProcessInstance;

        await activiti.step(
            'Given the user is authenticated as hruser ' +
                'When the user starts a process with a subProcess called PARENT_PROCESS',
            async () => {
                processInstance = await runtimeBundleServiceHrUser.startProcess({
                    processDefinitionKey: PARENT_PROCESS,
                });
                expect(processInstance).toBeTruthy();
                expect(processInstance.id).toBeTruthy();
            }
        );

        await activiti.step(
            'Then the parent process instance has a variable named name with value inName',
            async () => {
                await expect
                    .poll(
                        async () =>
                            runtimeBundleServiceHrUser.getProcessInstanceVariableValue(
                                processInstance.id,
                                'name'
                            ),
                        pollOptions('querySync')
                    )
                    .toBe('inName');
            }
        );

        await activiti.step('And the subprocess has been created', async () => {
            await expect
                .poll(async () => {
                    const subprocesses = await runtimeBundleServiceHrUser.getSubProcesses(
                        processInstance.id
                    );
                    return subprocesses.length;
                }, pollOptions('querySync'))
                .toBeGreaterThan(0);

            const subprocesses = await runtimeBundleServiceHrUser.getSubProcesses(processInstance.id);
            subprocessInstance = subprocesses[0];
            expect(subprocessInstance).toBeTruthy();
            expect(subprocessInstance.parentId).toBe(processInstance.id);
        });

        await activiti.step(
            'And a subprocess variable subprocess_input_var1 is created with value inName',
            async () => {
                await expect
                    .poll(
                        async () =>
                            runtimeBundleServiceHrUser.getProcessInstanceVariableValue(
                                subprocessInstance.id,
                                'subprocess_input_var1'
                            ),
                        pollOptions('querySync')
                    )
                    .toBe('inName');
            }
        );

        await activiti.step(
            'When the user claims and completes the subprocess task my-task-call-activity',
            async () => {
                const tasks = await taskServiceHrUser.getTasksByProcessInstanceId(subprocessInstance.id);
                expect(tasks.length).toBeGreaterThan(0);
                const subTask = tasks[0];
                expect(subTask.name).toBe('my-task-call-activity');
                await taskServiceHrUser.claimTask(subTask.id);
                await taskServiceHrUser.completeTask(subTask.id);
            }
        );

        await activiti.step(
            'Then the parent process instance has a variable named name with value outValue',
            async () => {
                await expect
                    .poll(
                        async () =>
                            runtimeBundleServiceHrUser.getProcessInstanceVariableValue(
                                processInstance.id,
                                'name'
                            ),
                        pollOptions('querySync')
                    )
                    .toBe('outValue');
            }
        );
    });
});
