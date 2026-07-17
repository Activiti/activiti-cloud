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
import { startCatalogProcess } from '../../flows/start-process-with-first-task';

activiti.describe('Process Instance Variable Admin Actions', { tag: '@slow' }, () => {
    activiti(
        'admin update process instance variables',
        async ({
            runtimeBundleServiceTestUser,
            runtimeAdminServiceTestAdmin,
        }) => {
            let processInstanceId: string;

            await activiti.step(
                'Given the user is authenticated as testuser ' +
                    'When the user starts the process PROCESS_INSTANCE_WITH_VARIABLES with variables start1 and start2',
                async () => {
                    const processInstance = await startCatalogProcess(
                        runtimeBundleServiceTestUser,
                        'PROCESS_INSTANCE_WITH_VARIABLES',
                        { variables: { start1: 'start1', start2: 'start2' } }
                    );
                    expect(processInstance.id).toBeTruthy();
                    processInstanceId = processInstance.id;
                }
            );

            await activiti.step(
                'And the admin update the instance variables start1 with value value1 and start2 with value value2',
                async () => {
                    await runtimeAdminServiceTestAdmin.processInstances.setProcessVariables(processInstanceId, {
                        start1: 'value1',
                        start2: 'value2',
                    });
                }
            );

            await activiti.step(
                'And variable start1 has value value1 and start2 has value value2',
                async () => {
                    const start1 = await runtimeBundleServiceTestUser.waitForProcessInstanceVariableValue(
                        processInstanceId,
                        'start1',
                        'value1'
                    );
                    const start2 = await runtimeBundleServiceTestUser.waitForProcessInstanceVariableValue(
                        processInstanceId,
                        'start2',
                        'value2'
                    );
                    expect(start1).toBe('value1');
                    expect(start2).toBe('value2');
                }
            );
        }
    );

    activiti(
        'admin set process instance variables',
        async ({
            runtimeBundleServiceTestUser,
            runtimeAdminServiceTestAdmin,
        }) => {
            let processInstanceId: string;

            await activiti.step(
                'Given the user is authenticated as testuser ' +
                    'When the user starts the process PROCESS_INSTANCE_WITH_VARIABLES with variables start1 and start2',
                async () => {
                    const processInstance = await startCatalogProcess(
                        runtimeBundleServiceTestUser,
                        'PROCESS_INSTANCE_WITH_VARIABLES',
                        { variables: { start1: 'start1', start2: 'start2' } }
                    );
                    expect(processInstance.id).toBeTruthy();
                    processInstanceId = processInstance.id;
                }
            );

            await activiti.step(
                'And the user set the instance variable dummy1 with value dummyValue1',
                async () => {
                    await runtimeAdminServiceTestAdmin.processInstances.setProcessVariables(processInstanceId, {
                        dummy1: 'dummyValue1',
                    });
                }
            );

            await activiti.step(
                'And the user set the instance variable dummy2 with value dummyValue2',
                async () => {
                    await runtimeAdminServiceTestAdmin.processInstances.setProcessVariables(processInstanceId, {
                        dummy2: 'dummyValue2',
                    });
                }
            );

            await activiti.step(
                'Then variable dummy1 has value dummyValue1 and dummy2 has value dummyValue2',
                async () => {
                    const dummy1 = await runtimeBundleServiceTestUser.waitForProcessInstanceVariableValue(
                        processInstanceId,
                        'dummy1',
                        'dummyValue1'
                    );
                    const dummy2 = await runtimeBundleServiceTestUser.waitForProcessInstanceVariableValue(
                        processInstanceId,
                        'dummy2',
                        'dummyValue2'
                    );
                    expect(dummy1).toBe('dummyValue1');
                    expect(dummy2).toBe('dummyValue2');
                }
            );
        }
    );

    activiti(
        'admin delete process instance variables',
        async ({
            runtimeBundleServiceTestUser,
            runtimeAdminServiceTestAdmin,
        }) => {
            let processInstanceId: string;

            await activiti.step(
                'Given the user is authenticated as testuser ' +
                    'When the user starts the process PROCESS_INSTANCE_WITH_VARIABLES with variables start1 and start2',
                async () => {
                    const processInstance = await startCatalogProcess(
                        runtimeBundleServiceTestUser,
                        'PROCESS_INSTANCE_WITH_VARIABLES',
                        { variables: { start1: 'start1', start2: 'start2' } }
                    );
                    expect(processInstance.id).toBeTruthy();
                    processInstanceId = processInstance.id;
                }
            );

            await activiti.step(
                'And the user set the instance variable dummy1 with value dummyValue1',
                async () => {
                    await runtimeAdminServiceTestAdmin.processInstances.setProcessVariables(processInstanceId, {
                        dummy1: 'dummyValue1',
                    });
                }
            );

            await activiti.step(
                'And the user set the instance variable dummy2 with value dummyValue2',
                async () => {
                    await runtimeAdminServiceTestAdmin.processInstances.setProcessVariables(processInstanceId, {
                        dummy2: 'dummyValue2',
                    });
                }
            );

            await activiti.step('And the admin delete the instance variable dummy1', async () => {
                await runtimeAdminServiceTestAdmin.processInstances.deleteProcessVariables(processInstanceId, [
                    'dummy1',
                ]);
            });

            await activiti.step('Then the process variable dummy1 is deleted', async () => {
                const dummy1 = await runtimeBundleServiceTestUser.waitForProcessInstanceVariableDeleted(
                    processInstanceId,
                    'dummy1'
                );
                expect(dummy1).toBeUndefined();
            });

            await activiti.step('And the process variable dummy2 is created', async () => {
                const dummy2 = await runtimeBundleServiceTestUser.waitForProcessInstanceVariableValue(
                    processInstanceId,
                    'dummy2',
                    'dummyValue2'
                );
                expect(dummy2).toBe('dummyValue2');
            });
        }
    );
});
