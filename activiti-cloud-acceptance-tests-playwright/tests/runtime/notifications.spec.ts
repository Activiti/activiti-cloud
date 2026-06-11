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

/**
 * Port of notifications-actions.story (AAE-46640).
 * Serenity story files remain until a separate retirement ticket.
 */

import { randomUUID } from 'node:crypto';
import { activiti, expect } from '../../fixtures/services.fixture';
import { pollOptions } from '../../config/runtime/timeouts';
import { ProcessDefinitionRegistry } from '../../models/process-definition-registry';
import { ProcessInstanceStatus } from '../../models/runtime-bundle.models';
import type { EngineEventType } from '../../models/notifications.models';
import { actorFromAccessToken, expectedEngineEventBatch } from '../../helpers/notifications.flow';
import { createEngineEventsSubscription } from '../../services/notifications-graphql.client';
import type { EngineEventsSubscription } from '../../services/notifications-graphql.client';
import type { QueryService } from '../../services/query.service';
import type { RuntimeBundleService } from '../../services/runtime-bundle.service';

async function assertNotificationBatch(
    subscription: EngineEventsSubscription,
    eventTypes: EngineEventType[],
    processDefinitionKey: string,
    options: { actor?: string; batchTimeoutMs?: number } = {}
): Promise<void> {
    const expected = expectedEngineEventBatch(eventTypes, processDefinitionKey, { actor: options.actor });
    const batch = await subscription.waitForExpectedEvents(
        expected,
        options.batchTimeoutMs ?? NOTIFICATIONS_BATCH_TIMEOUT_MS
    );
    expect(batch).toEqual(expect.arrayContaining(expected));
    expect(batch).toHaveLength(expected.length);
}

async function readProcessInstanceStatus(
    queryService: QueryService,
    runtimeBundle: RuntimeBundleService | undefined,
    processInstanceId: string
): Promise<string | undefined> {
    const sources = [queryService, runtimeBundle].filter(
        (service): service is QueryService | RuntimeBundleService => service !== undefined
    );

    for (const service of sources) {
        try {
            const instance = await service.getProcessInstance(processInstanceId);
            if (instance?.status) {
                return instance.status;
            }
        } catch {
            // Query may lag behind RB for fast processes (e.g. connector); try the next source.
        }
    }

    return undefined;
}

async function assertNotificationsProcessCompleted(
    queryService: QueryService,
    processInstanceId: string,
    pollProfile: 'querySync' | 'signalProcess' | 'processStatus' = 'querySync',
    runtimeBundle?: RuntimeBundleService
): Promise<void> {
    await expect
        .poll(
            async () =>
                readProcessInstanceStatus(queryService, runtimeBundle, processInstanceId),
            pollOptions(pollProfile)
        )
        .toBe(ProcessInstanceStatus.COMPLETED);
}

const CONNECTOR_PROCESS = ProcessDefinitionRegistry.processDefinitionKeyMatcher(
    'CONNECTOR_PROCESS_INSTANCE'
);
const PROCESS_WITH_VARIABLES = ProcessDefinitionRegistry.processDefinitionKeyMatcher(
    'PROCESS_INSTANCE_WITH_VARIABLES'
);
const SIGNAL_THROW_PROCESS = ProcessDefinitionRegistry.processDefinitionKeyMatcher(
    'SIGNAL_THROW_PROCESS_INSTANCE'
);
const SIGNAL_START_PROCESS = ProcessDefinitionRegistry.processDefinitionKeyMatcher(
    'SIGNAL_START_EVENT_PROCESS'
);
const INTERMEDIATE_TIMER_PROCESS = ProcessDefinitionRegistry.processDefinitionKeyMatcher(
    'INTERMEDIATE_TIMER_EVENT_PROCESS'
);
const BOUNDARY_TIMER_PROCESS = ProcessDefinitionRegistry.processDefinitionKeyMatcher(
    'BOUNDARY_TIMER_EVENT_PROCESS'
);

/** Batch wait for GraphQL WS events; must stay below describe timeout. */
const NOTIFICATIONS_BATCH_TIMEOUT_MS = 90_000;
/** Whole-test cap (CI default test timeout is 60s — too low for WS + actor filter on partitioned RabbitMQ). */
const NOTIFICATIONS_TEST_TIMEOUT_MS = 120_000;

activiti.describe('Runtime — Notifications Actions', { tag: '@slow' }, () => {
    activiti.describe.configure({ mode: 'serial', timeout: NOTIFICATIONS_TEST_TIMEOUT_MS });
    activiti(
        'complete a process instance that uses a connector with subscription to PROCESS event notifications',
        async ({ runtimeBundleServiceTestAdmin, queryServiceTestAdmin, testAdminUserContext }) => {
            const businessKey = randomUUID();
            const subscription = createEngineEventsSubscription({
                accessToken: testAdminUserContext.token,
                eventTypes: ['PROCESS_STARTED', 'PROCESS_COMPLETED'],
                businessKey,
                processDefinitionKey: CONNECTOR_PROCESS,
            });

            try {
                let processInstanceId: string;

                await activiti.step(
                    'When notifications: the user subscribes to PROCESS_STARTED,PROCESS_COMPLETED notifications ' +
                        'And notifications: the user starts a process CONNECTOR_PROCESS_INSTANCE',
                    async () => {
                        await subscription.awaitReady();
                        const processInstance = await runtimeBundleServiceTestAdmin.startProcess({
                            processDefinitionKey: CONNECTOR_PROCESS,
                            businessKey,
                        });
                        expect(processInstance.id).toBeTruthy();
                        processInstanceId = processInstance.id;
                    }
                );

                await activiti.step(
                    'Then notifications: verify process instance started response',
                    async () => {
                        expect(processInstanceId!).toBeTruthy();
                    }
                );

                await activiti.step(
                    'And notifications: the payload with PROCESS_STARTED notifications is expected',
                    async () => {
                        await assertNotificationBatch(subscription, ['PROCESS_STARTED'], CONNECTOR_PROCESS);
                    }
                );

                await activiti.step(
                    'And notifications: the payload with PROCESS_COMPLETED notifications is expected',
                    async () => {
                        await assertNotificationBatch(subscription, ['PROCESS_COMPLETED'], CONNECTOR_PROCESS);
                    }
                );

                await activiti.step('And notifications: verify the status of the process is completed', async () => {
                    await assertNotificationsProcessCompleted(
                        queryServiceTestAdmin,
                        processInstanceId!,
                        'processStatus',
                        runtimeBundleServiceTestAdmin
                    );
                });
            } finally {
                subscription.close();
            }
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
            const businessKey = randomUUID();
            const actor = actorFromAccessToken(testAdminUserContext.token);
            const subscription = createEngineEventsSubscription({
                accessToken: testAdminUserContext.token,
                eventTypes: ['PROCESS_STARTED', 'PROCESS_COMPLETED'],
                businessKey,
                processDefinitionKey: PROCESS_WITH_VARIABLES,
                actor,
            });

            try {
                await activiti.step(
                    'When notifications: the user subscribes to PROCESS_STARTED,PROCESS_COMPLETED notifications with actor filter set to testadmin',
                    async () => {
                        await subscription.awaitReady();
                    }
                );

                let processInstanceId: string;

                await activiti.step(
                    'And notifications: the user starts a process PROCESS_INSTANCE_WITH_VARIABLES',
                    async () => {
                        const processInstance = await runtimeBundleServiceTestAdmin.startProcess({
                            processDefinitionKey: PROCESS_WITH_VARIABLES,
                            businessKey,
                        });
                        expect(processInstance.id).toBeTruthy();
                        processInstanceId = processInstance.id;
                    }
                );

                await activiti.step(
                    'Then notifications: verify process instance started response',
                    async () => {
                        expect(processInstanceId!).toBeTruthy();
                    }
                );

                await activiti.step(
                    'And notifications: the payload with PROCESS_STARTED notifications with actor filter is expected',
                    async () => {
                        await assertNotificationBatch(subscription, ['PROCESS_STARTED'], PROCESS_WITH_VARIABLES, {
                            actor,
                        });
                    }
                );

                await activiti.step('And the admin completes the task', async () => {
                    const tasks = await taskServiceTestAdmin.getTasksByProcessInstanceId(processInstanceId!);
                    expect(tasks.length).toBeGreaterThan(0);
                    await taskAdminServiceTestAdmin.completeTask(tasks[0].id);
                });

                await activiti.step(
                    'And notifications: the payload with PROCESS_COMPLETED notifications with actor filter is expected',
                    async () => {
                        await assertNotificationBatch(subscription, ['PROCESS_COMPLETED'], PROCESS_WITH_VARIABLES, {
                            actor,
                        });
                    }
                );

                await activiti.step('And notifications: verify the status of the process is completed', async () => {
                    await assertNotificationsProcessCompleted(queryServiceTestAdmin, processInstanceId!);
                });
            } finally {
                subscription.close();
            }
        }
    );

    activiti(
        'complete a process instance that sends a signal with subscription to SIGNAL event notifications',
        async ({ runtimeBundleServiceTestAdmin, queryServiceTestAdmin, testAdminUserContext }) => {
            const subscription = createEngineEventsSubscription({
                accessToken: testAdminUserContext.token,
                eventTypes: ['SIGNAL_RECEIVED'],
                businessKey: '*',
                processDefinitionKey: SIGNAL_START_PROCESS,
            });

            try {
                await activiti.step(
                    'When notifications: the user subscribes to SIGNAL_RECEIVED notifications',
                    async () => {
                        await subscription.awaitReady();
                    }
                );

                let processInstanceId: string;

                await activiti.step(
                    'And notifications: the user starts a process SIGNAL_THROW_PROCESS_INSTANCE',
                    async () => {
                        const processInstance = await runtimeBundleServiceTestAdmin.startProcess({
                            processDefinitionKey: SIGNAL_THROW_PROCESS,
                        });
                        expect(processInstance.id).toBeTruthy();
                        processInstanceId = processInstance.id;
                    }
                );

                await activiti.step(
                    'Then notifications: verify process instance started response',
                    async () => {
                        expect(processInstanceId!).toBeTruthy();
                    }
                );

                await activiti.step(
                    'And notifications: the payload with SIGNAL_RECEIVED notifications is expected with process definition key value SignalStartEventProcess',
                    async () => {
                        await assertNotificationBatch(subscription, ['SIGNAL_RECEIVED'], SIGNAL_START_PROCESS, {
                            batchTimeoutMs: NOTIFICATIONS_BATCH_TIMEOUT_MS,
                        });
                    }
                );

                await activiti.step('And notifications: verify the status of the process is completed', async () => {
                    await assertNotificationsProcessCompleted(
                        queryServiceTestAdmin,
                        processInstanceId!,
                        'signalProcess'
                    );
                });
            } finally {
                subscription.close();
            }
        }
    );

    activiti(
        'complete a process instance with intermediate timer subscription to TIMER event notifications',
        async ({ runtimeBundleServiceTestAdmin, queryServiceTestAdmin, testAdminUserContext }) => {
            const businessKey = randomUUID();
            const subscription = createEngineEventsSubscription({
                accessToken: testAdminUserContext.token,
                eventTypes: ['TIMER_SCHEDULED', 'TIMER_FIRED', 'TIMER_EXECUTED'],
                businessKey,
                processDefinitionKey: INTERMEDIATE_TIMER_PROCESS,
            });

            try {
                await activiti.step(
                    'When notifications: the user subscribes to TIMER_SCHEDULED,TIMER_FIRED,TIMER_EXECUTED notifications',
                    async () => {
                        await subscription.awaitReady();
                    }
                );

                let processInstanceId: string;

                await activiti.step(
                    'And notifications: the user starts a process INTERMEDIATE_TIMER_EVENT_PROCESS',
                    async () => {
                        const processInstance = await runtimeBundleServiceTestAdmin.startProcess({
                            processDefinitionKey: INTERMEDIATE_TIMER_PROCESS,
                            businessKey,
                        });
                        expect(processInstance.id).toBeTruthy();
                        processInstanceId = processInstance.id;
                    }
                );

                await activiti.step(
                    'Then notifications: verify process instance started response',
                    async () => {
                        expect(processInstanceId!).toBeTruthy();
                    }
                );

                await activiti.step(
                    'And notifications: the payload with TIMER_SCHEDULED notifications is expected',
                    async () => {
                        await assertNotificationBatch(subscription, ['TIMER_SCHEDULED'], INTERMEDIATE_TIMER_PROCESS, {
                            batchTimeoutMs: NOTIFICATIONS_BATCH_TIMEOUT_MS,
                        });
                    }
                );

                await activiti.step(
                    'And notifications: the payload with TIMER_FIRED,TIMER_EXECUTED notifications is expected',
                    async () => {
                        await assertNotificationBatch(
                            subscription,
                            ['TIMER_FIRED', 'TIMER_EXECUTED'],
                            INTERMEDIATE_TIMER_PROCESS,
                            { batchTimeoutMs: NOTIFICATIONS_BATCH_TIMEOUT_MS }
                        );
                    }
                );

                await activiti.step('And notifications: verify the status of the process is completed', async () => {
                    await assertNotificationsProcessCompleted(
                        queryServiceTestAdmin,
                        processInstanceId!,
                        'processStatus'
                    );
                });
            } finally {
                subscription.close();
            }
        }
    );

    activiti(
        'complete a process instance with boundary timer subscription to TIMER event notifications',
        async ({ runtimeBundleServiceTestAdmin, queryServiceTestAdmin, testAdminUserContext }) => {
            const businessKey = randomUUID();
            const subscription = createEngineEventsSubscription({
                accessToken: testAdminUserContext.token,
                eventTypes: ['TIMER_SCHEDULED', 'TIMER_FIRED', 'TIMER_EXECUTED'],
                businessKey,
                processDefinitionKey: BOUNDARY_TIMER_PROCESS,
            });

            try {
                await activiti.step(
                    'When notifications: the user subscribes to TIMER_SCHEDULED,TIMER_FIRED,TIMER_EXECUTED notifications',
                    async () => {
                        await subscription.awaitReady();
                    }
                );

                let processInstanceId: string;

                await activiti.step(
                    'And notifications: the user starts a process BOUNDARY_TIMER_EVENT_PROCESS',
                    async () => {
                        const processInstance = await runtimeBundleServiceTestAdmin.startProcess({
                            processDefinitionKey: BOUNDARY_TIMER_PROCESS,
                            businessKey,
                        });
                        expect(processInstance.id).toBeTruthy();
                        processInstanceId = processInstance.id;
                    }
                );

                await activiti.step(
                    'Then notifications: verify process instance started response',
                    async () => {
                        expect(processInstanceId!).toBeTruthy();
                    }
                );

                await activiti.step(
                    'And notifications: the payload with TIMER_SCHEDULED notifications is expected',
                    async () => {
                        await assertNotificationBatch(subscription, ['TIMER_SCHEDULED'], BOUNDARY_TIMER_PROCESS, {
                            batchTimeoutMs: NOTIFICATIONS_BATCH_TIMEOUT_MS,
                        });
                    }
                );

                await activiti.step(
                    'And notifications: the payload with TIMER_FIRED,TIMER_EXECUTED notifications is expected',
                    async () => {
                        await assertNotificationBatch(
                            subscription,
                            ['TIMER_FIRED', 'TIMER_EXECUTED'],
                            BOUNDARY_TIMER_PROCESS,
                            { batchTimeoutMs: NOTIFICATIONS_BATCH_TIMEOUT_MS }
                        );
                    }
                );

                await activiti.step('And notifications: verify the status of the process is completed', async () => {
                    await assertNotificationsProcessCompleted(
                        queryServiceTestAdmin,
                        processInstanceId!,
                        'processStatus'
                    );
                });
            } finally {
                subscription.close();
            }
        }
    );

    activiti(
        'complete a process instance by messages with subscriptions to MESSAGE event notifications',
        async ({ runtimeBundleServiceTestAdmin, queryServiceTestAdmin, testAdminUserContext }) => {
            const businessId = randomUUID();
            const subscription = createEngineEventsSubscription({
                accessToken: testAdminUserContext.token,
                eventTypes: ['MESSAGE_RECEIVED', 'MESSAGE_WAITING', 'MESSAGE_SENT'],
                businessKey: businessId,
                processDefinitionKey: '*',
            });

            try {
                await activiti.step(
                    'When notifications: the user subscribes to MESSAGE_RECEIVED,MESSAGE_WAITING,MESSAGE_SENT notifications with businessKey value from session variable called businessId',
                    async () => {
                        await subscription.awaitReady();
                    }
                );

                let processInstanceId: string;
                let processDefinitionKey: string;

                await activiti.step(
                    'And notifications: the user sends a start message named startMessage with businessKey value from session variable called businessId',
                    async () => {
                        const processInstance = await runtimeBundleServiceTestAdmin.sendStartMessage({
                            name: 'startMessage',
                            businessKey: businessId,
                        });
                        expect(processInstance.id).toBeTruthy();
                        processInstanceId = processInstance.id;
                        processDefinitionKey = processInstance.processDefinitionKey;
                    }
                );

                await activiti.step(
                    'Then notifications: verify process instance started response',
                    async () => {
                        expect(processInstanceId!).toBeTruthy();
                    }
                );

                await activiti.step(
                    'And notifications: the payload with MESSAGE_RECEIVED,MESSAGE_WAITING notifications is expected',
                    async () => {
                        await assertNotificationBatch(
                            subscription,
                            ['MESSAGE_RECEIVED', 'MESSAGE_WAITING'],
                            processDefinitionKey!,
                            { batchTimeoutMs: NOTIFICATIONS_BATCH_TIMEOUT_MS }
                        );
                    }
                );

                await activiti.step(
                    'And notifications: the user sends a message named boundaryMessage with correlationKey value of session variable called businessId',
                    async () => {
                        await runtimeBundleServiceTestAdmin.sendReceiveMessage({
                            name: 'boundaryMessage',
                            correlationKey: businessId,
                        });
                    }
                );

                await activiti.step(
                    'And notifications: the payload with MESSAGE_RECEIVED,MESSAGE_WAITING notifications is expected',
                    async () => {
                        await assertNotificationBatch(
                            subscription,
                            ['MESSAGE_RECEIVED', 'MESSAGE_WAITING'],
                            processDefinitionKey!,
                            { batchTimeoutMs: NOTIFICATIONS_BATCH_TIMEOUT_MS }
                        );
                    }
                );

                await activiti.step(
                    'And notifications: the user sends a message named catchMessage with correlationKey value of session variable called businessId',
                    async () => {
                        await runtimeBundleServiceTestAdmin.sendReceiveMessage({
                            name: 'catchMessage',
                            correlationKey: businessId,
                        });
                    }
                );

                await activiti.step(
                    'And notifications: the payload with MESSAGE_RECEIVED,MESSAGE_SENT notifications is expected',
                    async () => {
                        await assertNotificationBatch(
                            subscription,
                            ['MESSAGE_RECEIVED', 'MESSAGE_SENT'],
                            processDefinitionKey!,
                            { batchTimeoutMs: NOTIFICATIONS_BATCH_TIMEOUT_MS }
                        );
                    }
                );

                await activiti.step('And notifications: verify the status of the process is completed', async () => {
                    await assertNotificationsProcessCompleted(queryServiceTestAdmin, processInstanceId!);
                });
            } finally {
                subscription.close();
            }
        }
    );
});
