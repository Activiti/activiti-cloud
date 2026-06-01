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
import { TaskStatus } from '../../models/task.models';

const ERROR_BOUNDARY_EVENT_SUBPROCESS = 'errorBoundaryEventSubProcess';
const ERROR_START_EVENT_SUBPROCESS = 'errorStartEventSubProcess';
const ERROR_BOUNDARY_EVENT_CALLACTIVITY = 'catchErrorOnCallActivity';

const scenarios: Array<{ title: string; processDefinitionKey: string; skip?: string }> = [
    {
        title: 'check a process instance with boundary error event for subprocess',
        processDefinitionKey: ERROR_BOUNDARY_EVENT_SUBPROCESS,
        // FIXME: In the current example-runtime-bundle image the deployed process definition for
        // `errorBoundaryEventSubProcess` has a raw UUID stored in `act_re_procdef.id_` instead of
        // the canonical `<key>:<version>:<uuid>` form used by the other two scenarios. As a
        // consequence, every audit event for this process gets a malformed `processDefinitionId`,
        // the ACL `LIKE` predicate in ApplicationProcessDefSecuritySpecification never matches and
        // the audit endpoint returns no events for `hruser`. Re-enable once the upstream image
        // stores the canonical processDefinitionId for this BPMN.
        skip: 'upstream image stores malformed processDefinitionId for errorBoundaryEventSubProcess',
    },
    {
        title: 'check a process instance with start error event for subprocess',
        processDefinitionKey: ERROR_START_EVENT_SUBPROCESS,
    },
    {
        title: 'check a process instance with boundary error event for callactivitiy',
        processDefinitionKey: ERROR_BOUNDARY_EVENT_CALLACTIVITY,
    },
];

activiti.describe('Process Instance Error Events Actions', { tag: '@slow' }, () => {
    for (const scenario of scenarios) {
        const testFn = scenario.skip ? activiti.skip : activiti;
        testFn(scenario.title, async ({
            runtimeBundleServiceHrUser,
            auditServiceHrUser,
            taskServiceHrUser,
        }) => {
            let processInstanceId: string;
            let processDefinitionId: string;
            let processDefinitionKey: string;
            let businessKey: string | undefined;

            await activiti.step(
                `When the user starts a process with error events called ${scenario.processDefinitionKey}`,
                async () => {
                    const processInstance = await runtimeBundleServiceHrUser.startProcess({
                        processDefinitionKey: scenario.processDefinitionKey,
                    });
                    processInstanceId = processInstance.id;
                    processDefinitionId = processInstance.processDefinitionId;
                    processDefinitionKey = processInstance.processDefinitionKey;
                    businessKey = processInstance.businessKey;
                    expect(processInstanceId).toBeTruthy();
                }
            );

            await activiti.step('Then error events are emitted for the process', async () => {
                await expect
                    .poll(async () => {
                        const events = await auditServiceHrUser.getEventsByProcessInstanceId(
                            processInstanceId
                        );

                        return events.some((event) => {
                            if (event.eventType !== EventType.ERROR_RECEIVED) {
                                return false;
                            }
                            const entity = event.entity as
                                | {
                                      processDefinitionId?: string;
                                      processInstanceId?: string;
                                      errorCode?: string;
                                      errorId?: string;
                                      activityType?: string | null;
                                      activityName?: string | null;
                                  }
                                | undefined;
                            return (
                                event.processDefinitionId === processDefinitionId &&
                                event.processInstanceId === processInstanceId &&
                                event.processDefinitionKey === processDefinitionKey &&
                                event.businessKey === businessKey &&
                                entity?.processDefinitionId === processDefinitionId &&
                                entity?.processInstanceId === processInstanceId &&
                                entity?.errorCode === '123' &&
                                entity?.errorId === 'errorId' &&
                                (entity?.activityType ?? null) === null &&
                                (entity?.activityName ?? null) === null
                            );
                        });
                    }, pollOptions('querySync'))
                    .toBe(true);
            });

            await activiti.step("And the user can see a task 'Task' with a status CREATED", async () => {
                await expect
                    .poll(async () => {
                        const tasks = await taskServiceHrUser.getTasksByProcessInstanceId(
                            processInstanceId
                        );
                        return tasks.some(
                            (task) => task.name === 'Task' && task.status === TaskStatus.CREATED
                        );
                    }, pollOptions('querySync'))
                    .toBe(true);
            });

            await activiti.step('And the user deletes the process with error events', async () => {
                await runtimeBundleServiceHrUser.deleteProcessInstance(processInstanceId);
            });
        });
    }
});
