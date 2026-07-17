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

const INTERMEDIATE_TIMER_EVENT_PROCESS = 'intermediateTimerEventExample';
const START_TIMER_EVENT_PROCESS = 'startTimerEventExample';
const BOUNDARY_TIMER_EVENT_PROCESS = 'boundaryTimerEventExample';

activiti.describe('Process Instance Timer Actions', { tag: '@slow' }, () => {
    // FIXME upstream BPMN bug: intermediateTimerEventExample is deployed with a raw-UUID
    // processDefinitionId (e.g. 'c16536d8-...') instead of 'intermediateTimerEventExample:1:<uuid>',
    // so the audit ACL filter `processDefinitionId LIKE 'intermediateTimerEventExample:%'`
    // returns 0 events and the scenario cannot be verified end-to-end.
    activiti.skip(
        'check a process instance with intermediate timer event',
        async ({ runtimeBundleServiceHrUser, queryServiceHrUser, auditServiceHrUser }) => {
            let processInstanceId: string;

            await activiti.step(
                'Given the user is authenticated as hruser ' +
                    'When the user starts a process with timer events called INTERMEDIATE_TIMER_EVENT_PROCESS',
                async () => {
                    const processInstance = await runtimeBundleServiceHrUser.processInstances.startProcess({
                        processDefinitionKey: INTERMEDIATE_TIMER_EVENT_PROCESS,
                    });
                    expect(processInstance.id).toBeTruthy();
                    processInstanceId = processInstance.id;
                }
            );

            await activiti.step(
                "Then TIMER_SCHEDULED events are emitted for the timer 'timer' and timeout 5 seconds",
                async () => {
                    const events = await auditServiceHrUser.waitForEventsByEntityAndType(
                        processInstanceId,
                        'timer',
                        EventType.TIMER_SCHEDULED
                    );
                    expect(events.length).toBeGreaterThan(0);
                }
            );

            await activiti.step(
                "And TIMER_EXECUTED events are emitted for the timer 'timer' and timeout 10 seconds",
                async () => {
                    const events = await auditServiceHrUser.waitForEventsByEntityAndType(
                        processInstanceId,
                        'timer',
                        EventType.TIMER_EXECUTED
                    );
                    expect(events.length).toBeGreaterThan(0);
                }
            );

            await activiti.step('And the process with timer events is completed', async () => {
                const instance = await queryServiceHrUser.waitForProcessInstanceStatus(
                    processInstanceId,
                    ProcessInstanceStatus.COMPLETED
                );
                expect(instance.status).toBe(ProcessInstanceStatus.COMPLETED);
            });
        }
    );

    // FIXME upstream BPMN bug: startTimerEventExample is deployed with a raw-UUID
    // processDefinitionId, so the audit ACL filter on processDefinitionId returns 0 events
    // for both testadmin and hruser, preventing verification of TIMER_SCHEDULED/FIRED/EXECUTED.
    activiti.skip(
        'check a process instance with start timer event',
        async ({ queryAdminServiceTestAdmin, auditServiceTestAdmin }) => {
            await activiti.step(
                'Given the user is authenticated as testadmin ' +
                    'Then the admin query returns 2 processes called START_TIMER_EVENT_PROCESS with timeout 60 seconds',
                async () => {
                    const instances = await queryAdminServiceTestAdmin.waitForProcessInstancesAdminCountGreaterThan(
                        { processDefinitionKey: START_TIMER_EVENT_PROCESS },
                        1
                    );
                    expect(instances.length).toBeGreaterThanOrEqual(2);
                }
            );

            await activiti.step(
                'And timer events are emitted for processes called START_TIMER_EVENT_PROCESS',
                async () => {
                    const types = await auditServiceTestAdmin.waitForEventTypesByEntityAndDefinitionKey(
                        'theStart',
                        START_TIMER_EVENT_PROCESS,
                        [
                            EventType.TIMER_SCHEDULED,
                            EventType.TIMER_FIRED,
                            EventType.TIMER_EXECUTED,
                            EventType.ACTIVITY_COMPLETED,
                        ]
                    );
                    expect(types).toEqual(
                        expect.arrayContaining([
                            EventType.TIMER_SCHEDULED,
                            EventType.TIMER_FIRED,
                            EventType.TIMER_EXECUTED,
                            EventType.ACTIVITY_COMPLETED,
                        ])
                    );
                }
            );
        }
    );

    activiti(
        'check a process instance with boundary timer event',
        async ({ runtimeBundleServiceHrUser, queryServiceHrUser, auditServiceHrUser }) => {
            let processInstanceId: string;

            await activiti.step(
                'Given the user is authenticated as hruser ' +
                    'When the user starts a process with timer events called BOUNDARY_TIMER_EVENT_PROCESS',
                async () => {
                    const processInstance = await runtimeBundleServiceHrUser.processInstances.startProcess({
                        processDefinitionKey: BOUNDARY_TIMER_EVENT_PROCESS,
                    });
                    expect(processInstance.id).toBeTruthy();
                    processInstanceId = processInstance.id;
                }
            );

            await activiti.step(
                "Then TIMER_SCHEDULED boundary events are emitted for the timer 'timer' and timeout 5 seconds",
                async () => {
                    const events = await auditServiceHrUser.waitForEventsByEntityAndType(
                        processInstanceId,
                        'timer',
                        EventType.TIMER_SCHEDULED
                    );
                    expect(events.length).toBeGreaterThan(0);
                }
            );

            await activiti.step(
                "And TIMER_EXECUTED events are emitted for the timer 'timer' and timeout 10 seconds",
                async () => {
                    const events = await auditServiceHrUser.waitForEventsByEntityAndType(
                        processInstanceId,
                        'timer',
                        EventType.TIMER_EXECUTED
                    );
                    expect(events.length).toBeGreaterThan(0);
                }
            );

            await activiti.step('And the process with timer events is completed', async () => {
                const instance = await queryServiceHrUser.waitForProcessInstanceStatus(
                    processInstanceId,
                    ProcessInstanceStatus.COMPLETED
                );
                expect(instance.status).toBe(ProcessInstanceStatus.COMPLETED);
            });
        }
    );
});
