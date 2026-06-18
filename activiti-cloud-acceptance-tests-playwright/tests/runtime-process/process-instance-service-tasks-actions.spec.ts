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
            const events = await auditServiceTestAdmin.waitForIntegrationContextEventTypes(
                processInstance.id,
                [EventType.INTEGRATION_REQUESTED, EventType.INTEGRATION_RESULT_RECEIVED]
            );
            expect(events.map((event) => event.eventType).sort()).toEqual(
                [
                    EventType.INTEGRATION_REQUESTED,
                    EventType.INTEGRATION_RESULT_RECEIVED,
                ].sort()
            );
        });

        await activiti.step('And the process with service tasks is completed', async () => {
            const instance = await queryAdminServiceTestAdmin.waitForProcessInstanceAdminStatus(
                processInstance.id,
                ProcessInstanceStatus.COMPLETED
            );
            expect(instance.status).toBe(ProcessInstanceStatus.COMPLETED);
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
            const tasks = await queryAdminServiceTestAdmin.waitForServiceTasksForProcessInstance(
                processInstance.id,
                (list) => list.length > 0 && list.every((task) => task.activityType === 'serviceTask'),
                `service tasks for process ${processInstance.id} of type serviceTask`
            );
            expect(tasks.length).toBeGreaterThan(0);
        });

        await activiti.step('And the process with service tasks is completed', async () => {
            const instance = await queryAdminServiceTestAdmin.waitForProcessInstanceAdminStatus(
                processInstance.id,
                ProcessInstanceStatus.COMPLETED
            );
            expect(instance.status).toBe(ProcessInstanceStatus.COMPLETED);
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
            const tasks = await queryAdminServiceTestAdmin.waitForServiceTasksForProcessInstance(
                processInstance.id,
                (list) => list.length === 1,
                `single service task for process ${processInstance.id}`
            );
            const serviceTask = await queryAdminServiceTestAdmin.getServiceTaskById(tasks[0].id);
            expect(serviceTask.activityType).toBe('serviceTask');
        });

        await activiti.step('And the process with service tasks is completed', async () => {
            const instance = await queryAdminServiceTestAdmin.waitForProcessInstanceAdminStatus(
                processInstance.id,
                ProcessInstanceStatus.COMPLETED
            );
            expect(instance.status).toBe(ProcessInstanceStatus.COMPLETED);
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
                const tasks = await queryAdminServiceTestAdmin.waitForServiceTasksForProcessInstance(
                    processInstance.id,
                    (list) => list.length === 1,
                    `single service task for process ${processInstance.id}`
                );
                const integrationContext =
                    await queryAdminServiceTestAdmin.waitForServiceTaskIntegrationContext(
                        tasks[0].id,
                        (context) =>
                            context.clientType === 'ServiceTask' &&
                            context.status === IntegrationContextStatus.INTEGRATION_RESULT_RECEIVED
                    );
                expect(integrationContext.clientType).toBe('ServiceTask');
            }
        );

        await activiti.step('And the process with service tasks is completed', async () => {
            const instance = await queryAdminServiceTestAdmin.waitForProcessInstanceAdminStatus(
                processInstance.id,
                ProcessInstanceStatus.COMPLETED
            );
            expect(instance.status).toBe(ProcessInstanceStatus.COMPLETED);
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
            const tasks = await queryAdminServiceTestAdmin.waitForServiceTasksForProcessInstance(
                processInstance.id,
                (list) => list.length === 1 && list[0].integrationContextCounter === 2,
                `service task with integrationContextCounter 2 for process ${processInstance.id}`
            );
            expect(tasks[0].integrationContextCounter).toBe(2);
        });

        await activiti.step(
            'Then the user can get all service task integration contexts by service task id',
            async () => {
                const tasks = await queryAdminServiceTestAdmin.waitForServiceTasksForProcessInstance(
                    processInstance.id,
                    (list) => list.length === 1 && list[0].integrationContextCounter === 2,
                    `service task ready with 2 integration contexts for process ${processInstance.id}`
                );
                const contexts = await queryAdminServiceTestAdmin.waitForServiceTaskIntegrationContexts(
                    tasks[0].id,
                    (list) =>
                        list.length === 2 &&
                        list.some(
                            (ctx) =>
                                ctx.clientType === 'ServiceTask' &&
                                ctx.status === IntegrationContextStatus.INTEGRATION_RESULT_RECEIVED
                        )
                );
                expect(contexts).toHaveLength(2);
            }
        );

        await activiti.step('And the process with service tasks is completed', async () => {
            const instance = await queryAdminServiceTestAdmin.waitForProcessInstanceAdminStatus(
                processInstance.id,
                ProcessInstanceStatus.COMPLETED
            );
            expect(instance.status).toBe(ProcessInstanceStatus.COMPLETED);
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
                const tasks = await queryAdminServiceTestAdmin.waitForServiceTasksByStatusForProcessInstance(
                    processInstance.id,
                    ServiceTaskStatus.COMPLETED,
                    (list) =>
                        list.length > 0 &&
                        list.every(
                            (task) =>
                                task.activityType === 'serviceTask' &&
                                task.status === ServiceTaskStatus.COMPLETED
                        )
                );
                expect(tasks.length).toBeGreaterThan(0);
            }
        );

        await activiti.step('And the process with service tasks is completed', async () => {
            const instance = await queryAdminServiceTestAdmin.waitForProcessInstanceAdminStatus(
                processInstance.id,
                ProcessInstanceStatus.COMPLETED
            );
            expect(instance.status).toBe(ProcessInstanceStatus.COMPLETED);
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
            const events = await auditServiceTestAdmin.waitForIntegrationContextEventTypes(
                processInstance.id,
                [EventType.INTEGRATION_REQUESTED, EventType.INTEGRATION_ERROR_RECEIVED]
            );
            expect(events.map((event) => event.eventType).sort()).toEqual(
                [
                    EventType.INTEGRATION_REQUESTED,
                    EventType.INTEGRATION_ERROR_RECEIVED,
                ].sort()
            );
        });

        await activiti.step('And the user can get list of service tasks with status of ERROR', async () => {
            const tasks = await queryAdminServiceTestAdmin.waitForServiceTasksByStatusForProcessInstance(
                processInstance.id,
                ServiceTaskStatus.ERROR,
                (list) =>
                    list.length > 0 &&
                    list.every(
                        (task) =>
                            task.activityType === 'serviceTask' &&
                            task.status === ServiceTaskStatus.ERROR
                    )
            );
            expect(tasks.length).toBeGreaterThan(0);
        });

        await activiti.step('And the status of the process is changed to cancelled', async () => {
            const instance = await queryAdminServiceTestAdmin.waitForProcessInstanceAdminStatus(
                processInstance.id,
                ProcessInstanceStatus.CANCELLED
            );
            expect(instance.status).toBe(ProcessInstanceStatus.CANCELLED);
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

                const tasks = await queryAdminServiceTestAdmin.waitForServiceTasksByQuery(
                    {
                        processDefinitionKey: CONNECTOR_PROCESS,
                        status: ServiceTaskStatus.COMPLETED,
                    },
                    (list) =>
                        list.length > 0 &&
                        list.every(
                            (task) =>
                                task.processDefinitionId === processDefinition?.id &&
                                task.processDefinitionKey === CONNECTOR_PROCESS &&
                                task.activityType === 'serviceTask' &&
                                task.status === ServiceTaskStatus.COMPLETED
                        )
                );
                expect(tasks.length).toBeGreaterThan(0);
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
                const tasks = await queryAdminServiceTestAdmin.waitForServiceTasksByQuery(
                    {
                        processDefinitionKey: TEST_BPMN_ERROR_CONNECTOR_PROCESS,
                        status: ServiceTaskStatus.ERROR,
                    },
                    (list) =>
                        list.length > 0 &&
                        list.every(
                            (task) =>
                                task.processDefinitionKey === TEST_BPMN_ERROR_CONNECTOR_PROCESS &&
                                task.activityType === 'serviceTask' &&
                                task.status === ServiceTaskStatus.ERROR
                        )
                );
                expect(tasks.length).toBeGreaterThan(0);
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
            const events = await auditServiceTestAdmin.waitForEventsByProcessInstanceMatching(
                processInstance.id,
                (list) => list.some((event) => event.eventType === EventType.INTEGRATION_ERROR_RECEIVED),
                `INTEGRATION_ERROR_RECEIVED event for process ${processInstance.id}`
            );
            expect(events.some((event) => event.eventType === EventType.INTEGRATION_ERROR_RECEIVED)).toBe(true);
        });

        await activiti.step('And the user can get list of service tasks with status of ERROR', async () => {
            const tasks = await queryAdminServiceTestAdmin.waitForServiceTasksByStatusForProcessInstance(
                processInstance.id,
                ServiceTaskStatus.ERROR,
                (list) =>
                    list.length > 0 &&
                    list.every((task) => task.status === ServiceTaskStatus.ERROR)
            );
            expect(tasks.length).toBeGreaterThan(0);
        });

        await activiti.step('Then the user set the instance variable var with value replay', async () => {
            await runtimeBundleServiceTestAdmin.setProcessVariables(processInstance.id, { var: 'replay' });
        });

        await activiti.step('And the user can replay service task execution', async () => {
            const errorEvent = await auditServiceTestAdmin.waitForEventMatching(
                processInstance.id,
                (event) => {
                    if (event.eventType !== EventType.INTEGRATION_ERROR_RECEIVED) {
                        return false;
                    }
                    const entity = event.entity as
                        | { executionId?: string; clientId?: string }
                        | undefined;
                    return Boolean(entity?.executionId && entity?.clientId);
                },
                `INTEGRATION_ERROR_RECEIVED event with executionId/clientId for process ${processInstance.id}`
            );
            const entity = errorEvent.entity as { executionId?: string; clientId?: string };
            const executionId = entity.executionId ?? '';
            const clientId = entity.clientId ?? '';

            const response = await runtimeAdminServiceTestAdmin.replayServiceTask(
                executionId,
                clientId
            );
            expect(response.httpStatus).toBeGreaterThanOrEqual(200);
            expect(response.httpStatus).toBeLessThan(300);
        });

        await activiti.step('And the user can get list of service tasks with status of STARTED', async () => {
            const tasks = await queryAdminServiceTestAdmin.waitForServiceTasksByStatusForProcessInstance(
                processInstance.id,
                ServiceTaskStatus.STARTED
            );
            expect(tasks.length).toBeGreaterThan(0);
        });

        await activiti.step('And all integration context events are emitted for the process', async () => {
            const events = await auditServiceTestAdmin.waitForIntegrationContextEventTypes(
                processInstance.id,
                [
                    EventType.INTEGRATION_REQUESTED,
                    EventType.INTEGRATION_ERROR_RECEIVED,
                    EventType.INTEGRATION_REQUESTED,
                    EventType.INTEGRATION_RESULT_RECEIVED,
                ]
            );
            expect(events.map((event) => event.eventType).sort()).toEqual(
                [
                    EventType.INTEGRATION_REQUESTED,
                    EventType.INTEGRATION_ERROR_RECEIVED,
                    EventType.INTEGRATION_REQUESTED,
                    EventType.INTEGRATION_RESULT_RECEIVED,
                ].sort()
            );
        });

        await activiti.step('And the user can get list of service tasks with status of COMPLETED', async () => {
            const tasks = await queryAdminServiceTestAdmin.waitForServiceTasksByStatusForProcessInstance(
                processInstance.id,
                ServiceTaskStatus.COMPLETED
            );
            expect(tasks.length).toBeGreaterThan(0);
        });

        await activiti.step('And the process with service tasks is completed', async () => {
            const instance = await queryAdminServiceTestAdmin.waitForProcessInstanceAdminStatus(
                processInstance.id,
                ProcessInstanceStatus.COMPLETED
            );
            expect(instance.status).toBe(ProcessInstanceStatus.COMPLETED);
        });
    });
});
