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
import {
    CloudProcessInstance,
    ProcessInstanceStatus,
} from '../../models/runtime-bundle.models';

const SIGNAL_CATCH_EVENT_PROCESS = 'SignalCatchEventProcess';
const SIGNAL_THROW_EVENT_PROCESS = 'SignalThrowEventProcess';
const SIGNAL_START_EVENT_PROCESS = 'SignalStartEventProcess';
const PROCESS_WITH_BOUNDARY_SIGNAL = 'ProcessWithBoundarySignal';

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
                    const instances = await queryAdminServiceTestAdmin.adminProcessInstances.getProcessInstances({
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
                const task = await taskServiceHrUser.waitForTaskByName(
                    processInstanceBoundarySignal.id,
                    'Boundary container'
                );
                expect(task.name).toBe('Boundary container');
            });

            await activiti.step('When the user starts a process with intermediate throw signal', async () => {
                processInstanceThrowSignal = await runtimeBundleServiceHrUser.startProcess({
                    processDefinitionKey: SIGNAL_THROW_EVENT_PROCESS,
                });
                expect(processInstanceThrowSignal).toBeTruthy();
                expect(processInstanceThrowSignal.id).toBeTruthy();
            });

            await activiti.step('Then the process throwing a signal is completed', async () => {
                const instance = await queryServiceHrUser.waitForProcessInstanceStatus(
                    processInstanceThrowSignal.id,
                    ProcessInstanceStatus.COMPLETED
                );
                expect(instance.status).toBe(ProcessInstanceStatus.COMPLETED);
            });

            await activiti.step('And the process catching a signal is completed', async () => {
                const instance = await queryServiceHrUser.waitForProcessInstanceStatus(
                    processInstanceCatchSignal.id,
                    ProcessInstanceStatus.COMPLETED
                );
                expect(instance.status).toBe(ProcessInstanceStatus.COMPLETED);
            });

            await activiti.step(
                'And the SIGNAL_RECEIVED event was caught up by intermediateCatchEvent process',
                async () => {
                    const event = await auditServiceHrUser.waitForEventOfTypeForProcessInstance(
                        processInstanceCatchSignal.id,
                        EventType.SIGNAL_RECEIVED
                    );
                    expect(event.processDefinitionKey).toBe(processInstanceCatchSignal.processDefinitionKey);
                }
            );

            await activiti.step("And the task 'Boundary target' is created", async () => {
                const task = await taskServiceHrUser.waitForTaskByName(
                    processInstanceBoundarySignal.id,
                    'Boundary target'
                );
                expect(task.name).toBe('Boundary target');
            });

            await activiti.step(
                'And the SIGNAL_RECEIVED event was caught up by boundary signal process',
                async () => {
                    const event = await auditServiceHrUser.waitForEventOfTypeForProcessInstance(
                        processInstanceBoundarySignal.id,
                        EventType.SIGNAL_RECEIVED
                    );
                    expect(event.processDefinitionKey).toBe(processInstanceBoundarySignal.processDefinitionKey);
                }
            );

            await activiti.step(
                'When another user is authenticated as testadmin ' +
                    'And the admin deletes boundary signal process',
                async () => {
                    await runtimeAdminServiceTestAdmin.processInstances.deleteProcessInstance(
                        processInstanceBoundarySignal.id
                    );
                }
            );

            await activiti.step('Then boundary signal process is deleted', async () => {
                const notFound = await runtimeBundleServiceHrUser.waitForProcessInstanceNotFoundInRuntime(
                    processInstanceBoundarySignal.id
                );
                expect(notFound).toBe(true);
            });

            await activiti.step(
                'And check number of processes with processDefinitionKey SignalStartEventProcess increased',
                async () => {
                    const instances = await queryAdminServiceTestAdmin.waitForProcessInstancesAdminCountGreaterThan(
                        { processDefinitionKey: SIGNAL_START_EVENT_PROCESS },
                        initialSignalStartCount
                    );
                    expect(instances.length).toBeGreaterThan(initialSignalStartCount);
                }
            );
        }
    );
});
