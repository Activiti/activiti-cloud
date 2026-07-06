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
import { startCatalogProcessWithFirstTask } from '../../flows/start-process-with-first-task';
import { IntegrationContextStatus } from '../../models/runtime-bundle.models';

const LINK_TYPE = 'acceptance-link-type';
const CONNECTOR_PROCESS = 'ConnectorProcess';

activiti.describe('Runtime — Query Remaining Actions', () => {
    activiti('should cover remaining query user endpoints', async ({
        runtimeBundleServiceTestUser,
        taskServiceTestUser,
        queryServiceTestUser,
    }) => {
        let taskId = '';
        let parentProcessInstanceId = '';
        let linkedOrphanProcessInstanceId = '';

        await activiti.step('Given a process task synced to query', async () => {
            const { processInstance, task } = await startCatalogProcessWithFirstTask(
                runtimeBundleServiceTestUser,
                taskServiceTestUser,
                'PROCESS_INSTANCE_WITH_SINGLE_TASK_ASSIGNED'
            );
            taskId = task.id;
            parentProcessInstanceId = processInstance.id;
            await queryServiceTestUser.waitForTaskById(taskId, () => true);
        });

        await activiti.step('When the user fetches the task by path', async () => {
            const task = await queryServiceTestUser.tasks.getTask(taskId);
            expect(task.id).toBe(taskId);
        });

        await activiti.step('And lists subprocesses via the user query API', async () => {
            const parentProcess = await startCatalogProcess(
                runtimeBundleServiceTestUser,
                'PROCESS_INSTANCE_WITH_CALL_ACTIVITIES'
            );
            await runtimeBundleServiceTestUser.waitForSubProcesses(parentProcess.id);
            await queryServiceTestUser.waitForProcessInstanceSynced(parentProcess.id);
            const subprocesses = await queryServiceTestUser.waitForSubprocesses(parentProcess.id);
            expect(subprocesses.length).toBeGreaterThan(0);
        });

        await activiti.step('When the user links an orphan process instance to a main process', async () => {
            const orphanProcess = await runtimeBundleServiceTestUser.startProcess({
                processDefinitionKey: catalogProcessKey('PROCESS_INSTANCE_WITH_SINGLE_TASK_ASSIGNED'),
                linkedProcessInstanceType: LINK_TYPE,
            });
            linkedOrphanProcessInstanceId = orphanProcess.id;
            await queryServiceTestUser.waitForProcessInstanceSynced(linkedOrphanProcessInstanceId);
            await queryServiceTestUser.processInstances.linkProcessInstances(
                parentProcessInstanceId,
                [linkedOrphanProcessInstanceId],
                LINK_TYPE
            );
        });

        await activiti.step('Then the orphan process is linked to the main process instance', async () => {
            const linkedProcess = await queryServiceTestUser.waitForLinkedProcess(
                parentProcessInstanceId,
                linkedOrphanProcessInstanceId
            );
            expect(linkedProcess.linkedProcessInstanceId).toBe(parentProcessInstanceId);
        });
    });

    activiti('should cover remaining query admin endpoints', { tag: '@slow' }, async ({
        runtimeBundleServiceTestAdmin,
        queryAdminServiceTestAdmin,
    }) => {
        let integrationContextId = '';

        await activiti.step('When the admin lists applications', async () => {
            const applications = await queryAdminServiceTestAdmin.adminApplications.getApplications();
            expect(applications.map((application) => application.name)).toContain('default-app');
        });

        await activiti.step('Given a completed connector process with an integration context', async () => {
            const processInstance = await runtimeBundleServiceTestAdmin.startProcess({
                processDefinitionKey: CONNECTOR_PROCESS,
            });
            const tasks = await queryAdminServiceTestAdmin.waitForServiceTasksForProcessInstance(
                processInstance.id,
                (list) => list.length === 1,
                `single service task for process ${processInstance.id}`
            );
            const integrationContext = await queryAdminServiceTestAdmin.waitForServiceTaskIntegrationContext(
                tasks[0].id,
                (context) =>
                    context.clientType === 'ServiceTask' &&
                    context.status === IntegrationContextStatus.INTEGRATION_RESULT_RECEIVED
            );
            integrationContextId = integrationContext.id!;
            expect(integrationContextId).toBeTruthy();
        });

        await activiti.step('Then the admin can fetch the integration context by id', async () => {
            const integrationContext =
                await queryAdminServiceTestAdmin.adminIntegrationContexts.getIntegrationContext(integrationContextId);
            expect(integrationContext.id).toBe(integrationContextId);
            expect(integrationContext.clientType).toBe('ServiceTask');
        });
    });
});
