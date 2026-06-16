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
import { pollOptions } from '../../config/runtime/timeouts';
import { EventType } from '../../models/audit.models';
import { ProcessInstanceStatus } from '../../models/runtime-bundle.models';

activiti.describe('Process Instance Message Actions', { tag: '@slow' }, () => {
    // FIXME: All scenarios below are blocked by an upstream BPMN deployment bug in the
    // runtime-bundle image (activiti/example-runtime-bundle:9.1.0-alpha.49). For the
    // process definitions `shouldDeliverMessagesViaRestApi`,
    // `ThrowCatchMessageIT_Process1/2/3` the column `act_re_procdef.id_` is stored as a
    // raw UUID instead of the canonical `<key>:<version>:<uuid>` format. The audit/query
    // ACL filter (`processDefinitionId LIKE '<key>:%'`) therefore rejects all events for
    // these definitions, even though the events are written to the audit_event table.
    // Re-enable once the upstream image is fixed.
    activiti.skip('deliver messages via process runtime Rest Api', async ({
        runtimeBundleServiceHrUser,
        queryServiceHrUser,
        auditServiceHrUser,
    }) => {
        const businessId = randomUUID();
        let processInstanceId: string;

        await activiti.step(
            'When messages: the user sends a start message named startMessage with businessKey value of businessId session variable',
            async () => {
                const processInstance = await runtimeBundleServiceHrUser.sendStartMessage({
                    name: 'startMessage',
                    businessKey: businessId,
                });
                processInstanceId = processInstance.id;
                expect(processInstanceId).toBeTruthy();
            }
        );

        await activiti.step(
            "Then messages: MESSAGE_RECEIVED event is emitted for the message 'startMessage'",
            async () => {
                await expect
                    .poll(
                        async () =>
                            (
                                await auditServiceHrUser.getMessageEventsForProcessInstance(
                                    processInstanceId,
                                    EventType.MESSAGE_RECEIVED,
                                    'startMessage'
                                )
                            ).length,
                        pollOptions('querySync')
                    )
                    .toBeGreaterThan(0);
            }
        );

        await activiti.step(
            "And messages: MESSAGE_WAITING event is emitted for the message 'boundaryMessage'",
            async () => {
                await expect
                    .poll(
                        async () =>
                            (
                                await auditServiceHrUser.getMessageEventsForProcessInstance(
                                    processInstanceId,
                                    EventType.MESSAGE_WAITING,
                                    'boundaryMessage'
                                )
                            ).length,
                        pollOptions('querySync')
                    )
                    .toBeGreaterThan(0);
            }
        );

        await activiti.step(
            'And messages: the user sends a message named boundaryMessage with correlationKey value of businessId session variable',
            async () => {
                const response = await runtimeBundleServiceHrUser.sendReceiveMessage({
                    name: 'boundaryMessage',
                    correlationKey: businessId,
                });
                expect(response.httpStatus).toBeGreaterThanOrEqual(200);
                expect(response.httpStatus).toBeLessThan(300);
            }
        );

        await activiti.step(
            "And messages: MESSAGE_RECEIVED event is emitted for the message 'boundaryMessage'",
            async () => {
                await expect
                    .poll(
                        async () =>
                            (
                                await auditServiceHrUser.getMessageEventsForProcessInstance(
                                    processInstanceId,
                                    EventType.MESSAGE_RECEIVED,
                                    'boundaryMessage'
                                )
                            ).length,
                        pollOptions('querySync')
                    )
                    .toBeGreaterThan(0);
            }
        );

        await activiti.step(
            "And messages: MESSAGE_WAITING event is emitted for the message 'catchMessage'",
            async () => {
                await expect
                    .poll(
                        async () =>
                            (
                                await auditServiceHrUser.getMessageEventsForProcessInstance(
                                    processInstanceId,
                                    EventType.MESSAGE_WAITING,
                                    'catchMessage'
                                )
                            ).length,
                        pollOptions('querySync')
                    )
                    .toBeGreaterThan(0);
            }
        );

        await activiti.step(
            'And messages: the user sends a message named catchMessage with correlationKey value of businessId session variable',
            async () => {
                const response = await runtimeBundleServiceHrUser.sendReceiveMessage({
                    name: 'catchMessage',
                    correlationKey: businessId,
                });
                expect(response.httpStatus).toBeGreaterThanOrEqual(200);
                expect(response.httpStatus).toBeLessThan(300);
            }
        );

        await activiti.step(
            "And messages: MESSAGE_RECEIVED event is emitted for the message 'catchMessage'",
            async () => {
                await expect
                    .poll(
                        async () =>
                            (
                                await auditServiceHrUser.getMessageEventsForProcessInstance(
                                    processInstanceId,
                                    EventType.MESSAGE_RECEIVED,
                                    'catchMessage'
                                )
                            ).length,
                        pollOptions('querySync')
                    )
                    .toBeGreaterThan(0);
            }
        );

        await activiti.step(
            "And messages: MESSAGE_SENT event is emitted for the message 'endMessage'",
            async () => {
                await expect
                    .poll(
                        async () =>
                            (
                                await auditServiceHrUser.getMessageEventsForProcessInstance(
                                    processInstanceId,
                                    EventType.MESSAGE_SENT,
                                    'endMessage'
                                )
                            ).length,
                        pollOptions('querySync')
                    )
                    .toBeGreaterThan(0);
            }
        );

        await activiti.step('And messages: the process with message events is completed', async () => {
            await expect
                .poll(async () => {
                    const instance = await queryServiceHrUser.getProcessInstance(processInstanceId);
                    return instance.status;
                }, pollOptions('querySync'))
                .toBe(ProcessInstanceStatus.COMPLETED);
        });
    });

    activiti.skip('start process with non existing correlation key', async ({
        runtimeBundleServiceHrUser,
        auditServiceHrUser,
    }) => {
        const businessId = randomUUID();
        let processInstanceId: string;

        await activiti.step(
            'When messages: the user sends a start message named startMessage with businessKey value of businessId session variable',
            async () => {
                const processInstance = await runtimeBundleServiceHrUser.sendStartMessage({
                    name: 'startMessage',
                    businessKey: businessId,
                });
                processInstanceId = processInstance.id;
                expect(processInstanceId).toBeTruthy();
            }
        );

        await activiti.step(
            "Then messages: MESSAGE_RECEIVED event is emitted for the message 'startMessage'",
            async () => {
                await expect
                    .poll(
                        async () =>
                            (
                                await auditServiceHrUser.getMessageEventsForProcessInstance(
                                    processInstanceId,
                                    EventType.MESSAGE_RECEIVED,
                                    'startMessage'
                                )
                            ).length,
                        pollOptions('querySync')
                    )
                    .toBeGreaterThan(0);
            }
        );

        await activiti.step(
            "And messages: MESSAGE_WAITING event is emitted for the message 'boundaryMessage'",
            async () => {
                await expect
                    .poll(
                        async () =>
                            (
                                await auditServiceHrUser.getMessageEventsForProcessInstance(
                                    processInstanceId,
                                    EventType.MESSAGE_WAITING,
                                    'boundaryMessage'
                                )
                            ).length,
                        pollOptions('querySync')
                    )
                    .toBeGreaterThan(0);
            }
        );

        await activiti.step(
            'And messages: the user gets not found error when sends a message named boundaryMessage with nonexisting correlationKey',
            async () => {
                const response = await runtimeBundleServiceHrUser.sendReceiveMessage({
                    name: 'boundaryMessage',
                    correlationKey: 'nonexistingkey',
                });
                expect(response.httpStatus).toBe(404);
                expect(JSON.stringify(response)).toContain(
                    "Message subscription name 'boundaryMessage' with correlation key 'nonexistingkey' not found."
                );
            }
        );
    });

    activiti.skip('start process with duplicate correlation key', async ({
        runtimeBundleServiceHrUser,
        auditServiceHrUser,
    }) => {
        const businessId = randomUUID();
        let processInstanceId: string;

        await activiti.step(
            'When messages: the user sends a start message named startMessage with businessKey value of businessId session variable',
            async () => {
                const processInstance = await runtimeBundleServiceHrUser.sendStartMessage({
                    name: 'startMessage',
                    businessKey: businessId,
                });
                processInstanceId = processInstance.id;
                expect(processInstanceId).toBeTruthy();
            }
        );

        await activiti.step(
            "Then messages: MESSAGE_RECEIVED event is emitted for the message 'startMessage'",
            async () => {
                await expect
                    .poll(
                        async () =>
                            (
                                await auditServiceHrUser.getMessageEventsForProcessInstance(
                                    processInstanceId,
                                    EventType.MESSAGE_RECEIVED,
                                    'startMessage'
                                )
                            ).length,
                        pollOptions('querySync')
                    )
                    .toBeGreaterThan(0);
            }
        );

        await activiti.step(
            "And messages: MESSAGE_WAITING event is emitted for the message 'boundaryMessage'",
            async () => {
                await expect
                    .poll(
                        async () =>
                            (
                                await auditServiceHrUser.getMessageEventsForProcessInstance(
                                    processInstanceId,
                                    EventType.MESSAGE_WAITING,
                                    'boundaryMessage'
                                )
                            ).length,
                        pollOptions('querySync')
                    )
                    .toBeGreaterThan(0);
            }
        );

        await activiti.step(
            'And messages: the user gets internal server error when starting a process with message named startMessage and duplicate correlationKey businessId',
            async () => {
                const response = await runtimeBundleServiceHrUser.trySendStartMessage({
                    name: 'startMessage',
                    businessKey: businessId,
                });
                expect(response.httpStatus).toBe(409);
                expect(JSON.stringify(response)).toContain(
                    `Duplicate message subscription 'boundaryMessage' with correlation key '${businessId}'`
                );
            }
        );
    });

    activiti.skip(
        'execute processes using cloud native message events with businessKey correlation',
        async ({
            runtimeBundleServiceTestAdmin,
            queryAdminServiceTestAdmin,
            auditServiceTestAdmin,
        }) => {
            const businessId = randomUUID();

            await activiti.step(
                'When messages: the user sends a start message named StartCloudMessage1 with businessKey value of businessId session variable',
                async () => {
                    const processInstance = await runtimeBundleServiceTestAdmin.sendStartMessage({
                        name: 'StartCloudMessage1',
                        businessKey: businessId,
                    });
                    expect(processInstance.id).toBeTruthy();
                }
            );

            await activiti.step(
                "Then messages: MESSAGE_RECEIVED event is emitted for the message 'StartCloudMessage1'",
                async () => {
                    await expect
                        .poll(
                            async () =>
                                (
                                    await auditServiceTestAdmin.getMessageEventsByDefinitionAndBusinessKey(
                                        'ThrowCatchMessageIT_Process1',
                                        businessId,
                                        EventType.MESSAGE_RECEIVED,
                                        'StartCloudMessage1'
                                    )
                                ).length,
                            pollOptions('querySync')
                        )
                        .toBeGreaterThan(0);
                }
            );

            await activiti.step(
                "And messages: MESSAGE_SENT event is emitted for the message 'StartCloudMessage3'",
                async () => {
                    await expect
                        .poll(
                            async () =>
                                (
                                    await auditServiceTestAdmin.getMessageEventsByDefinitionAndBusinessKey(
                                        'ThrowCatchMessageIT_Process1',
                                        businessId,
                                        EventType.MESSAGE_SENT,
                                        'StartCloudMessage3'
                                    )
                                ).length,
                            pollOptions('querySync')
                        )
                        .toBeGreaterThan(0);
                }
            );

            await activiti.step(
                "And messages: MESSAGE_SENT event is emitted for the message 'StartCloudMessage2'",
                async () => {
                    await expect
                        .poll(
                            async () =>
                                (
                                    await auditServiceTestAdmin.getMessageEventsByDefinitionAndBusinessKey(
                                        'ThrowCatchMessageIT_Process1',
                                        businessId,
                                        EventType.MESSAGE_SENT,
                                        'StartCloudMessage2'
                                    )
                                ).length,
                            pollOptions('querySync')
                        )
                        .toBeGreaterThan(0);
                }
            );

            await activiti.step(
                "And messages: the process with definition key of 'ThrowCatchMessageIT_Process1' having businessKey value of 'businessId' session variable has status 'COMPLETED'",
                async () => {
                    await expect
                        .poll(
                            async () =>
                                queryAdminServiceTestAdmin.getProcessInstanceStatusesByBusinessKey(
                                    'ThrowCatchMessageIT_Process1',
                                    businessId
                                ),
                            pollOptions('querySync')
                        )
                        .toContain(ProcessInstanceStatus.COMPLETED);
                }
            );

            await activiti.step(
                "And messages: MESSAGE_RECEIVED event is emitted for the message 'StartCloudMessage3' for process definition key 'ThrowCatchMessageIT_Process3' having businessKey value of 'businessId' session variable",
                async () => {
                    await expect
                        .poll(
                            async () =>
                                (
                                    await auditServiceTestAdmin.getMessageEventsByDefinitionAndBusinessKey(
                                        'ThrowCatchMessageIT_Process3',
                                        businessId,
                                        EventType.MESSAGE_RECEIVED,
                                        'StartCloudMessage3'
                                    )
                                ).length,
                            pollOptions('querySync')
                        )
                        .toBeGreaterThan(0);
                }
            );

            await activiti.step(
                "And messages: MESSAGE_SENT event is emitted for the message 'IntermediateCloudMessage2' for process definition key 'ThrowCatchMessageIT_Process3' having businessKey value of 'businessId' session variable",
                async () => {
                    await expect
                        .poll(
                            async () =>
                                (
                                    await auditServiceTestAdmin.getMessageEventsByDefinitionAndBusinessKey(
                                        'ThrowCatchMessageIT_Process3',
                                        businessId,
                                        EventType.MESSAGE_SENT,
                                        'IntermediateCloudMessage2'
                                    )
                                ).length,
                            pollOptions('querySync')
                        )
                        .toBeGreaterThan(0);
                }
            );

            await activiti.step(
                "And messages: MESSAGE_WAITING event is emitted for the message 'IntermediateCloudMessage3' for process definition key 'ThrowCatchMessageIT_Process3' having businessKey value of 'businessId' session variable",
                async () => {
                    await expect
                        .poll(
                            async () =>
                                (
                                    await auditServiceTestAdmin.getMessageEventsByDefinitionAndBusinessKey(
                                        'ThrowCatchMessageIT_Process3',
                                        businessId,
                                        EventType.MESSAGE_WAITING,
                                        'IntermediateCloudMessage3'
                                    )
                                ).length,
                            pollOptions('querySync')
                        )
                        .toBeGreaterThan(0);
                }
            );

            await activiti.step(
                "And messages: MESSAGE_RECEIVED event is emitted for the message 'StartCloudMessage2' for process definition key 'ThrowCatchMessageIT_Process2' having businessKey value of 'businessId' session variable",
                async () => {
                    await expect
                        .poll(
                            async () =>
                                (
                                    await auditServiceTestAdmin.getMessageEventsByDefinitionAndBusinessKey(
                                        'ThrowCatchMessageIT_Process2',
                                        businessId,
                                        EventType.MESSAGE_RECEIVED,
                                        'StartCloudMessage2'
                                    )
                                ).length,
                            pollOptions('querySync')
                        )
                        .toBeGreaterThan(0);
                }
            );

            await activiti.step(
                "And messages: MESSAGE_WAITING event is emitted for the message 'IntermediateCloudMessage2' for process definition key 'ThrowCatchMessageIT_Process2' having businessKey value of 'businessId' session variable",
                async () => {
                    await expect
                        .poll(
                            async () =>
                                (
                                    await auditServiceTestAdmin.getMessageEventsByDefinitionAndBusinessKey(
                                        'ThrowCatchMessageIT_Process2',
                                        businessId,
                                        EventType.MESSAGE_WAITING,
                                        'IntermediateCloudMessage2'
                                    )
                                ).length,
                            pollOptions('querySync')
                        )
                        .toBeGreaterThan(0);
                }
            );

            await activiti.step(
                "And messages: MESSAGE_SENT event is emitted for the message 'IntermediateCloudMessage3' for process definition key 'ThrowCatchMessageIT_Process2' having businessKey value of 'businessId' session variable",
                async () => {
                    await expect
                        .poll(
                            async () =>
                                (
                                    await auditServiceTestAdmin.getMessageEventsByDefinitionAndBusinessKey(
                                        'ThrowCatchMessageIT_Process2',
                                        businessId,
                                        EventType.MESSAGE_SENT,
                                        'IntermediateCloudMessage3'
                                    )
                                ).length,
                            pollOptions('querySync')
                        )
                        .toBeGreaterThan(0);
                }
            );

            await activiti.step(
                "And messages: MESSAGE_RECEIVED event is emitted for the message 'IntermediateCloudMessage3' for process definition key 'ThrowCatchMessageIT_Process3' having businessKey value of 'businessId' session variable",
                async () => {
                    await expect
                        .poll(
                            async () =>
                                (
                                    await auditServiceTestAdmin.getMessageEventsByDefinitionAndBusinessKey(
                                        'ThrowCatchMessageIT_Process3',
                                        businessId,
                                        EventType.MESSAGE_RECEIVED,
                                        'IntermediateCloudMessage3'
                                    )
                                ).length,
                            pollOptions('querySync')
                        )
                        .toBeGreaterThan(0);
                }
            );

            await activiti.step(
                "And messages: the process with definition key of 'ThrowCatchMessageIT_Process2' having businessKey value of 'businessId' session variable has status 'COMPLETED'",
                async () => {
                    await expect
                        .poll(
                            async () =>
                                queryAdminServiceTestAdmin.getProcessInstanceStatusesByBusinessKey(
                                    'ThrowCatchMessageIT_Process2',
                                    businessId
                                ),
                            pollOptions('querySync')
                        )
                        .toContain(ProcessInstanceStatus.COMPLETED);
                }
            );

            await activiti.step(
                "And messages: the process with definition key of 'ThrowCatchMessageIT_Process3' having businessKey value of 'businessId' session variable has status 'COMPLETED'",
                async () => {
                    await expect
                        .poll(
                            async () =>
                                queryAdminServiceTestAdmin.getProcessInstanceStatusesByBusinessKey(
                                    'ThrowCatchMessageIT_Process3',
                                    businessId
                                ),
                            pollOptions('querySync')
                        )
                        .toContain(ProcessInstanceStatus.COMPLETED);
                }
            );
        }
    );
});
