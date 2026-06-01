/*
 * Copyright 2017-2026 Alfresco Software, Ltd.
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
    expectEventsForKey,
    expectProcessInstancesForKey,
    expectQueryProcessInstancesForKey,
} from '../helpers/security-policies.assertions';

activiti.describe('Security Policies - HR User Actions', { tag: '@smoke' }, () => {
    activiti.describe('Simple Process Instance Operations', () => {
        activiti('should allow hruser to start and access simple process instances', async ({ securityPoliciesServiceHrUser }) => {
            let simpleProcessInstance: CloudProcessInstance;

            await activiti.step('When the user starts an instance of the process called SIMPLE_PROCESS_INSTANCE', async () => {
                simpleProcessInstance = await securityPoliciesServiceHrUser.startProcess('SIMPLE_PROCESS_INSTANCE');
                expect(simpleProcessInstance).toBeDefined();
                expect(simpleProcessInstance.id).toBeTruthy();
                expect(simpleProcessInstance.processDefinitionKey).toBe('SimpleProcess');
            });

            await activiti.step('Then the user can get simple process instances', async () => {
                const fetched = await securityPoliciesServiceHrUser.getRuntimeProcessInstance(simpleProcessInstance.id);
                expect(fetched.id).toBe(simpleProcessInstance.id);
                expect(fetched.processDefinitionKey).toBe('SimpleProcess');
            });

            await activiti.step('And the user can query simple process instances', async () => {
                const queryProcessInstances = await expectQueryProcessInstancesForKey(
                    securityPoliciesServiceHrUser,
                    'SIMPLE_PROCESS_INSTANCE',
                    true
                );
                expect(queryProcessInstances.length).toBeGreaterThan(0);
            });

            await activiti.step('And the user can get events for simple process instances', async () => {
                const events = await expectEventsForKey(
                    securityPoliciesServiceHrUser,
                    'SIMPLE_PROCESS_INSTANCE',
                    true
                );
                expect(events.length).toBeGreaterThan(0);
            });
        });
    });

    activiti.describe('Process with Variables Restrictions', () => {
        activiti('should restrict hruser from starting process with variables', async ({ securityPoliciesServiceHrUser }) => {
            await activiti.step('Then the user cannot start the process with variables', async () => {
                await expect(async () => {
                    await securityPoliciesServiceHrUser.startProcess('PROCESS_INSTANCE_WITH_VARIABLES');
                }).rejects.toThrow(/Unable to find process definition.*ProcessWithVariables/);
            });
        });

        activiti('should restrict hruser from accessing process with variables instances', async ({ securityPoliciesServiceHrUser }) => {
            await activiti.step('Then the user cannot get process with variables instances', async () => {
                const processInstances = await expectProcessInstancesForKey(
                    securityPoliciesServiceHrUser,
                    'PROCESS_INSTANCE_WITH_VARIABLES',
                    false
                );
                expect(processInstances).toHaveLength(0);
            });
        });

        activiti('should restrict hruser from querying process with variables instances', async ({ securityPoliciesServiceHrUser }) => {
            await activiti.step('Then the user cannot query process with variables instances', async () => {
                const queryProcessInstances = await expectQueryProcessInstancesForKey(
                    securityPoliciesServiceHrUser,
                    'PROCESS_INSTANCE_WITH_VARIABLES',
                    false
                );
                expect(queryProcessInstances).toHaveLength(0);
            });
        });

        activiti('should restrict hruser from accessing events for process with variables', async ({ securityPoliciesServiceHrUser }) => {
            await activiti.step('Then the user cannot get events for process with variables instances', async () => {
                const events = await expectEventsForKey(
                    securityPoliciesServiceHrUser,
                    'PROCESS_INSTANCE_WITH_VARIABLES',
                    false
                );
                expect(events).toHaveLength(0);
            });
        });
    });

    activiti.describe('Task Access', () => {
        activiti('should allow hruser to access tasks', async ({ securityPoliciesServiceHrUser }) => {
            await activiti.step('Then the user can get tasks', async () => {
                const tasks = await securityPoliciesServiceHrUser.getAllTasks();
                expect(tasks).toBeDefined();
            });
        });

        activiti('should allow hruser to query tasks', async ({ securityPoliciesServiceHrUser }) => {
            await activiti.step('Then the user can query tasks', async () => {
                const queryTasks = await securityPoliciesServiceHrUser.queryAllTasks();
                expect(queryTasks).toBeDefined();
            });
        });
    });
});
