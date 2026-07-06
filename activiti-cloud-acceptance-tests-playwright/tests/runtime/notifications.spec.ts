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

import { randomUUID } from 'node:crypto';
import { activiti, expect } from '../../fixtures/services.fixture';
import { ProcessDefinitionRegistry } from '../../models/process-definition-registry';
import { ProcessInstanceStatus } from '../../models/runtime-bundle.models';
import type { EngineEventsSubscription } from '../../services/notifications';
import {
    actorFromAccessToken,
    expectedEngineEventBatch,
    openEngineEventsSubscription,
} from '../../services/notifications';

activiti.describe('Runtime — Notifications Actions', () => {
    activiti.describe.configure({ mode: 'serial' });

    let activeSubscription: EngineEventsSubscription | undefined;

    activiti.afterEach(() => {
        activeSubscription?.close();
        activeSubscription = undefined;
    });

    activiti(
        'complete a process instance that uses a connector with subscription to PROCESS event notifications',
        async ({ runtimeBundleServiceTestAdmin, queryServiceTestAdmin, testAdminUserContext }) => {
            const processDefinitionKey = ProcessDefinitionRegistry.processDefinitionKeyMatcher(
                'CONNECTOR_PROCESS_INSTANCE'
            );
            const businessKey = randomUUID();
            let processInstanceId: string;

            await activiti.step(
                'When the user subscribes to PROCESS_STARTED and PROCESS_COMPLETED notifications',
                async () => {
                    activeSubscription = await openEngineEventsSubscription({
                        accessToken: testAdminUserContext.token,
                        eventTypes: ['PROCESS_STARTED', 'PROCESS_COMPLETED'],
                        businessKey,
                        processDefinitionKey,
                    });
                }
            );

            await activiti.step('And the user starts CONNECTOR_PROCESS_INSTANCE', async () => {
                const processInstance = await runtimeBundleServiceTestAdmin.startProcess({
                    processDefinitionKey,
                    businessKey,
                });
                expect(processInstance.id).toBeTruthy();
                processInstanceId = processInstance.id;
            });

            await activiti.step('Then the process instance id is returned', async () => {
                expect(processInstanceId!).toBeTruthy();
            });

            await activiti.step('And PROCESS_STARTED notification payload is received', async () => {
                const expected = expectedEngineEventBatch(['PROCESS_STARTED'], processDefinitionKey);
                const batch = await activeSubscription!.waitForExpectedEvents(expected);
                expect(batch).toEqual(
                    expect.arrayContaining(
                        expected.map((item) => expect.objectContaining(item as Record<string, unknown>))
                    )
                );
                expect(batch).toHaveLength(expected.length);
            });

            await activiti.step('And PROCESS_COMPLETED notification payload is received', async () => {
                const expected = expectedEngineEventBatch(['PROCESS_COMPLETED'], processDefinitionKey);
                const batch = await activeSubscription!.waitForExpectedEvents(expected);
                expect(batch).toEqual(
                    expect.arrayContaining(
                        expected.map((item) => expect.objectContaining(item as Record<string, unknown>))
                    )
                );
                expect(batch).toHaveLength(expected.length);
            });

            await activiti.step('And the process instance status is COMPLETED', async () => {
                const instance = await queryServiceTestAdmin.waitForProcessInstanceStatus(
                    processInstanceId!,
                    ProcessInstanceStatus.COMPLETED
                );
                expect(instance.status).toBe(ProcessInstanceStatus.COMPLETED);
            });
        }
    );

    activiti(
        'complete a process instance that uses a simple process with subscription to PROCESS_COMPLETED event notifications with actor filter',
        async ({
            runtimeBundleServiceTestAdmin,
            queryServiceTestAdmin,
            taskServiceTestAdmin,
            taskAdminServiceTestAdmin,
            testAdminUserContext,
        }) => {
            const processDefinitionKey = ProcessDefinitionRegistry.processDefinitionKeyMatcher(
                'PROCESS_INSTANCE_WITH_VARIABLES'
            );
            const businessKey = randomUUID();
            const actor = actorFromAccessToken(testAdminUserContext.token);
            let processInstanceId: string;

            await activiti.step(
                'When the user subscribes to PROCESS_STARTED and PROCESS_COMPLETED notifications with actor filter',
                async () => {
                    activeSubscription = await openEngineEventsSubscription({
                        accessToken: testAdminUserContext.token,
                        eventTypes: ['PROCESS_STARTED', 'PROCESS_COMPLETED'],
                        businessKey,
                        processDefinitionKey,
                        actor,
                    });
                }
            );

            await activiti.step('And the user starts PROCESS_INSTANCE_WITH_VARIABLES', async () => {
                const processInstance = await runtimeBundleServiceTestAdmin.startProcess({
                    processDefinitionKey,
                    businessKey,
                });
                expect(processInstance.id).toBeTruthy();
                processInstanceId = processInstance.id;
            });

            await activiti.step('Then the process instance id is returned', async () => {
                expect(processInstanceId!).toBeTruthy();
            });

            await activiti.step('And PROCESS_STARTED notification payload with actor filter is received', async () => {
                const expected = expectedEngineEventBatch(['PROCESS_STARTED'], processDefinitionKey, { actor });
                const batch = await activeSubscription!.waitForExpectedEvents(expected);
                expect(batch).toEqual(
                    expect.arrayContaining(
                        expected.map((item) => expect.objectContaining(item as Record<string, unknown>))
                    )
                );
                expect(batch).toHaveLength(expected.length);
            });

            await activiti.step('And the admin completes the task', async () => {
                const tasks = await taskServiceTestAdmin.getTasksByProcessInstanceId(processInstanceId!);
                expect(tasks.length).toBeGreaterThan(0);
                await taskAdminServiceTestAdmin.tasks.completeTask(tasks[0].id);
            });

            await activiti.step('And PROCESS_COMPLETED notification payload with actor filter is received', async () => {
                const expected = expectedEngineEventBatch(['PROCESS_COMPLETED'], processDefinitionKey, { actor });
                const batch = await activeSubscription!.waitForExpectedEvents(expected);
                expect(batch).toEqual(
                    expect.arrayContaining(
                        expected.map((item) => expect.objectContaining(item as Record<string, unknown>))
                    )
                );
                expect(batch).toHaveLength(expected.length);
            });

            await activiti.step('And the process instance status is COMPLETED', async () => {
                const instance = await queryServiceTestAdmin.waitForProcessInstanceStatus(
                    processInstanceId!,
                    ProcessInstanceStatus.COMPLETED
                );
                expect(instance.status).toBe(ProcessInstanceStatus.COMPLETED);
            });
        }
    );

    activiti(
        'complete a process instance that sends a signal with subscription to SIGNAL event notifications',
        async ({ runtimeBundleServiceTestAdmin, queryServiceTestAdmin, testAdminUserContext }) => {
            const signalStartProcessKey = ProcessDefinitionRegistry.processDefinitionKeyMatcher(
                'SIGNAL_START_EVENT_PROCESS'
            );
            const signalThrowProcessKey = ProcessDefinitionRegistry.processDefinitionKeyMatcher(
                'SIGNAL_THROW_PROCESS_INSTANCE'
            );
            let processInstanceId: string;

            await activiti.step('When the user subscribes to SIGNAL_RECEIVED notifications', async () => {
                activeSubscription = await openEngineEventsSubscription({
                    accessToken: testAdminUserContext.token,
                    eventTypes: ['SIGNAL_RECEIVED'],
                    businessKey: '*',
                    processDefinitionKey: signalStartProcessKey,
                });
            });

            await activiti.step('And the user starts SIGNAL_THROW_PROCESS_INSTANCE', async () => {
                const processInstance = await runtimeBundleServiceTestAdmin.startProcess({
                    processDefinitionKey: signalThrowProcessKey,
                });
                expect(processInstance.id).toBeTruthy();
                processInstanceId = processInstance.id;
            });

            await activiti.step('Then the process instance id is returned', async () => {
                expect(processInstanceId!).toBeTruthy();
            });

            await activiti.step(
                'And SIGNAL_RECEIVED notification payload for SignalStartEventProcess is received',
                async () => {
                    const expected = expectedEngineEventBatch(['SIGNAL_RECEIVED'], signalStartProcessKey);
                    const batch = await activeSubscription!.waitForExpectedEvents(expected);
                    expect(batch).toEqual(
                        expect.arrayContaining(
                            expected.map((item) => expect.objectContaining(item as Record<string, unknown>))
                        )
                    );
                    expect(batch).toHaveLength(expected.length);
                }
            );

            await activiti.step('And the process instance status is COMPLETED', async () => {
                const instance = await queryServiceTestAdmin.waitForProcessInstanceStatus(
                    processInstanceId!,
                    ProcessInstanceStatus.COMPLETED,
                    'signalProcess'
                );
                expect(instance.status).toBe(ProcessInstanceStatus.COMPLETED);
            });
        }
    );

    activiti(
        'complete a process instance with intermediate timer subscription to TIMER event notifications',
        async ({ runtimeBundleServiceTestAdmin, queryServiceTestAdmin, testAdminUserContext }) => {
            const processDefinitionKey = ProcessDefinitionRegistry.processDefinitionKeyMatcher(
                'INTERMEDIATE_TIMER_EVENT_PROCESS'
            );
            const businessKey = randomUUID();
            let processInstanceId: string;

            await activiti.step(
                'When the user subscribes to TIMER_SCHEDULED, TIMER_FIRED and TIMER_EXECUTED notifications',
                async () => {
                    activeSubscription = await openEngineEventsSubscription({
                        accessToken: testAdminUserContext.token,
                        eventTypes: ['TIMER_SCHEDULED', 'TIMER_FIRED', 'TIMER_EXECUTED'],
                        businessKey,
                        processDefinitionKey,
                    });
                }
            );

            await activiti.step('And the user starts INTERMEDIATE_TIMER_EVENT_PROCESS', async () => {
                const processInstance = await runtimeBundleServiceTestAdmin.startProcess({
                    processDefinitionKey,
                    businessKey,
                });
                expect(processInstance.id).toBeTruthy();
                processInstanceId = processInstance.id;
            });

            await activiti.step('Then the process instance id is returned', async () => {
                expect(processInstanceId!).toBeTruthy();
            });

            await activiti.step('And TIMER_SCHEDULED notification payload is received', async () => {
                const expected = expectedEngineEventBatch(['TIMER_SCHEDULED'], processDefinitionKey);
                const batch = await activeSubscription!.waitForExpectedEvents(expected);
                expect(batch).toEqual(
                    expect.arrayContaining(
                        expected.map((item) => expect.objectContaining(item as Record<string, unknown>))
                    )
                );
                expect(batch).toHaveLength(expected.length);
            });

            await activiti.step('And TIMER_FIRED and TIMER_EXECUTED notification payloads are received', async () => {
                const expected = expectedEngineEventBatch(['TIMER_FIRED', 'TIMER_EXECUTED'], processDefinitionKey);
                const batch = await activeSubscription!.waitForExpectedEvents(expected);
                expect(batch).toEqual(
                    expect.arrayContaining(
                        expected.map((item) => expect.objectContaining(item as Record<string, unknown>))
                    )
                );
                expect(batch).toHaveLength(expected.length);
            });

            await activiti.step('And the process instance status is COMPLETED', async () => {
                const instance = await queryServiceTestAdmin.waitForProcessInstanceStatus(
                    processInstanceId!,
                    ProcessInstanceStatus.COMPLETED,
                    'processStatus'
                );
                expect(instance.status).toBe(ProcessInstanceStatus.COMPLETED);
            });
        }
    );

    activiti(
        'complete a process instance with boundary timer subscription to TIMER event notifications',
        async ({ runtimeBundleServiceTestAdmin, queryServiceTestAdmin, testAdminUserContext }) => {
            const processDefinitionKey = ProcessDefinitionRegistry.processDefinitionKeyMatcher(
                'BOUNDARY_TIMER_EVENT_PROCESS'
            );
            const businessKey = randomUUID();
            let processInstanceId: string;

            await activiti.step(
                'When the user subscribes to TIMER_SCHEDULED, TIMER_FIRED and TIMER_EXECUTED notifications',
                async () => {
                    activeSubscription = await openEngineEventsSubscription({
                        accessToken: testAdminUserContext.token,
                        eventTypes: ['TIMER_SCHEDULED', 'TIMER_FIRED', 'TIMER_EXECUTED'],
                        businessKey,
                        processDefinitionKey,
                    });
                }
            );

            await activiti.step('And the user starts BOUNDARY_TIMER_EVENT_PROCESS', async () => {
                const processInstance = await runtimeBundleServiceTestAdmin.startProcess({
                    processDefinitionKey,
                    businessKey,
                });
                expect(processInstance.id).toBeTruthy();
                processInstanceId = processInstance.id;
            });

            await activiti.step('Then the process instance id is returned', async () => {
                expect(processInstanceId!).toBeTruthy();
            });

            await activiti.step('And TIMER_SCHEDULED notification payload is received', async () => {
                const expected = expectedEngineEventBatch(['TIMER_SCHEDULED'], processDefinitionKey);
                const batch = await activeSubscription!.waitForExpectedEvents(expected);
                expect(batch).toEqual(
                    expect.arrayContaining(
                        expected.map((item) => expect.objectContaining(item as Record<string, unknown>))
                    )
                );
                expect(batch).toHaveLength(expected.length);
            });

            await activiti.step('And TIMER_FIRED and TIMER_EXECUTED notification payloads are received', async () => {
                const expected = expectedEngineEventBatch(['TIMER_FIRED', 'TIMER_EXECUTED'], processDefinitionKey);
                const batch = await activeSubscription!.waitForExpectedEvents(expected);
                expect(batch).toEqual(
                    expect.arrayContaining(
                        expected.map((item) => expect.objectContaining(item as Record<string, unknown>))
                    )
                );
                expect(batch).toHaveLength(expected.length);
            });

            await activiti.step('And the process instance status is COMPLETED', async () => {
                const instance = await queryServiceTestAdmin.waitForProcessInstanceStatus(
                    processInstanceId!,
                    ProcessInstanceStatus.COMPLETED,
                    'processStatus'
                );
                expect(instance.status).toBe(ProcessInstanceStatus.COMPLETED);
            });
        }
    );

    activiti(
        'complete a process instance by messages with subscriptions to MESSAGE event notifications',
        async ({ runtimeBundleServiceTestAdmin, queryServiceTestAdmin, testAdminUserContext }) => {
            const businessId = randomUUID();
            let processInstanceId: string;
            let processDefinitionKey: string;

            await activiti.step(
                'When the user subscribes to MESSAGE_RECEIVED, MESSAGE_WAITING and MESSAGE_SENT notifications',
                async () => {
                    activeSubscription = await openEngineEventsSubscription({
                        accessToken: testAdminUserContext.token,
                        eventTypes: ['MESSAGE_RECEIVED', 'MESSAGE_WAITING', 'MESSAGE_SENT'],
                        businessKey: businessId,
                        processDefinitionKey: '*',
                    });
                }
            );

            await activiti.step('And the user sends startMessage with the session businessId', async () => {
                const processInstance = await runtimeBundleServiceTestAdmin.sendStartMessage({
                    name: 'startMessage',
                    businessKey: businessId,
                });
                expect(processInstance.id).toBeTruthy();
                processInstanceId = processInstance.id;
                processDefinitionKey = processInstance.processDefinitionKey;
            });

            await activiti.step('Then the process instance id is returned', async () => {
                expect(processInstanceId!).toBeTruthy();
            });

            await activiti.step('And MESSAGE_RECEIVED and MESSAGE_WAITING notification payloads are received', async () => {
                const expected = expectedEngineEventBatch(
                    ['MESSAGE_RECEIVED', 'MESSAGE_WAITING'],
                    processDefinitionKey!
                );
                const batch = await activeSubscription!.waitForExpectedEvents(expected);
                expect(batch).toEqual(
                    expect.arrayContaining(
                        expected.map((item) => expect.objectContaining(item as Record<string, unknown>))
                    )
                );
                expect(batch).toHaveLength(expected.length);
            });

            await activiti.step('And the user sends boundaryMessage with the session businessId', async () => {
                await runtimeBundleServiceTestAdmin.sendReceiveMessage({
                    name: 'boundaryMessage',
                    correlationKey: businessId,
                });
            });

            await activiti.step('And MESSAGE_RECEIVED and MESSAGE_WAITING notification payloads are received again', async () => {
                const expected = expectedEngineEventBatch(
                    ['MESSAGE_RECEIVED', 'MESSAGE_WAITING'],
                    processDefinitionKey!
                );
                const batch = await activeSubscription!.waitForExpectedEvents(expected);
                expect(batch).toEqual(
                    expect.arrayContaining(
                        expected.map((item) => expect.objectContaining(item as Record<string, unknown>))
                    )
                );
                expect(batch).toHaveLength(expected.length);
            });

            await activiti.step('And the user sends catchMessage with the session businessId', async () => {
                await runtimeBundleServiceTestAdmin.sendReceiveMessage({
                    name: 'catchMessage',
                    correlationKey: businessId,
                });
            });

            await activiti.step('And MESSAGE_RECEIVED and MESSAGE_SENT notification payloads are received', async () => {
                const expected = expectedEngineEventBatch(
                    ['MESSAGE_RECEIVED', 'MESSAGE_SENT'],
                    processDefinitionKey!
                );
                const batch = await activeSubscription!.waitForExpectedEvents(expected);
                expect(batch).toEqual(
                    expect.arrayContaining(
                        expected.map((item) => expect.objectContaining(item as Record<string, unknown>))
                    )
                );
                expect(batch).toHaveLength(expected.length);
            });

            await activiti.step('And the process instance status is COMPLETED', async () => {
                const instance = await queryServiceTestAdmin.waitForProcessInstanceStatus(
                    processInstanceId!,
                    ProcessInstanceStatus.COMPLETED
                );
                expect(instance.status).toBe(ProcessInstanceStatus.COMPLETED);
            });
        }
    );
});
