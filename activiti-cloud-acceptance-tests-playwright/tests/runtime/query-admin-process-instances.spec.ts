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
import { catalogProcessKey, startCatalogProcess } from '../../flows/start-catalog-process';

const LINK_TYPE = 'acceptance-link-type';

activiti.describe('Runtime — Query Admin Process Instances', () => {
    activiti('should query admin process instances via POST search, count, and related GET endpoints', async ({
        runtimeBundleServiceTestUser,
        queryAdminServiceTestAdmin,
    }) => {
        let processInstanceId = '';

        await activiti.step('Given a process instance with variables synced to query', async () => {
            const processInstance = await startCatalogProcess(
                runtimeBundleServiceTestUser,
                'PROCESS_INSTANCE_WITH_VARIABLES',
                { variables: { start1: 'value1', start2: 'value2' } }
            );
            processInstanceId = processInstance.id;
            await queryAdminServiceTestAdmin.waitForProcessInstanceAdminSynced(processInstanceId);
        });

        await activiti.step('When the admin lists process instances with variable keys via GET', async () => {
            const instances = await queryAdminServiceTestAdmin.adminProcessInstances.getProcessInstancesWithVariableKeys('start1');
            expect(Array.isArray(instances)).toBe(true);
        });

        await activiti.step('And searches process instances by id via POST', async () => {
            const instances = await queryAdminServiceTestAdmin.adminProcessInstances.searchProcessInstances({
                id: [processInstanceId],
            });
            expect(instances.map((instance) => instance.id)).toContain(processInstanceId);
        });

        await activiti.step('Then the admin process instance count matches', async () => {
            const count = await queryAdminServiceTestAdmin.adminProcessInstances.countProcessInstances({
                id: [processInstanceId],
            });
            expect(count).toBeGreaterThanOrEqual(1);
        });

        await activiti.step('When the admin reads process instance variables', async () => {
            const variables = await queryAdminServiceTestAdmin.adminProcessInstances.getProcessInstanceVariables(processInstanceId);
            expect(variables.map((variable) => variable.name)).toEqual(
                expect.arrayContaining(['start1', 'start2'])
            );
        });

        await activiti.step('Then sequence flows and BPMN activities are returned', async () => {
            const sequenceFlows = await queryAdminServiceTestAdmin.adminProcessInstances.getSequenceFlows(processInstanceId);
            const activities = await queryAdminServiceTestAdmin.adminProcessInstances.getBpmnActivities(processInstanceId);
            expect(Array.isArray(sequenceFlows)).toBe(true);
            expect(Array.isArray(activities)).toBe(true);
        });

        await activiti.step('And application versions are listed', async () => {
            const appVersions = await queryAdminServiceTestAdmin.adminProcessInstances.getProcessInstanceAppVersions();
            expect(Array.isArray(appVersions)).toBe(true);
        });
    });

    activiti('should list admin subprocesses and linked process instances', async ({
        runtimeBundleServiceTestUser,
        queryServiceTestUser,
        queryAdminServiceTestAdmin,
    }) => {
        let parentProcessInstanceId = '';
        let linkedOrphanProcessInstanceId = '';

        await activiti.step('Given a parent process with a subprocess synced to query', async () => {
            const parentProcess = await startCatalogProcess(
                runtimeBundleServiceTestUser,
                'PROCESS_INSTANCE_WITH_CALL_ACTIVITIES'
            );
            parentProcessInstanceId = parentProcess.id;
            const subprocesses = await runtimeBundleServiceTestUser.waitForSubProcesses(parentProcessInstanceId);
            expect(subprocesses.length).toBeGreaterThan(0);
            await queryAdminServiceTestAdmin.waitForProcessInstanceAdminSynced(parentProcessInstanceId);
        });

        await activiti.step('When the admin lists subprocesses for the parent process instance', async () => {
            const subprocesses = await queryAdminServiceTestAdmin.adminProcessInstances.getSubprocesses(parentProcessInstanceId);
            expect(subprocesses.length).toBeGreaterThan(0);
        });

        await activiti.step('Given a main process and a linkable orphan process', async () => {
            const mainProcess = await runtimeBundleServiceTestUser.startProcess({
                processDefinitionKey: catalogProcessKey('PROCESS_INSTANCE_WITH_SINGLE_TASK_ASSIGNED'),
            });
            const orphanProcess = await runtimeBundleServiceTestUser.startProcess({
                processDefinitionKey: catalogProcessKey('PROCESS_INSTANCE_WITH_SINGLE_TASK_ASSIGNED'),
                linkedProcessInstanceType: LINK_TYPE,
            });
            parentProcessInstanceId = mainProcess.id;
            linkedOrphanProcessInstanceId = orphanProcess.id;
            await queryServiceTestUser.waitForProcessInstanceSynced(mainProcess.id);
            await queryServiceTestUser.waitForProcessInstanceSynced(orphanProcess.id);
        });

        await activiti.step('When the user links the orphan to the main process instance', async () => {
            await queryServiceTestUser.processInstances.linkProcessInstances(
                parentProcessInstanceId,
                [linkedOrphanProcessInstanceId],
                LINK_TYPE
            );
        });

        await activiti.step('Then the admin can list linked process instances', async () => {
            const linkedProcesses = await queryAdminServiceTestAdmin.waitForLinkedProcessAdmin(
                parentProcessInstanceId,
                linkedOrphanProcessInstanceId
            );
            expect(linkedProcesses.map((instance) => instance.id)).toContain(linkedOrphanProcessInstanceId);
        });
    });
});
