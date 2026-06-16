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
import { getQueryProcessInstanceAdminWhenSynced, loadOrUndefined } from '../../helpers/query-sync';
import { EventType } from '../../models/audit.models';
import {
    CloudProcessInstance,
    IntegrationContextStatus,
    ProcessInstanceStatus,
    ServiceTaskStatus,
} from '../../models/runtime-bundle.models';

const CONNECTOR_PROCESS = 'ConnectorProcess';
const CONNECTOR_PROCESS_WITH_LOOP = 'ConnectorProcessWithLoop';
const TEST_BPMN_ERROR_CONNECTOR_PROCESS = 'testBpmnErrorConnectorProcess';
const TEST_ERROR_CONNECTOR_PROCESS = 'testErrorConnectorProcess';

activiti.describe('Process Instance Service Tasks Actions', { tag: '@slow' }, () => {
    activiti('audit service tasks integration context events for process instance', async ({
        runtimeBundleServiceTestAdmin,
        queryAdminServiceTestAdmin,
        auditServiceTestAdmin,
    }) => {
        let processInstance: CloudProcessInstance;

        await activiti.step(
            'When the user starts a process with service tasks called CONNECTOR_PROCESS_INSTANCE',
            async () => {
                processInstance = await runtimeBundleServiceTestAdmin.startProcess({ processDefinitionKey: CONNECTOR_PROCESS });
                expect(processInstance.id).toBeTruthy();
            }
        );

        await activiti.step('Then integration context events are emitted for the process', async () => {
            await expect
                .poll(
                    async () =>
                        (
                            await auditServiceTestAdmin.getIntegrationContextEvents(
                                processInstance.id
                            )
                        )
                            .map((event) => event.eventType)
                            .sort(),
                    pollOptions('querySync')
                )
                .toEqual(
                    [
                        EventType.INTEGRATION_REQUESTED,
                        EventType.INTEGRATION_RESULT_RECEIVED,
                    ].sort()
                );
        });

        await activiti.step('And the process with service tasks is completed', async () => {
            await expect
                .poll(
                    async () =>
                        (
                            await getQueryProcessInstanceAdminWhenSynced(
                                queryAdminServiceTestAdmin,
                                processInstance.id
                            )
                        )?.status,
                    pollOptions('querySync')
                )
                .toBe(ProcessInstanceStatus.COMPLETED);
        });
    });

    activiti('get service tasks for process instance', async ({
        runtimeBundleServiceTestAdmin,
        queryAdminServiceTestAdmin,
    }) => {
        let processInstance: CloudProcessInstance;

        await activiti.step(
            'When the user starts a process with service tasks called CONNECTOR_PROCESS_INSTANCE',
            async () => {
                processInstance = await runtimeBundleServiceTestAdmin.startProcess({ processDefinitionKey: CONNECTOR_PROCESS });
                expect(processInstance.id).toBeTruthy();
            }
        );

        await activiti.step('Then the user can get list of service tasks for process instance', async () => {
            await expect
                .poll(async () => {
                    const tasks = await queryAdminServiceTestAdmin.getServiceTasksForProcessInstance(
                        processInstance.id
                    );
                    return tasks.length > 0 && tasks.every((task) => task.activityType === 'serviceTask');
                }, pollOptions('querySync'))
                .toBe(true);
        });

        await activiti.step('And the process with service tasks is completed', async () => {
            await expect
                .poll(
                    async () =>
                        (
                            await getQueryProcessInstanceAdminWhenSynced(
                                queryAdminServiceTestAdmin,
                                processInstance.id
                            )
                        )?.status,
                    pollOptions('querySync')
                )
                .toBe(ProcessInstanceStatus.COMPLETED);
        });
    });

    activiti('get service task by id', async ({
        runtimeBundleServiceTestAdmin,
        queryAdminServiceTestAdmin,
    }) => {
        let processInstance: CloudProcessInstance;

        await activiti.step(
            'When the user starts a process with service tasks called CONNECTOR_PROCESS_INSTANCE',
            async () => {
                processInstance = await runtimeBundleServiceTestAdmin.startProcess({ processDefinitionKey: CONNECTOR_PROCESS });
                expect(processInstance.id).toBeTruthy();
            }
        );

        await activiti.step('Then the user can get service task by id', async () => {
            await expect
                .poll(async () => {
                    const tasks = await queryAdminServiceTestAdmin.getServiceTasksForProcessInstance(
                        processInstance.id
                    );
                    if (tasks.length !== 1) {
                        return false;
                    }
                    const serviceTask = await queryAdminServiceTestAdmin.getServiceTaskById(tasks[0].id);
                    return serviceTask?.activityType === 'serviceTask';
                }, pollOptions('querySync'))
                .toBe(true);
        });

        await activiti.step('And the process with service tasks is completed', async () => {
            await expect
                .poll(
                    async () =>
                        (
                            await getQueryProcessInstanceAdminWhenSynced(
                                queryAdminServiceTestAdmin,
                                processInstance.id
                            )
                        )?.status,
                    pollOptions('querySync')
                )
                .toBe(ProcessInstanceStatus.COMPLETED);
        });
    });

    activiti('get service task integration context by service task id', async ({
        runtimeBundleServiceTestAdmin,
        queryAdminServiceTestAdmin,
    }) => {
        let processInstance: CloudProcessInstance;

        await activiti.step(
            'When the user starts a process with service tasks called CONNECTOR_PROCESS_INSTANCE',
            async () => {
                processInstance = await runtimeBundleServiceTestAdmin.startProcess({ processDefinitionKey: CONNECTOR_PROCESS });
                expect(processInstance.id).toBeTruthy();
            }
        );

        await activiti.step(
            'Then the user can get service task integration context by service task id',
            async () => {
                await expect
                    .poll(async () => {
                        const tasks = await queryAdminServiceTestAdmin.getServiceTasksForProcessInstance(
                            processInstance.id
                        );
                        if (tasks.length !== 1) {
                            return false;
                        }
                        const integrationContext = await loadOrUndefined(() =>
                            queryAdminServiceTestAdmin.getServiceTaskIntegrationContext(tasks[0].id)
                        );
                        return (
                            integrationContext?.clientType === 'ServiceTask' &&
                            integrationContext?.status ===
                                IntegrationContextStatus.INTEGRATION_RESULT_RECEIVED
                        );
                    }, pollOptions('querySync'))
                    .toBe(true);
            }
        );

        await activiti.step('And the process with service tasks is completed', async () => {
            await expect
                .poll(
                    async () =>
                        (
                            await getQueryProcessInstanceAdminWhenSynced(
                                queryAdminServiceTestAdmin,
                                processInstance.id
                            )
                        )?.status,
                    pollOptions('querySync')
                )
                .toBe(ProcessInstanceStatus.COMPLETED);
        });
    });

    activiti('get service task all integration contexts by service task id', async ({
        runtimeBundleServiceTestAdmin,
        queryAdminServiceTestAdmin,
    }) => {
        let processInstance: CloudProcessInstance;

        await activiti.step(
            'Given the user provides an integer variable named executionCount with value 0 ' +
                'When the user starts a process with service tasks called CONNECTOR_PROCESS_WITH_LOOP',
            async () => {
                processInstance = await runtimeBundleServiceTestAdmin.startProcessWithVariables(
                    CONNECTOR_PROCESS_WITH_LOOP,
                    { executionCount: 0 }
                );
                expect(processInstance.id).toBeTruthy();
            }
        );

        await activiti.step('And the service task is executed two times', async () => {
            await expect
                .poll(async () => {
                    const tasks = await queryAdminServiceTestAdmin.getServiceTasksForProcessInstance(
                        processInstance.id
                    );
                    if (tasks.length !== 1) {
                        return false;
                    }
                    return tasks[0].integrationContextCounter === 2;
                }, pollOptions('querySync'))
                .toBe(true);
        });

        await activiti.step(
            'Then the user can get all service task integration contexts by service task id',
            async () => {
                await expect
                    .poll(async () => {
                        const tasks = await queryAdminServiceTestAdmin.getServiceTasksForProcessInstance(
                            processInstance.id
                        );
                        if (tasks.length !== 1) {
                            return false;
                        }
                        const serviceTask = tasks[0];
                        if (serviceTask.integrationContextCounter !== 2) {
                            return false;
                        }
                        const contexts = await loadOrUndefined(() =>
                            queryAdminServiceTestAdmin.getServiceTaskIntegrationContexts(serviceTask.id)
                        );
                        if (!contexts || contexts.length !== 2) {
                            return false;
                        }
                        return contexts.some(
                            (ctx) =>
                                ctx.clientType === 'ServiceTask' &&
                                ctx.status === IntegrationContextStatus.INTEGRATION_RESULT_RECEIVED
                        );
                    }, pollOptions('querySync'))
                    .toBe(true);
            }
        );

        await activiti.step('And the process with service tasks is completed', async () => {
            await expect
                .poll(
                    async () =>
                        (
                            await getQueryProcessInstanceAdminWhenSynced(
                                queryAdminServiceTestAdmin,
                                processInstance.id
                            )
                        )?.status,
                    pollOptions('querySync')
                )
                .toBe(ProcessInstanceStatus.COMPLETED);
        });
    });

    activiti('get service tasks by COMPLETED status for process instance', async ({
        runtimeBundleServiceTestAdmin,
        queryAdminServiceTestAdmin,
    }) => {
        let processInstance: CloudProcessInstance;

        await activiti.step(
            'When the user starts a process with service tasks called CONNECTOR_PROCESS_INSTANCE',
            async () => {
                processInstance = await runtimeBundleServiceTestAdmin.startProcess({ processDefinitionKey: CONNECTOR_PROCESS });
                expect(processInstance.id).toBeTruthy();
            }
        );

        await activiti.step(
            'Then the user can get list of service tasks with status of COMPLETED',
            async () => {
                await expect
                    .poll(async () => {
                        const tasks = await queryAdminServiceTestAdmin.getServiceTasksByStatusForProcessInstance(
                            processInstance.id,
                            ServiceTaskStatus.COMPLETED
                        );
                        return (
                            tasks.length > 0 &&
                            tasks.every(
                                (task) =>
                                    task.activityType === 'serviceTask' &&
                                    task.status === ServiceTaskStatus.COMPLETED
                            )
                        );
                    }, pollOptions('querySync'))
                    .toBe(true);
            }
        );

        await activiti.step('And the process with service tasks is completed', async () => {
            await expect
                .poll(
                    async () =>
                        (
                            await getQueryProcessInstanceAdminWhenSynced(
                                queryAdminServiceTestAdmin,
                                processInstance.id
                            )
                        )?.status,
                    pollOptions('querySync')
                )
                .toBe(ProcessInstanceStatus.COMPLETED);
        });
    });

    // FIXME: Blocked by upstream BPMN deployment bug — `testBpmnErrorConnectorProcess` is stored
    // with a raw UUID in `act_re_procdef.id_` instead of the canonical `<key>:<version>:<uuid>`
    // form, so the audit/query ACL filter `processDefinitionId LIKE '<key>:%'` rejects all events
    // and service-task rows for this definition even though they are written to the DB. Re-enable
    // once the upstream image is fixed.
    activiti.skip('get service tasks by ERROR status for process instance', async ({
        runtimeBundleServiceTestAdmin,
        queryAdminServiceTestAdmin,
        auditServiceTestAdmin,
    }) => {
        let processInstance: CloudProcessInstance;

        await activiti.step(
            'Given the user provides a variable named var with value test ' +
                'When the user starts a process with service tasks called BPMN_ERROR_CONNECTOR_PROCESS',
            async () => {
                processInstance = await runtimeBundleServiceTestAdmin.startProcessWithVariables(
                    TEST_BPMN_ERROR_CONNECTOR_PROCESS,
                    { var: 'test' }
                );
                expect(processInstance.id).toBeTruthy();
            }
        );

        await activiti.step('Then integration context error events are emitted for the process', async () => {
            await expect
                .poll(
                    async () =>
                        (
                            await auditServiceTestAdmin.getIntegrationContextEvents(
                                processInstance.id
                            )
                        )
                            .map((event) => event.eventType)
                            .sort(),
                    pollOptions('querySync')
                )
                .toEqual(
                    [
                        EventType.INTEGRATION_REQUESTED,
                        EventType.INTEGRATION_ERROR_RECEIVED,
                    ].sort()
                );
        });

        await activiti.step('And the user can get list of service tasks with status of ERROR', async () => {
            await expect
                .poll(async () => {
                    const tasks = await queryAdminServiceTestAdmin.getServiceTasksByStatusForProcessInstance(
                        processInstance.id,
                        ServiceTaskStatus.ERROR
                    );
                    return (
                        tasks.length > 0 &&
                        tasks.every(
                            (task) =>
                                task.activityType === 'serviceTask' &&
                                task.status === ServiceTaskStatus.ERROR
                        )
                    );
                }, pollOptions('querySync'))
                .toBe(true);
        });

        await activiti.step('And the status of the process is changed to cancelled', async () => {
            await expect
                .poll(async () => {
                    const instances = await queryAdminServiceTestAdmin.getProcessInstancesAdminWithParams({
                        processDefinitionKey: TEST_BPMN_ERROR_CONNECTOR_PROCESS,
                    });
                    const match = instances.find((i) => i.id === processInstance.id);
                    return match?.status;
                }, pollOptions('querySync'))
                .toBe(ProcessInstanceStatus.CANCELLED);
        });
    });

    activiti('get all completed service tasks by query', async ({
        runtimeBundleServiceTestAdmin,
        queryAdminServiceTestAdmin,
        queryServiceTestAdmin,
    }) => {
        let processInstance: CloudProcessInstance;

        await activiti.step(
            'When the user starts a process with service tasks called CONNECTOR_PROCESS_INSTANCE',
            async () => {
                processInstance = await runtimeBundleServiceTestAdmin.startProcess({ processDefinitionKey: CONNECTOR_PROCESS });
                expect(processInstance.id).toBeTruthy();
            }
        );

        await activiti.step(
            'Then the user can get list of service tasks for process key ConnectorProcess and status COMPLETED',
            async () => {
                const processDefinitions = await queryServiceTestAdmin.getProcessDefinitions();
                const processDefinition = processDefinitions.find((d) => d.key === CONNECTOR_PROCESS);
                expect(processDefinition?.id).toBeTruthy();

                await expect
                    .poll(async () => {
                        const tasks = await queryAdminServiceTestAdmin.getServiceTasksByQuery({
                            processDefinitionKey: CONNECTOR_PROCESS,
                            status: ServiceTaskStatus.COMPLETED,
                        });
                        return (
                            tasks.length > 0 &&
                            tasks.every(
                                (task) =>
                                    task.processDefinitionId === processDefinition?.id &&
                                    task.processDefinitionKey === CONNECTOR_PROCESS &&
                                    task.activityType === 'serviceTask' &&
                                    task.status === ServiceTaskStatus.COMPLETED
                            )
                        );
                    }, pollOptions('querySync'))
                    .toBe(true);
            }
        );
    });

    // FIXME: Same upstream BPMN deployment bug as `get service tasks by ERROR status for process
    // instance`: `testBpmnErrorConnectorProcess` is deployed with a raw UUID in
    // `act_re_procdef.id_`, so audit/query ACL filtering excludes its events and service-task
    // rows. Re-enable once the upstream image emits the canonical processDefinitionId.
    activiti.skip('get all error service tasks by query', async ({
        runtimeBundleServiceTestAdmin,
        queryAdminServiceTestAdmin,
    }) => {
        let processInstance: CloudProcessInstance;

        await activiti.step(
            'When the user starts a process with service tasks called BPMN_ERROR_CONNECTOR_PROCESS',
            async () => {
                processInstance = await runtimeBundleServiceTestAdmin.startProcessWithVariables(
                    TEST_BPMN_ERROR_CONNECTOR_PROCESS,
                    { var: 'test' }
                );
                expect(processInstance.id).toBeTruthy();
            }
        );

        await activiti.step(
            'Then the user can get list of service tasks for process key testBpmnErrorConnectorProcess and status ERROR',
            async () => {
                await expect
                    .poll(async () => {
                        const tasks = await queryAdminServiceTestAdmin.getServiceTasksByQuery({
                            processDefinitionKey: TEST_BPMN_ERROR_CONNECTOR_PROCESS,
                            status: ServiceTaskStatus.ERROR,
                        });
                        return (
                            tasks.length > 0 &&
                            tasks.every(
                                (task) =>
                                    task.processDefinitionKey === TEST_BPMN_ERROR_CONNECTOR_PROCESS &&
                                    task.activityType === 'serviceTask' &&
                                    task.status === ServiceTaskStatus.ERROR
                            )
                        );
                    }, pollOptions('querySync'))
                    .toBe(true);
            }
        );
    });

    activiti('replay service task execution with ERROR status', async ({
        runtimeBundleServiceTestAdmin,
        runtimeAdminServiceTestAdmin,
        queryAdminServiceTestAdmin,
        auditServiceTestAdmin,
    }) => {
        let processInstance: CloudProcessInstance;

        await activiti.step(
            'Given the user provides a variable named var with value test ' +
                'When the user starts an instance of process called testErrorConnectorProcess with the provided variables',
            async () => {
                processInstance = await runtimeBundleServiceTestAdmin.startProcessWithVariables(
                    TEST_ERROR_CONNECTOR_PROCESS,
                    { var: 'test' }
                );
                expect(processInstance.id).toBeTruthy();
            }
        );

        await activiti.step('And integration error event is emitted for the process', async () => {
            await expect
                .poll(async () => {
                    const events = await auditServiceTestAdmin.getEventsByProcessInstanceId(
                        processInstance.id
                    );
                    return events.some(
                        (event) => event.eventType === EventType.INTEGRATION_ERROR_RECEIVED
                    );
                }, pollOptions('querySync'))
                .toBe(true);
        });

        await activiti.step('And the user can get list of service tasks with status of ERROR', async () => {
            await expect
                .poll(async () => {
                    const tasks = await queryAdminServiceTestAdmin.getServiceTasksByStatusForProcessInstance(
                        processInstance.id,
                        ServiceTaskStatus.ERROR
                    );
                    return (
                        tasks.length > 0 &&
                        tasks.every((task) => task.status === ServiceTaskStatus.ERROR)
                    );
                }, pollOptions('querySync'))
                .toBe(true);
        });

        await activiti.step('Then the user set the instance variable var with value replay', async () => {
            await runtimeBundleServiceTestAdmin.setProcessVariables(processInstance.id, { var: 'replay' });
        });

        await activiti.step('And the user can replay service task execution', async () => {
            const events = await auditServiceTestAdmin.getEventsByProcessInstanceId(
                processInstance.id
            );
            const errorEvent = events.find(
                (event) => event.eventType === EventType.INTEGRATION_ERROR_RECEIVED
            );
            const entity = errorEvent?.entity as
                | { executionId?: string; clientId?: string }
                | undefined;
            expect(entity?.executionId).toBeTruthy();
            expect(entity?.clientId).toBeTruthy();
            const response = await runtimeAdminServiceTestAdmin.replayServiceTask(
                entity!.executionId!,
                entity!.clientId!
            );
            expect(response.httpStatus).toBeGreaterThanOrEqual(200);
            expect(response.httpStatus).toBeLessThan(300);
        });

        await activiti.step('And the user can get list of service tasks with status of STARTED', async () => {
            await expect
                .poll(async () => {
                    const tasks = await queryAdminServiceTestAdmin.getServiceTasksByStatusForProcessInstance(
                        processInstance.id,
                        ServiceTaskStatus.STARTED
                    );
                    return tasks.length > 0;
                }, pollOptions('querySync'))
                .toBe(true);
        });

        await activiti.step('And all integration context events are emitted for the process', async () => {
            await expect
                .poll(
                    async () =>
                        (
                            await auditServiceTestAdmin.getIntegrationContextEvents(
                                processInstance.id
                            )
                        )
                            .map((event) => event.eventType)
                            .sort(),
                    pollOptions('querySync')
                )
                .toEqual(
                    [
                        EventType.INTEGRATION_REQUESTED,
                        EventType.INTEGRATION_ERROR_RECEIVED,
                        EventType.INTEGRATION_REQUESTED,
                        EventType.INTEGRATION_RESULT_RECEIVED,
                    ].sort()
                );
        });

        await activiti.step('And the user can get list of service tasks with status of COMPLETED', async () => {
            await expect
                .poll(async () => {
                    const tasks = await queryAdminServiceTestAdmin.getServiceTasksByStatusForProcessInstance(
                        processInstance.id,
                        ServiceTaskStatus.COMPLETED
                    );
                    return tasks.length > 0;
                }, pollOptions('querySync'))
                .toBe(true);
        });

        await activiti.step('And the process with service tasks is completed', async () => {
            await expect
                .poll(
                    async () =>
                        (
                            await getQueryProcessInstanceAdminWhenSynced(
                                queryAdminServiceTestAdmin,
                                processInstance.id
                            )
                        )?.status,
                    pollOptions('querySync')
                )
                .toBe(ProcessInstanceStatus.COMPLETED);
        });
    });
});
