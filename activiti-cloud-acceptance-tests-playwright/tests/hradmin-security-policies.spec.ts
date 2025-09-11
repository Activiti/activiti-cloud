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

import { activiti } from '../fixtures/services.fixture';
import { expect } from '../fixtures/context.fixture';
import { CloudProcessInstance } from '../models/runtime-bundle.models';

activiti.describe('Security Policies - HR Admin Actions', () => {
    let processWithVariablesInstance: CloudProcessInstance;

    activiti.describe('Admin Access to Process with Variables', () => {
        activiti('should allow hradmin to access process with variables through admin endpoints', async ({ securityPoliciesServiceProcessAdmin }) => {

            await activiti.step('When the user starts an instance of the process called PROCESS_INSTANCE_WITH_VARIABLES', async () => {
                processWithVariablesInstance = await securityPoliciesServiceProcessAdmin.startProcess('PROCESS_INSTANCE_WITH_VARIABLES');
                expect(processWithVariablesInstance).toBeDefined();
                expect(processWithVariablesInstance.id).toBeTruthy();
                expect(processWithVariablesInstance.processDefinitionKey).toBe('ProcessWithVariables');
            });

            await activiti.step('Then the user can get process with variables instances in admin endpoint', async () => {
                const adminProcessInstances = await securityPoliciesServiceProcessAdmin.expectProcessInstancesAdminForKey('PROCESS_INSTANCE_WITH_VARIABLES', true);
                expect(adminProcessInstances.length).toBeGreaterThan(0);
                expect(adminProcessInstances.some(pi => pi.id === processWithVariablesInstance.id)).toBeTruthy();
            });

            await activiti.step('And the user can query process with variables instances in admin endpoints', async () => {
                const adminQueryProcessInstances = await securityPoliciesServiceProcessAdmin.expectQueryProcessInstancesAdminForKey('PROCESS_INSTANCE_WITH_VARIABLES', true);
                expect(adminQueryProcessInstances.length).toBeGreaterThan(0);
                expect(adminQueryProcessInstances.some(pi => pi.id === processWithVariablesInstance.id)).toBeTruthy();
            });

            await activiti.step('And the user can get events for process with variables instances in admin endpoint', async () => {
                const adminEvents = await securityPoliciesServiceProcessAdmin.expectEventsAdminForKey(
                    processWithVariablesInstance.id,
                    'PROCESS_INSTANCE_WITH_VARIABLES',
                    true
                );
                expect(adminEvents.length).toBeGreaterThan(0);
                expect(adminEvents.some(event => event.processInstanceId === processWithVariablesInstance.id)).toBeTruthy();

                // Verify the process definition key in the events
                const processStartEvent = adminEvents.find(event =>
                    event.eventType === 'PROCESS_STARTED' &&
                    event.processInstanceId === processWithVariablesInstance.id
                );
                expect(processStartEvent).toBeDefined();
                expect(processStartEvent?.processDefinitionKey).toBe('ProcessWithVariables');
            });
        });
    });

    activiti.describe('User-level Access Restrictions', () => {
        activiti('should restrict hradmin from user-level access to process with variables', async ({ securityPoliciesServiceProcessAdmin }) => {
            await activiti.step('Given the user is authenticated as hradmin', async () => {
                // Authentication is handled by the processAdminContext fixture
            });

            await activiti.step('When the user starts an instance of the process called PROCESS_INSTANCE_WITH_VARIABLES', async () => {
                processWithVariablesInstance = await securityPoliciesServiceProcessAdmin.startProcess('PROCESS_INSTANCE_WITH_VARIABLES');
                expect(processWithVariablesInstance).toBeDefined();
            });

            await activiti.step('Then the user cannot get process with variables instances (through user endpoints)', async () => {
                const userProcessInstances = await securityPoliciesServiceProcessAdmin.expectProcessInstancesForKey('PROCESS_INSTANCE_WITH_VARIABLES', false);
                expect(userProcessInstances).toHaveLength(0);
            });

            await activiti.step('And the user cannot query process with variables instances (through user endpoints)', async () => {
                const userQueryProcessInstances = await securityPoliciesServiceProcessAdmin.expectQueryProcessInstancesForKey('PROCESS_INSTANCE_WITH_VARIABLES', false);
                expect(userQueryProcessInstances).toHaveLength(0);
            });

            await activiti.step('And the user cannot get events for process with variables instances (through user endpoints)', async () => {
                const userEvents = await securityPoliciesServiceProcessAdmin.expectEventsForKey('PROCESS_INSTANCE_WITH_VARIABLES', false);
                expect(userEvents).toHaveLength(0);
            });
        });
    });
});
