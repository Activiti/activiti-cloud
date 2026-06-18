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
import { AuditService } from '../../services/audit.service';
import { QueryAdminService } from '../../services/query-admin.service';

const SESSION_TIMEOUT_MS = 5_000;

const messagePollOptions = (label: 'querySync' | 'auditEvents' = 'querySync') => ({
    ...pollOptions(label),
    timeout: Math.max(pollOptions(label).timeout ?? SESSION_TIMEOUT_MS, SESSION_TIMEOUT_MS),
});

async function expectMessageEventEmitted(
    auditService: AuditService,
    processInstanceId: string,
    eventType: EventType,
    messageName: string
): Promise<void> {
    await expect
        .poll(async () => {
            const events = await auditService.getEvents({ processInstanceId, eventType });
            return events.some((event) => {
                const entity = event.entity as
                    | { messagePayload?: { name?: string } }
                    | undefined;
                return (
                    event.eventType === eventType &&
                    event.processInstanceId === processInstanceId &&
                    entity?.messagePayload?.name === messageName
                );
            });
        }, messagePollOptions())
        .toBe(true);
}

async function expectMessageEventEmittedForProcessDefinitionKey(
    auditService: AuditService,
    processDefinitionKey: string,
    businessKey: string,
    eventType: EventType,
    messageName: string
): Promise<void> {
    await expect
        .poll(async () => {
            const events = await auditService.getEvents({ processDefinitionKey });
            return events.some((event) => {
                if (event.businessKey !== businessKey || event.eventType !== eventType) {
                    return false;
                }
                const entity = event.entity as
                    | { messagePayload?: { name?: string } }
                    | undefined;
                return entity?.messagePayload?.name === messageName;
            });
        }, messagePollOptions())
        .toBe(true);
}

async function expectProcessInstanceStatusByBusinessKey(
    queryAdminService: QueryAdminService,
    processDefinitionKey: string,
    businessKey: string,
    status: ProcessInstanceStatus
): Promise<void> {
    await expect
        .poll(async () => {
            const instances = await queryAdminService.getProcessInstancesAdminWithParams({
                processDefinitionKey,
            });
            return instances
                .filter((instance) => instance.businessKey === businessKey)
                .map((instance) => instance.status);
        }, messagePollOptions())
        .toContain(status);
}

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
                await expectMessageEventEmitted(
                    auditServiceHrUser,
                    processInstanceId,
                    EventType.MESSAGE_RECEIVED,
                    'startMessage'
                );
            }
        );

        await activiti.step(
            "And messages: MESSAGE_WAITING event is emitted for the message 'boundaryMessage'",
            async () => {
                await expectMessageEventEmitted(
                    auditServiceHrUser,
                    processInstanceId,
                    EventType.MESSAGE_WAITING,
                    'boundaryMessage'
                );
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
                await expectMessageEventEmitted(
                    auditServiceHrUser,
                    processInstanceId,
                    EventType.MESSAGE_RECEIVED,
                    'boundaryMessage'
                );
            }
        );

        await activiti.step(
            "And messages: MESSAGE_WAITING event is emitted for the message 'catchMessage'",
            async () => {
                await expectMessageEventEmitted(
                    auditServiceHrUser,
                    processInstanceId,
                    EventType.MESSAGE_WAITING,
                    'catchMessage'
                );
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
                await expectMessageEventEmitted(
                    auditServiceHrUser,
                    processInstanceId,
                    EventType.MESSAGE_RECEIVED,
                    'catchMessage'
                );
            }
        );

        await activiti.step(
            "And messages: MESSAGE_SENT event is emitted for the message 'endMessage'",
            async () => {
                await expectMessageEventEmitted(
                    auditServiceHrUser,
                    processInstanceId,
                    EventType.MESSAGE_SENT,
                    'endMessage'
                );
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
                await expectMessageEventEmitted(
                    auditServiceHrUser,
                    processInstanceId,
                    EventType.MESSAGE_RECEIVED,
                    'startMessage'
                );
            }
        );

        await activiti.step(
            "And messages: MESSAGE_WAITING event is emitted for the message 'boundaryMessage'",
            async () => {
                await expectMessageEventEmitted(
                    auditServiceHrUser,
                    processInstanceId,
                    EventType.MESSAGE_WAITING,
                    'boundaryMessage'
                );
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
                const body = JSON.stringify(response);
                expect(body).toContain(
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
                await expectMessageEventEmitted(
                    auditServiceHrUser,
                    processInstanceId,
                    EventType.MESSAGE_RECEIVED,
                    'startMessage'
                );
            }
        );

        await activiti.step(
            "And messages: MESSAGE_WAITING event is emitted for the message 'boundaryMessage'",
            async () => {
                await expectMessageEventEmitted(
                    auditServiceHrUser,
                    processInstanceId,
                    EventType.MESSAGE_WAITING,
                    'boundaryMessage'
                );
            }
        );

        await activiti.step(
            'And messages: the user gets internal server error when starting a process with message named startMessage and duplicate correlationKey businessId',
            async () => {
                let caughtStatus: number | undefined;
                let caughtBody = '';
                try {
                    const response = await runtimeBundleServiceHrUser.sendStartMessage({
                        name: 'startMessage',
                        businessKey: businessId,
                    });
                    caughtStatus = (response as unknown as { httpStatus?: number }).httpStatus;
                    caughtBody = JSON.stringify(response);
                } catch (error) {
                    const message = error instanceof Error ? error.message : String(error);
                    caughtBody = message;
                    const match = message.match(/\[(4\d\d|5\d\d)\]/);
                    if (match) {
                        caughtStatus = Number(match[1]);
                    }
                }
                expect(caughtStatus).toBe(409);
                expect(caughtBody).toContain(
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
                    await expectMessageEventEmittedForProcessDefinitionKey(
                        auditServiceTestAdmin,
                        'ThrowCatchMessageIT_Process1',
                        businessId,
                        EventType.MESSAGE_RECEIVED,
                        'StartCloudMessage1'
                    );
                }
            );

            await activiti.step(
                "And messages: MESSAGE_SENT event is emitted for the message 'StartCloudMessage3'",
                async () => {
                    await expectMessageEventEmittedForProcessDefinitionKey(
                        auditServiceTestAdmin,
                        'ThrowCatchMessageIT_Process1',
                        businessId,
                        EventType.MESSAGE_SENT,
                        'StartCloudMessage3'
                    );
                }
            );

            await activiti.step(
                "And messages: MESSAGE_SENT event is emitted for the message 'StartCloudMessage2'",
                async () => {
                    await expectMessageEventEmittedForProcessDefinitionKey(
                        auditServiceTestAdmin,
                        'ThrowCatchMessageIT_Process1',
                        businessId,
                        EventType.MESSAGE_SENT,
                        'StartCloudMessage2'
                    );
                }
            );

            await activiti.step(
                "And messages: the process with definition key of 'ThrowCatchMessageIT_Process1' having businessKey value of 'businessId' session variable has status 'COMPLETED'",
                async () => {
                    await expectProcessInstanceStatusByBusinessKey(
                        queryAdminServiceTestAdmin,
                        'ThrowCatchMessageIT_Process1',
                        businessId,
                        ProcessInstanceStatus.COMPLETED
                    );
                }
            );

            await activiti.step(
                "And messages: MESSAGE_RECEIVED event is emitted for the message 'StartCloudMessage3' for process definition key 'ThrowCatchMessageIT_Process3' having businessKey value of 'businessId' session variable",
                async () => {
                    await expectMessageEventEmittedForProcessDefinitionKey(
                        auditServiceTestAdmin,
                        'ThrowCatchMessageIT_Process3',
                        businessId,
                        EventType.MESSAGE_RECEIVED,
                        'StartCloudMessage3'
                    );
                }
            );

            await activiti.step(
                "And messages: MESSAGE_SENT event is emitted for the message 'IntermediateCloudMessage2' for process definition key 'ThrowCatchMessageIT_Process3' having businessKey value of 'businessId' session variable",
                async () => {
                    await expectMessageEventEmittedForProcessDefinitionKey(
                        auditServiceTestAdmin,
                        'ThrowCatchMessageIT_Process3',
                        businessId,
                        EventType.MESSAGE_SENT,
                        'IntermediateCloudMessage2'
                    );
                }
            );

            await activiti.step(
                "And messages: MESSAGE_WAITING event is emitted for the message 'IntermediateCloudMessage3' for process definition key 'ThrowCatchMessageIT_Process3' having businessKey value of 'businessId' session variable",
                async () => {
                    await expectMessageEventEmittedForProcessDefinitionKey(
                        auditServiceTestAdmin,
                        'ThrowCatchMessageIT_Process3',
                        businessId,
                        EventType.MESSAGE_WAITING,
                        'IntermediateCloudMessage3'
                    );
                }
            );

            await activiti.step(
                "And messages: MESSAGE_RECEIVED event is emitted for the message 'StartCloudMessage2' for process definition key 'ThrowCatchMessageIT_Process2' having businessKey value of 'businessId' session variable",
                async () => {
                    await expectMessageEventEmittedForProcessDefinitionKey(
                        auditServiceTestAdmin,
                        'ThrowCatchMessageIT_Process2',
                        businessId,
                        EventType.MESSAGE_RECEIVED,
                        'StartCloudMessage2'
                    );
                }
            );

            await activiti.step(
                "And messages: MESSAGE_WAITING event is emitted for the message 'IntermediateCloudMessage2' for process definition key 'ThrowCatchMessageIT_Process2' having businessKey value of 'businessId' session variable",
                async () => {
                    await expectMessageEventEmittedForProcessDefinitionKey(
                        auditServiceTestAdmin,
                        'ThrowCatchMessageIT_Process2',
                        businessId,
                        EventType.MESSAGE_WAITING,
                        'IntermediateCloudMessage2'
                    );
                }
            );

            await activiti.step(
                "And messages: MESSAGE_SENT event is emitted for the message 'IntermediateCloudMessage3' for process definition key 'ThrowCatchMessageIT_Process2' having businessKey value of 'businessId' session variable",
                async () => {
                    await expectMessageEventEmittedForProcessDefinitionKey(
                        auditServiceTestAdmin,
                        'ThrowCatchMessageIT_Process2',
                        businessId,
                        EventType.MESSAGE_SENT,
                        'IntermediateCloudMessage3'
                    );
                }
            );

            await activiti.step(
                "And messages: MESSAGE_RECEIVED event is emitted for the message 'IntermediateCloudMessage3' for process definition key 'ThrowCatchMessageIT_Process3' having businessKey value of 'businessId' session variable",
                async () => {
                    await expectMessageEventEmittedForProcessDefinitionKey(
                        auditServiceTestAdmin,
                        'ThrowCatchMessageIT_Process3',
                        businessId,
                        EventType.MESSAGE_RECEIVED,
                        'IntermediateCloudMessage3'
                    );
                }
            );

            await activiti.step(
                "And messages: the process with definition key of 'ThrowCatchMessageIT_Process2' having businessKey value of 'businessId' session variable has status 'COMPLETED'",
                async () => {
                    await expectProcessInstanceStatusByBusinessKey(
                        queryAdminServiceTestAdmin,
                        'ThrowCatchMessageIT_Process2',
                        businessId,
                        ProcessInstanceStatus.COMPLETED
                    );
                }
            );

            await activiti.step(
                "And messages: the process with definition key of 'ThrowCatchMessageIT_Process3' having businessKey value of 'businessId' session variable has status 'COMPLETED'",
                async () => {
                    await expectProcessInstanceStatusByBusinessKey(
                        queryAdminServiceTestAdmin,
                        'ThrowCatchMessageIT_Process3',
                        businessId,
                        ProcessInstanceStatus.COMPLETED
                    );
                }
            );
        }
    );
});
