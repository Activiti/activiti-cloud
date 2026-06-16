/*
 * Copyright 2017-2020 Alfresco Software, Ltd.
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
import { ProcessInstanceStatus } from '../../models/runtime-bundle.models';

const RANK_MOVIE_PROCESS = 'RankMovieId';
const MI_PARALLEL_CLOUD_CONNECTOR_PROCESS = 'miParallelCloudConnector';
const TEST_ERROR_CONNECTOR_PROCESS = 'testErrorConnectorProcess';
const TEST_BPMN_ERROR_CONNECTOR_PROCESS = 'testBpmnErrorConnectorProcess';

activiti.describe('Process Instance Connectors Actions', { tag: '@slow' }, () => {
    activiti('Start a process containing cloud connector', async ({
        runtimeBundleServiceTestUser,
        taskServiceTestUser,
    }) => {
        const movieToRank = 'The Lord of The Rings';
        const expectedMovieDesc =
            'The Lord of the Rings is an epic high fantasy novel written by English author and scholar J. R. R. Tolkien';
        let processInstanceId: string;

        await activiti.step(
            'When the user starts an instance of process called RankMovieId with the provided variables',
            async () => {
                const processInstance = await runtimeBundleServiceTestUser.startProcessWithVariables(
                    RANK_MOVIE_PROCESS,
                    { movieToRank }
                );
                processInstanceId = processInstance.id;
                expect(processInstanceId).toBeTruthy();
            }
        );

        await activiti.step(
            'Then the process instance has a variable named movieToRank with value The Lord of The Rings',
            async () => {
                await expect
                    .poll(async () => {
                        const variables = await runtimeBundleServiceTestUser.getProcessInstanceVariables(
                            processInstanceId
                        );
                        const match = variables.find((variable) => variable.name === 'movieToRank');
                        return match ? String(match.value) : undefined;
                    }, pollOptions('querySync'))
                    .toBe(movieToRank);
            }
        );

        await activiti.step(
            'And the process instance has a variable named movieDesc with value The Lord of the Rings is an epic high fantasy novel written by English author and scholar J. R. R. Tolkien',
            async () => {
                await expect
                    .poll(async () => {
                        const variables = await runtimeBundleServiceTestUser.getProcessInstanceVariables(
                            processInstanceId
                        );
                        const match = variables.find((variable) => variable.name === 'movieDesc');
                        return match ? String(match.value) : undefined;
                    }, pollOptions('querySync'))
                    .toBe(expectedMovieDesc);
            }
        );

        await activiti.step('And the process instance has a task named Add Rating', async () => {
            await expect
                .poll(async () => {
                    const tasks = await taskServiceTestUser.getTasksByProcessInstanceId(processInstanceId);
                    return tasks.some((task) => task.name === 'Add Rating');
                }, pollOptions('querySync'))
                .toBe(true);
        });
    });

    activiti('Complete a process containing multi-instance cloud connector', async ({
        runtimeBundleServiceTestUser,
        queryServiceTestUser,
    }) => {
        const instanceCount = 3;
        let processInstanceId: string;

        await activiti.step(
            'When the user starts an instance of process called miParallelCloudConnector with the provided variables',
            async () => {
                const processInstance = await runtimeBundleServiceTestUser.startProcessWithVariables(
                    MI_PARALLEL_CLOUD_CONNECTOR_PROCESS,
                    { instanceCount }
                );
                processInstanceId = processInstance.id;
                expect(processInstanceId).toBeTruthy();
            }
        );

        await activiti.step(
            'Then the query process instance has an integer variable named instanceCount with value 3',
            async () => {
                await expect
                    .poll(async () => {
                        const variables = await queryServiceTestUser.getProcessInstanceVariables(
                            processInstanceId
                        );
                        const match = variables.find((variable) => variable.name === 'instanceCount');
                        return match ? Number(match.value) : undefined;
                    }, pollOptions('querySync'))
                    .toBe(instanceCount);
            }
        );

        await activiti.step(
            'And the process instance has a resultCollection named miResult with the following integer entries',
            async () => {
                const expectedEntries = [
                    { executionCount: 1 },
                    { executionCount: 2 },
                    { executionCount: 3 },
                ];

                await expect
                    .poll(async () => {
                        const variables = await queryServiceTestUser.getProcessInstanceVariables(
                            processInstanceId
                        );
                        const match = variables.find((variable) => variable.name === 'miResult');
                        if (!match || !Array.isArray(match.value)) {
                            return undefined;
                        }
                        return [...(match.value as Array<Record<string, number>>)].sort(
                            (a, b) => Number(a.executionCount) - Number(b.executionCount)
                        );
                    }, pollOptions('querySync'))
                    .toEqual(expectedEntries);
            }
        );

        await activiti.step('And the status of the process is changed to completed', async () => {
            await expect
                .poll(
                    async () =>
                        (
                            await getQueryProcessInstanceWhenSynced(
                                queryServiceTestUser,
                                processInstanceId
                            )
                        )?.status,
                    pollOptions('querySync')
                )
                .toBe(ProcessInstanceStatus.COMPLETED);
        });
    });

    activiti('Propagate integration error for a process containing cloud connector to audit', async ({
        runtimeBundleServiceTestUser,
        auditServiceTestUser,
    }) => {
        let processInstanceId: string;

        await activiti.step(
            'When the user starts an instance of process called testErrorConnectorProcess with the provided variables',
            async () => {
                const processInstance = await runtimeBundleServiceTestUser.startProcessWithVariables(
                    TEST_ERROR_CONNECTOR_PROCESS,
                    { var: 'test' }
                );
                processInstanceId = processInstance.id;
                expect(processInstanceId).toBeTruthy();
            }
        );

        await activiti.step('Then integration error event is emitted for the process', async () => {
            await expect
                .poll(async () => {
                    const events = await auditServiceTestUser.getEventsByProcessInstanceId(processInstanceId);
                    return events.some(
                        (event) =>
                            event.eventType === EventType.INTEGRATION_ERROR_RECEIVED &&
                            event.errorMessage === 'TestErrorConnector' &&
                            event.errorClassName === 'java.lang.RuntimeException'
                    );
                }, pollOptions('querySync'))
                .toBe(true);
        });
    });

    // FIXME: This scenario relies on audit ACL filtering by `processDefinitionId LIKE '<key>:%'`,
    // but in the current example-runtime-bundle image the deployed process definition for
    // `testBpmnErrorConnectorProcess` has a raw UUID stored in `act_re_procdef.id_` (instead of
    // the canonical `<key>:<version>:<uuid>` form used by all other processes, including
    // `testErrorConnectorProcess`). As a consequence, every audit event for this process gets a
    // malformed `processDefinitionId`, the ACL `LIKE` predicate never matches and the audit
    // endpoint returns no events for `testuser` — even though the events exist in the DB and the
    // RB has correctly received the CloudBpmnError integration error. Re-enable once the upstream
    // image stores the canonical processDefinitionId for this BPMN.
    activiti.skip('Propagate cloud bpmn error for a process containing cloud connector to audit', async ({
        runtimeBundleServiceTestUser,
        queryServiceTestUser,
        auditServiceTestUser,
    }) => {
        let processInstanceId: string;
        let processDefinitionId: string;
        let processDefinitionKey: string;
        let businessKey: string | undefined;

        await activiti.step(
            'When the user starts an instance of process called testBpmnErrorConnectorProcess with the provided variables',
            async () => {
                const processInstance = await runtimeBundleServiceTestUser.startProcessWithVariables(
                    TEST_BPMN_ERROR_CONNECTOR_PROCESS,
                    { var: 'test' }
                );
                processInstanceId = processInstance.id;
                processDefinitionId = processInstance.processDefinitionId;
                processDefinitionKey = processInstance.processDefinitionKey;
                businessKey = processInstance.businessKey;
                expect(processInstanceId).toBeTruthy();
            }
        );

        await activiti.step('Then cloud bpmn error event is emitted for the process', async () => {
            await expect
                .poll(async () => {
                    const events = await auditServiceTestUser.getEventsByProcessInstanceId(processInstanceId);

                    const hasBpmnErrorReceived = events.some((event) => {
                        if (event.eventType !== EventType.ERROR_RECEIVED) {
                            return false;
                        }
                        const entity = event.entity as
                            | {
                                  processDefinitionId?: string;
                                  processInstanceId?: string;
                                  errorCode?: string;
                                  errorId?: string;
                              }
                            | undefined;
                        return (
                            event.processDefinitionId === processDefinitionId &&
                            event.processInstanceId === processInstanceId &&
                            event.processDefinitionKey === processDefinitionKey &&
                            event.businessKey === businessKey &&
                            entity?.processDefinitionId === processDefinitionId &&
                            entity?.processInstanceId === processInstanceId &&
                            entity?.errorCode === 'CLOUD_BPMN_ERROR' &&
                            entity?.errorId === 'CLOUD_BPMN_ERROR'
                        );
                    });

                    const hasIntegrationError = events.some(
                        (event) =>
                            event.eventType === EventType.INTEGRATION_ERROR_RECEIVED &&
                            event.errorMessage === 'CLOUD_BPMN_ERROR' &&
                            event.errorClassName ===
                                'org.activiti.cloud.api.process.model.CloudBpmnError'
                    );

                    return hasBpmnErrorReceived && hasIntegrationError;
                }, pollOptions('querySync'))
                .toBe(true);
        });

        await activiti.step('And the status of the process is changed to cancelled', async () => {
            await expect(async () => {
                await runtimeBundleServiceTestUser.getProcessInstance(processInstanceId);
            }).rejects.toThrow();

            await expect
                .poll(
                    async () =>
                        (
                            await getQueryProcessInstanceWhenSynced(
                                queryServiceTestUser,
                                processInstanceId
                            )
                        )?.status,
                    pollOptions('querySync')
                )
                .toBe(ProcessInstanceStatus.CANCELLED);

            await expect
                .poll(async () => {
                    const events = await auditServiceTestUser.getEventsByProcessInstanceId(processInstanceId);
                    return events.some((event) => event.eventType === EventType.PROCESS_CANCELLED);
                }, pollOptions('auditEvents'))
                .toBe(true);
        });
    });
});
