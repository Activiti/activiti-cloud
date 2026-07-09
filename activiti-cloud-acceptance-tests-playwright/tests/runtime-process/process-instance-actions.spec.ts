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
import { CloudProcessInstance, ProcessInstanceStatus } from '../../models/runtime-bundle.models';

activiti.describe('Process Instance Actions - Multiple Runtime Bundle Services', { tag: '@slow' }, () => {
    activiti.describe('Signal Between Multiple Runtime Bundles', () => {
        activiti('should handle signal communication between primary and secondary runtime bundles', async ({ multipleRuntimeServiceTestUser }) => {
            let processInstanceCatchSignal: CloudProcessInstance;
            let processInstanceThrowSignal: CloudProcessInstance;

            await activiti.step('When the user starts signal catch process on primary runtime', async () => {
                processInstanceCatchSignal = await multipleRuntimeServiceTestUser.primary.processInstances.startProcess({
                    processDefinitionKey: 'SignalCatchEventProcess',
                });
                expect(processInstanceCatchSignal).toBeDefined();
                expect(processInstanceCatchSignal.id).toBeTruthy();
                expect(processInstanceCatchSignal.processDefinitionKey).toBe('SignalCatchEventProcess');
            });

            await activiti.step('And starts signal throw process on secondary runtime', async () => {
                processInstanceThrowSignal = await multipleRuntimeServiceTestUser.secondary.processInstances.startProcess({
                    processDefinitionKey: 'SignalThrowEventProcess',
                });
                expect(processInstanceThrowSignal).toBeDefined();
                expect(processInstanceThrowSignal.id).toBeTruthy();
                expect(processInstanceThrowSignal.processDefinitionKey).toBe('SignalThrowEventProcess');
            });

            await activiti.step('Then a signal was received and the signal catch and throw processes were completed', async () => {
                const finalCatchProcess = await multipleRuntimeServiceTestUser.waitForProcessInstanceStatusOnPrimary(
                    processInstanceCatchSignal.id,
                    ProcessInstanceStatus.COMPLETED
                );
                const finalThrowProcess = await multipleRuntimeServiceTestUser.waitForProcessInstanceStatusOnSecondary(
                    processInstanceThrowSignal.id,
                    ProcessInstanceStatus.COMPLETED
                );

                expect(finalCatchProcess.status).toBe(ProcessInstanceStatus.COMPLETED);
                expect(finalThrowProcess.status).toBe(ProcessInstanceStatus.COMPLETED);
                expect(finalCatchProcess.serviceName).toBeTruthy();
                expect(finalCatchProcess.serviceFullName).toBeTruthy();
                expect(finalThrowProcess.serviceName).toBeTruthy();
                expect(finalThrowProcess.serviceFullName).toBeTruthy();
            });
        });
    });
});
