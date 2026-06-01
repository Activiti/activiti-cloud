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
import { EventType } from '../../models/audit.models';
import {
    CloudProcessInstance,
    ProcessInstanceStatus,
} from '../../models/runtime-bundle.models';
import { AuditService } from '../../services/audit.service';
import { QueryService } from '../../services/query.service';

const SIGNAL_CATCH_EVENT_PROCESS = 'SignalCatchEventProcess';
const SIGNAL_THROW_EVENT_PROCESS = 'SignalThrowEventProcess';
const SIGNAL_START_EVENT_PROCESS = 'SignalStartEventProcess';
const PROCESS_WITH_BOUNDARY_SIGNAL = 'ProcessWithBoundarySignal';

async function expectProcessCompleted(
    queryService: QueryService,
    processInstanceId: string
): Promise<void> {
    await expect
        .poll(async () => {
            const instance = await queryService.getProcessInstance(processInstanceId);
            return instance.status;
        }, pollOptions('querySync'))
        .toBe(ProcessInstanceStatus.COMPLETED);
}

async function expectSignalReceivedByProcess(
    auditService: AuditService,
    process: CloudProcessInstance
): Promise<void> {
    await expect
        .poll(async () => {
            const events = await auditService.getEvents({
                processInstanceId: process.id,
                eventType: EventType.SIGNAL_RECEIVED,
            });
            return events.some(
                (event) =>
                    event.eventType === EventType.SIGNAL_RECEIVED &&
                    event.processInstanceId === process.id &&
                    event.processDefinitionKey === process.processDefinitionKey
            );
        }, pollOptions('querySync'))
        .toBe(true);
}

activiti.describe('Process Instance Signal Actions', { tag: '@slow' }, () => {
    activiti(
        'process instances with throw, catch, boundary and start signal events',
        async ({
            runtimeBundleServiceHrUser,
            runtimeAdminServiceTestAdmin,
            queryServiceHrUser,
            queryAdminServiceTestAdmin,
            taskServiceHrUser,
            auditServiceHrUser,
        }) => {
            let initialSignalStartCount = 0;
            let processInstanceCatchSignal: CloudProcessInstance;
            let processInstanceBoundarySignal: CloudProcessInstance;
            let processInstanceThrowSignal: CloudProcessInstance;

            await activiti.step(
                'Given the user is authenticated as testadmin ' +
                    'Then query number of processes with processDefinitionKey SignalStartEventProcess',
                async () => {
                    const instances = await queryAdminServiceTestAdmin.getProcessInstancesAdminWithParams({
                        processDefinitionKey: SIGNAL_START_EVENT_PROCESS,
                    });
                    initialSignalStartCount = instances.length;
                }
            );

            await activiti.step(
                'Given the user is authenticated as hruser ' +
                    'When the user starts a process with intermediate catch signal',
                async () => {
                    processInstanceCatchSignal = await runtimeBundleServiceHrUser.startProcess({
                        processDefinitionKey: SIGNAL_CATCH_EVENT_PROCESS,
                    });
                    expect(processInstanceCatchSignal).toBeTruthy();
                    expect(processInstanceCatchSignal.id).toBeTruthy();
                }
            );

            await activiti.step('And the user starts a process with a boundary signal', async () => {
                processInstanceBoundarySignal = await runtimeBundleServiceHrUser.startProcess({
                    processDefinitionKey: PROCESS_WITH_BOUNDARY_SIGNAL,
                });
                expect(processInstanceBoundarySignal).toBeTruthy();
                expect(processInstanceBoundarySignal.id).toBeTruthy();
            });

            await activiti.step("Then the task 'Boundary container' is created", async () => {
                await expect
                    .poll(async () => {
                        const tasks = await taskServiceHrUser.getTasksByProcessInstanceId(
                            processInstanceBoundarySignal.id
                        );
                        return tasks[0]?.name;
                    }, pollOptions('querySync'))
                    .toBe('Boundary container');
            });

            await activiti.step('When the user starts a process with intermediate throw signal', async () => {
                processInstanceThrowSignal = await runtimeBundleServiceHrUser.startProcess({
                    processDefinitionKey: SIGNAL_THROW_EVENT_PROCESS,
                });
                expect(processInstanceThrowSignal).toBeTruthy();
                expect(processInstanceThrowSignal.id).toBeTruthy();
            });

            await activiti.step('Then the process throwing a signal is completed', async () => {
                await expectProcessCompleted(queryServiceHrUser, processInstanceThrowSignal.id);
            });

            await activiti.step('And the process catching a signal is completed', async () => {
                await expectProcessCompleted(queryServiceHrUser, processInstanceCatchSignal.id);
            });

            await activiti.step(
                'And the SIGNAL_RECEIVED event was catched up by intermediateCatchEvent process',
                async () => {
                    await expectSignalReceivedByProcess(auditServiceHrUser, processInstanceCatchSignal);
                }
            );

            await activiti.step("And the task 'Boundary target' is created", async () => {
                await expect
                    .poll(async () => {
                        const tasks = await taskServiceHrUser.getTasksByProcessInstanceId(
                            processInstanceBoundarySignal.id
                        );
                        return tasks[0]?.name;
                    }, pollOptions('querySync'))
                    .toBe('Boundary target');
            });

            await activiti.step(
                'And the SIGNAL_RECEIVED event was catched up by boundary signal process',
                async () => {
                    await expectSignalReceivedByProcess(auditServiceHrUser, processInstanceBoundarySignal);
                }
            );

            await activiti.step(
                'When another user is authenticated as testadmin ' +
                    'And the admin deletes boundary signal process',
                async () => {
                    await runtimeAdminServiceTestAdmin.deleteProcessInstance(
                        processInstanceBoundarySignal.id
                    );
                }
            );

            await activiti.step('Then boundary signal process is deleted', async () => {
                await expect
                    .poll(
                        async () =>
                            runtimeBundleServiceHrUser.isProcessInstanceNotFoundInRuntime(
                                processInstanceBoundarySignal.id
                            ),
                        pollOptions('querySync')
                    )
                    .toBe(true);
            });

            await activiti.step(
                'And check number of processes with processDefinitionKey SignalStartEventProcess increased',
                async () => {
                    await expect
                        .poll(async () => {
                            const instances = await queryAdminServiceTestAdmin.getProcessInstancesAdminWithParams({
                                processDefinitionKey: SIGNAL_START_EVENT_PROCESS,
                            });
                            return instances.length;
                        }, pollOptions('querySync'))
                        .toBeGreaterThan(initialSignalStartCount);
                }
            );
        }
    );
});
