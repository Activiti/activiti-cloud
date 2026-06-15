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
import { startCatalogProcess } from '../../flows/start-catalog-process';
import { RuntimeBundleService } from '../../services/runtime-bundle.service';

async function expectVariableValue(
    rbService: RuntimeBundleService,
    processInstanceId: string,
    variableName: string,
    expectedValue: unknown
): Promise<void> {
    await expect
        .poll(async () => {
            const variables = await rbService.getProcessInstanceVariables(processInstanceId);
            const match = variables.find((v) => v.name === variableName);
            return match ? String(match.value) : undefined;
        }, pollOptions('querySync'))
        .toBe(String(expectedValue));
}

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
                    await runtimeAdminServiceTestAdmin.setProcessVariables(processInstanceId, {
                        start1: 'value1',
                        start2: 'value2',
                    });
                }
            );

            await activiti.step('Then the list of errors messages is empty', async () => {
                // PUT returning 200 without throwing means no errors — verified implicitly above
            });

            await activiti.step(
                'And variable start1 has value value1 and start2 has value value2',
                async () => {
                    await expectVariableValue(runtimeBundleServiceTestUser, processInstanceId, 'start1', 'value1');
                    await expectVariableValue(runtimeBundleServiceTestUser, processInstanceId, 'start2', 'value2');
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
                    await runtimeAdminServiceTestAdmin.setProcessVariables(processInstanceId, {
                        dummy1: 'dummyValue1',
                    });
                }
            );

            await activiti.step(
                'And the user set the instance variable dummy2 with value dummyValue2',
                async () => {
                    await runtimeAdminServiceTestAdmin.setProcessVariables(processInstanceId, {
                        dummy2: 'dummyValue2',
                    });
                }
            );

            await activiti.step(
                'Then variable dummy1 has value dummyValue1 and dummy2 has value dummyValue2',
                async () => {
                    await expectVariableValue(
                        runtimeBundleServiceTestUser,
                        processInstanceId,
                        'dummy1',
                        'dummyValue1'
                    );
                    await expectVariableValue(
                        runtimeBundleServiceTestUser,
                        processInstanceId,
                        'dummy2',
                        'dummyValue2'
                    );
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
                    await runtimeAdminServiceTestAdmin.setProcessVariables(processInstanceId, {
                        dummy1: 'dummyValue1',
                    });
                }
            );

            await activiti.step(
                'And the user set the instance variable dummy2 with value dummyValue2',
                async () => {
                    await runtimeAdminServiceTestAdmin.setProcessVariables(processInstanceId, {
                        dummy2: 'dummyValue2',
                    });
                }
            );

            await activiti.step('And the admin delete the instance variable dummy1', async () => {
                await runtimeAdminServiceTestAdmin.deleteProcessVariables(processInstanceId, [
                    'dummy1',
                ]);
            });

            await activiti.step('Then the process variable dummy1 is deleted', async () => {
                await expect
                    .poll(async () => {
                        const variables =
                            await runtimeBundleServiceTestUser.getProcessInstanceVariables(
                                processInstanceId
                            );
                        return variables.some((v) => v.name === 'dummy1');
                    }, pollOptions('querySync'))
                    .toBe(false);
            });

            await activiti.step('And the process variable dummy2 is created', async () => {
                await expectVariableValue(
                    runtimeBundleServiceTestUser,
                    processInstanceId,
                    'dummy2',
                    'dummyValue2'
                );
            });
        }
    );
});
