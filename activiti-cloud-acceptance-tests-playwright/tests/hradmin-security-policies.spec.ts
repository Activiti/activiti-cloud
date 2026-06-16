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

import { activiti, expect } from '../fixtures/services.fixture';
import { CloudProcessInstance } from '../models/runtime-bundle.models';
import {
    expectNoAuditEventsForProcessInstance,
    expectProcessInstancesAdminForKey,
    expectProcessInstancesForKey,
    expectQueryDoesNotIncludeProcessInstance,
    expectQueryProcessInstancesAdminForKey,
} from '../helpers/security-policies.assertions';

activiti.describe('Security Policies - HR Admin Actions', { tag: '@smoke' }, () => {
    activiti.describe('Admin Access to Process with Variables', () => {
        activiti('should allow hradmin to access process with variables through admin endpoints', async ({ securityPoliciesServiceProcessAdmin }) => {
            let processWithVariablesInstance: CloudProcessInstance;

            await activiti.step('When the user starts an instance of the process called PROCESS_INSTANCE_WITH_VARIABLES', async () => {
                processWithVariablesInstance = await securityPoliciesServiceProcessAdmin.startProcess('PROCESS_INSTANCE_WITH_VARIABLES');
                expect(processWithVariablesInstance).toBeDefined();
                expect(processWithVariablesInstance.id).toBeTruthy();
                expect(processWithVariablesInstance.processDefinitionKey).toBe('ProcessWithVariables');
            });

            await activiti.step('Then the user can access the instance from the admin APIs', async () => {
                const adminProcessInstances = await expectProcessInstancesAdminForKey(
                    securityPoliciesServiceProcessAdmin,
                    'PROCESS_INSTANCE_WITH_VARIABLES',
                    true
                );

                expect(adminProcessInstances.length).toBeGreaterThan(0);
            });

            await activiti.step('And the user can access query admin endpoints', async () => {
                const adminQueryProcessInstances = await expectQueryProcessInstancesAdminForKey(
                    securityPoliciesServiceProcessAdmin,
                    'PROCESS_INSTANCE_WITH_VARIABLES',
                    true
                );
                expect(adminQueryProcessInstances.length).toBeGreaterThan(0);
            });

            await activiti.step('And the user can access audit admin endpoints', async () => {
                const adminEvents = await securityPoliciesServiceProcessAdmin.getEventsByEntityIdAdmin(
                    processWithVariablesInstance.id
                );
                expect(adminEvents.length).toBeGreaterThan(0);
            });
        });
    });

    activiti.describe('User-level Access Restrictions', () => {
        activiti('should restrict hradmin from user-level access to process with variables', async ({
            securityPoliciesServiceHradmin,
            securityPoliciesServiceProcessAdmin
        }) => {
            let processWithVariablesInstance: CloudProcessInstance;

            await activiti.step('When an admin starts an instance of the process called PROCESS_INSTANCE_WITH_VARIABLES', async () => {
                processWithVariablesInstance = await securityPoliciesServiceProcessAdmin.startProcess('PROCESS_INSTANCE_WITH_VARIABLES');
                expect(processWithVariablesInstance).toBeDefined();
            });

            await activiti.step('Then the user cannot get process with variables instances (through user endpoints)', async () => {
                const userProcessInstances = await expectProcessInstancesForKey(
                    securityPoliciesServiceHradmin,
                    'PROCESS_INSTANCE_WITH_VARIABLES',
                    false
                );
                expect(userProcessInstances).toHaveLength(0);
            });

            await activiti.step('And the user cannot query process with variables instances (through user endpoints)', async () => {
                await expectQueryDoesNotIncludeProcessInstance(
                    securityPoliciesServiceHradmin,
                    processWithVariablesInstance.id,
                    'PROCESS_INSTANCE_WITH_VARIABLES'
                );
            });

            await activiti.step('And the user cannot get events for process with variables instances (through user endpoints)', async () => {
                await expectNoAuditEventsForProcessInstance(
                    securityPoliciesServiceHradmin,
                    processWithVariablesInstance.id
                );
            });
        });
    });
});
