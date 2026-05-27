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

import { activiti } from '../../fixtures/services.fixture';
import { expect } from '../../fixtures/context.fixture';
import { scopedName } from '../../helpers/test-isolation';
import { ProcessDefinitionRegistry } from '../../models/process-definition-registry';
import { ProcessInstanceStatus } from '../../models/runtime-bundle.models';
import { pollOptions } from '../../config/runtime/timeouts';
import { buildConnectorStartVariables } from '../../helpers/connector-process-payload';
import {
    expectProcessVariable,
    expectProcessVariableValue,
} from '../../helpers/process-variables';

const CONNECTOR_RESULT_VARIABLES = [
    'var1',
    'test_json_variable_result',
    'test_long_json_variable_result',
    'test_int_variable_result',
    'test_bool_variable_result',
    'test_long_variable_result',
    'test_bigdecimal_variable_result',
    'test_date_variable_result',
] as const;

activiti.describe('Runtime — Process Instance Actions (extended)', () => {
    activiti('should reject query diagram admin endpoint for hruser', async ({
        runtimeBundleServiceTestUser,
        queryAdminServiceHrUser,
    }) => {
        let processInstanceId: string;

        await activiti.step('When the user starts an instance of the process called CONNECTOR_PROCESS_INSTANCE', async () => {
            const processInstance = await runtimeBundleServiceTestUser.startProcess({
                processDefinitionKey: ProcessDefinitionRegistry.getProcessDefinitionKey('CONNECTOR_PROCESS_INSTANCE'),
            });
            processInstanceId = processInstance.id;
        });

        await activiti.step('Then query the process diagram admin endpoint is unauthorized', async () => {
            const status = await queryAdminServiceHrUser.getProcessInstanceDiagramStatus(processInstanceId);
            expect(status).toBe(403);
        });
    });

    activiti('should complete a connector process instance with variables', async ({
        runtimeBundleServiceTestUser,
        queryServiceTestUser,
    }) => {
        let processInstanceId: string;

        await activiti.step('When the user starts a process with variables called CONNECTOR_PROCESS_INSTANCE', async () => {
            const processInstance = await runtimeBundleServiceTestUser.startProcessWithVariables(
                ProcessDefinitionRegistry.getProcessDefinitionKey('CONNECTOR_PROCESS_INSTANCE'),
                buildConnectorStartVariables()
            );
            processInstanceId = processInstance.id;
        });

        await activiti.step('Then the status of the process is changed to completed', async () => {
            await expect
                .poll(async () => {
                    try {
                        const instance = await queryServiceTestUser.getProcessInstance(processInstanceId);
                        return instance.status === ProcessInstanceStatus.COMPLETED
                            ? ProcessInstanceStatus.COMPLETED
                            : instance.status;
                    } catch (error) {
                        const message = error instanceof Error ? error.message : String(error);
                        if (message.includes('Unable to find process instance')) {
                            return 'SYNC_PENDING';
                        }
                        throw error;
                    }
                }, pollOptions('querySync'))
                .toBe(ProcessInstanceStatus.COMPLETED);
        });

        for (const variableName of CONNECTOR_RESULT_VARIABLES) {
            await activiti.step(`And a variable was created with name ${variableName}`, async () => {
                await expectProcessVariable(queryServiceTestUser, processInstanceId, variableName);
            });
        }

        await activiti.step('And query process instance variable test_bigdecimal_variable_result has value 12345678.90', async () => {
            await expectProcessVariableValue(
                queryServiceTestUser,
                processInstanceId,
                'test_bigdecimal_variable_result',
                '12345678.90'
            );
        });
    });

    activiti('should list process definitions on query admin endpoint', async ({
        queryAdminServiceProcessAdmin,
    }) => {
        await activiti.step('Then the user gets all the process definitions in admin endpoint', async () => {
            const definitions = await queryAdminServiceProcessAdmin.getAllProcessDefinitionsAdmin();
            const names = definitions.map((definition) => definition.name).filter(Boolean);
            expect(names.length).toBeGreaterThan(0);
            expect(names).toEqual(
                expect.arrayContaining(['single-task', 'Process with variables', 'ConnectorProcess'])
            );
        });
    });

    activiti('should expose admin access to process with variables', async ({
        securityPoliciesServiceProcessAdmin,
    }) => {
        let processInstanceId: string;

        await activiti.step('When the user starts an instance of the process called PROCESS_INSTANCE_WITH_VARIABLES', async () => {
            const instance = await securityPoliciesServiceProcessAdmin.startProcess('PROCESS_INSTANCE_WITH_VARIABLES');
            processInstanceId = instance.id;
        });

        await activiti.step('Then the user can get process with variables instances in admin endpoint', async () => {
            await securityPoliciesServiceProcessAdmin.expectProcessInstancesAdminForKey(
                'PROCESS_INSTANCE_WITH_VARIABLES',
                true
            );
        });

        await activiti.step('And the user can query process with variables instances in admin endpoints', async () => {
            const queryInstances = await securityPoliciesServiceProcessAdmin.expectQueryProcessInstancesAdminForKey(
                'PROCESS_INSTANCE_WITH_VARIABLES',
                true
            );
            expect(queryInstances.length).toBeGreaterThan(0);
        });

        await activiti.step('And the user can get events for process with variables instances in admin endpoint', async () => {
            await securityPoliciesServiceProcessAdmin.expectEventsAdminForKey(
                processInstanceId,
                'PROCESS_INSTANCE_WITH_VARIABLES',
                true
            );
        });
    });

    activiti('should expose formKey on single task process definition', async ({
        runtimeBundleServiceTestUser,
    }) => {
        await activiti.step('Then the PROCESS_INSTANCE_WITH_SINGLE_TASK_ASSIGNED definition has the formKey field with value startForm', async () => {
            const definition = await runtimeBundleServiceTestUser.getProcessDefinitionByKey(
                ProcessDefinitionRegistry.getProcessDefinitionKey('PROCESS_INSTANCE_WITH_SINGLE_TASK_ASSIGNED')
            );
            expect(definition.formKey).toBe('startForm');
        });
    });

    activiti('should update process instance name', async ({
        runtimeBundleServiceTestUser,
        queryServiceTestUser,
    }) => {
        const newName = 'new-process-name';
        let processInstanceId: string;

        await activiti.step('When the user starts an instance of the process called PROCESS_INSTANCE_WITH_SINGLE_TASK_ASSIGNED', async () => {
            const processInstance = await runtimeBundleServiceTestUser.startProcess({
                processDefinitionKey: ProcessDefinitionRegistry.getProcessDefinitionKey(
                    'PROCESS_INSTANCE_WITH_SINGLE_TASK_ASSIGNED'
                ),
            });
            processInstanceId = processInstance.id;
        });

        await activiti.step('And the user updates the name of the process instance to new-process-name', async () => {
            await runtimeBundleServiceTestUser.updateProcessInstance(processInstanceId, { name: newName });
        });

        await activiti.step('Then the process instance is updated', async () => {
            const runtimeInstance = await runtimeBundleServiceTestUser.getProcessInstance(processInstanceId);
            expect(runtimeInstance.name).toBe(newName);
        });

        await activiti.step('And the process has the name new-process-name', async () => {
            await expect
                .poll(async () => {
                    const queryInstance = await queryServiceTestUser.getProcessInstance(processInstanceId);
                    return queryInstance.name;
                }, pollOptions('querySync'))
                .toBe(newName);
        });
    });

    activiti('should start a process instance with a custom name', async ({
        testScope,
        runtimeBundleServiceTestUser,
        queryServiceTestUser,
    }) => {
        const processInstanceName = scopedName(testScope, 'my_process_instance_name');

        await activiti.step('When the user set a process instance name my_process_instance_name and starts the process SIMPLE_PROCESS_INSTANCE', async () => {
            const processInstance = await runtimeBundleServiceTestUser.startProcess({
                processDefinitionKey: ProcessDefinitionRegistry.getProcessDefinitionKey('SIMPLE_PROCESS_INSTANCE'),
                name: processInstanceName,
            });
            const runtimeInstance = await runtimeBundleServiceTestUser.getProcessInstance(processInstance.id);
            expect(runtimeInstance.name).toBe(processInstanceName);

            await expect
                .poll(async () => {
                    const queryInstance = await queryServiceTestUser.getProcessInstance(processInstance.id);
                    return queryInstance.name;
                }, pollOptions('querySync'))
                .toBe(processInstanceName);
        });

        await activiti.step('Then verify the process instance name is my_process_instance_name', async () => {
            const instances = await queryServiceTestUser.getProcessInstancesByName(processInstanceName);
            expect(instances.some((instance) => instance.name === processInstanceName)).toBe(true);
        });
    });

    activiti('should admin delete a process instance', async ({
        runtimeBundleServiceTestAdmin,
        runtimeAdminServiceTestAdmin,
    }) => {
        let processInstanceId: string;

        await activiti.step('When the user starts an instance of the process called PROCESS_INSTANCE_WITH_VARIABLES', async () => {
            const processInstance = await runtimeBundleServiceTestAdmin.startProcess({
                processDefinitionKey: ProcessDefinitionRegistry.getProcessDefinitionKey('PROCESS_INSTANCE_WITH_VARIABLES'),
            });
            processInstanceId = processInstance.id;
        });

        await activiti.step('And the admin deletes the process', async () => {
            await runtimeAdminServiceTestAdmin.deleteProcessInstance(processInstanceId);
        });

        await activiti.step('Then the process instance is deleted', async () => {
            await expect(async () => {
                await runtimeBundleServiceTestAdmin.getProcessInstance(processInstanceId);
            }).rejects.toThrow();
        });
    });

    activiti('should admin force destroy a process instance', async ({
        runtimeBundleServiceTestAdmin,
        runtimeAdminServiceTestAdmin,
        queryServiceTestUser,
    }) => {
        let processInstanceId: string;

        await activiti.step('When the user starts an instance of the process called PROCESS_INSTANCE_WITH_VARIABLES', async () => {
            const processInstance = await runtimeBundleServiceTestAdmin.startProcess({
                processDefinitionKey: ProcessDefinitionRegistry.getProcessDefinitionKey('PROCESS_INSTANCE_WITH_VARIABLES'),
            });
            processInstanceId = processInstance.id;
        });

        await activiti.step('And the admin force destroys the process', async () => {
            await runtimeAdminServiceTestAdmin.destroyProcessInstance(processInstanceId, true);
        });

        await activiti.step('Then the process instance is destroyed', async () => {
            await expect(async () => {
                await runtimeBundleServiceTestAdmin.getProcessInstance(processInstanceId);
            }).rejects.toThrow();

            await expect(async () => {
                await queryServiceTestUser.getProcessInstance(processInstanceId);
            }).rejects.toThrow();
        });
    });

    activiti('should set sequence number and message id on audit events', async ({
        runtimeBundleServiceTestUser,
        auditServiceTestUser,
    }) => {
        let processInstanceId: string;

        await activiti.step('When the user starts an instance of the process called SIMPLE_PROCESS_INSTANCE', async () => {
            const processInstance = await runtimeBundleServiceTestUser.startProcess({
                processDefinitionKey: ProcessDefinitionRegistry.getProcessDefinitionKey('SIMPLE_PROCESS_INSTANCE'),
                name: 'process-instance-name',
            });
            processInstanceId = processInstance.id;
        });

        await activiti.step('Then the generated events have sequence number set', async () => {
            await expect
                .poll(async () => {
                    const events = await auditServiceTestUser.getEventsByEntityId(processInstanceId);
                    if (events.length === 0) {
                        return false;
                    }
                    const sequenceNumbers = events
                        .map((event) => event.sequenceNumber)
                        .filter((value): value is number => value !== undefined);
                    for (let index = 0; index < events.length; index++) {
                        if (!sequenceNumbers.includes(index)) {
                            return false;
                        }
                    }
                    return true;
                }, pollOptions('auditEvents'))
                .toBe(true);
        });

        await activiti.step('And the generated events have the same message id', async () => {
            await expect
                .poll(async () => {
                    const events = await auditServiceTestUser.getEventsByProcessInstanceId(processInstanceId);
                    if (events.length === 0) {
                        return false;
                    }
                    const messageId = events[0].messageId;
                    return events.every((event) => event.messageId === messageId && Boolean(messageId));
                }, pollOptions('auditEvents'))
                .toBe(true);
        });
    });

    activiti('should query process instances by name using LIKE operator', async ({
        testScope,
        runtimeBundleServiceTestUser,
        queryServiceTestUser,
    }) => {
        const processInstanceName = scopedName(testScope, 'like-query-process-name');

        await activiti.step('When the user starts an instance of the process called SIMPLE_PROCESS_INSTANCE', async () => {
            await runtimeBundleServiceTestUser.startProcess({
                processDefinitionKey: ProcessDefinitionRegistry.getProcessDefinitionKey('SIMPLE_PROCESS_INSTANCE'),
                name: processInstanceName,
            });
        });

        await activiti.step('Then the process instance can be queried using LIKE operator', async () => {
            const namePrefix = testScope.prefix;
            await expect
                .poll(async () => {
                    const instances = await queryServiceTestUser.getProcessInstancesByName(namePrefix);
                    return instances.some((instance) => instance.name?.includes(namePrefix));
                }, pollOptions('querySync'))
                .toBe(true);
        });
    });

    activiti('should set process definition headers on connector integration', async ({
        runtimeBundleServiceHrUser,
        taskServiceHrUser,
        queryServiceHrUser,
    }) => {
        let processInstanceId: string;
        const headerVariableNames = [
            'processDefinitionVersion',
            'processDefinitionKey',
            'processDefinitionId',
        ] as const;

        await activiti.step('When the user starts an instance of the process called PROCESS_WITH_HEADERS_CONNECTOR', async () => {
            const processInstance = await runtimeBundleServiceHrUser.startProcess({
                processDefinitionKey: ProcessDefinitionRegistry.getProcessDefinitionKey('PROCESS_WITH_HEADERS_CONNECTOR'),
            });
            processInstanceId = processInstance.id;
        });

        await activiti.step('And the user claims the task', async () => {
            const tasks = await taskServiceHrUser.getTasksByProcessInstanceId(processInstanceId);
            expect(tasks.length).toBeGreaterThan(0);
            await taskServiceHrUser.claimTask(tasks[0].id);
        });

        await activiti.step('And the user completes the task', async () => {
            const tasks = await taskServiceHrUser.getTasksByProcessInstanceId(processInstanceId);
            await taskServiceHrUser.completeTask(tasks[0].id);
        });

        for (const variableName of headerVariableNames) {
            await activiti.step(`Then a variable was created with name ${variableName}`, async () => {
                await expectProcessVariable(queryServiceHrUser, processInstanceId, variableName);
            });
        }
    });
});
