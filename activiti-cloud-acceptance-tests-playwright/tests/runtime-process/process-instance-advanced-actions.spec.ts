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
import { ProcessInstanceStatus } from '../../models/runtime-bundle.models';
import { catalogProcessKey } from '../../flows/start-catalog-process';
import { startCatalogProcessWithFirstTask } from '../../flows/start-process-with-first-task';

const SIGNAL_CATCH_EVENT_PROCESS = 'SignalCatchEventProcess';
const SIGNAL_NAME = 'Test';

activiti.describe('Runtime — Process Instance Advanced Actions', { tag: '@slow' }, () => {
    activiti('should create, start, signal, and claim next task via RB advanced endpoints', async ({
        runtimeBundleServiceTestUser,
        taskServiceTestUser,
        queryServiceTestUser,
    }) => {
        let createdProcessInstanceId = '';

        await activiti.step('When the user creates a process instance without starting it', async () => {
            const created = await runtimeBundleServiceTestUser.createProcess({
                processDefinitionKey: catalogProcessKey('PROCESS_INSTANCE_WITH_SINGLE_TASK_ASSIGNED'),
                businessKey: `create-start-${Date.now()}`,
            });
            createdProcessInstanceId = created.id;
            expect(created.status).toBe(ProcessInstanceStatus.CREATED);
        });

        await activiti.step('And starts the created process instance', async () => {
            const started = await runtimeBundleServiceTestUser.startCreatedProcess(createdProcessInstanceId);
            expect(started.status).toBe(ProcessInstanceStatus.RUNNING);
        });

        await activiti.step('Then the process has an open task in runtime', async () => {
            const task = await taskServiceTestUser.waitForOpenTaskByProcessInstanceId(createdProcessInstanceId);
            expect(task.processInstanceId).toBe(createdProcessInstanceId);
        });

        let catchProcessInstanceId = '';

        await activiti.step('Given a process waiting for signal Test', async () => {
            const catchProcess = await runtimeBundleServiceTestUser.startProcess({
                processDefinitionKey: SIGNAL_CATCH_EVENT_PROCESS,
            });
            catchProcessInstanceId = catchProcess.id;
            expect(catchProcessInstanceId).toBeTruthy();
            await queryServiceTestUser.waitForProcessInstanceSynced(catchProcessInstanceId);
        });

        await activiti.step('When the user broadcasts signal Test via RB', async () => {
            const response = await runtimeBundleServiceTestUser.sendSignal(SIGNAL_NAME);
            expect(response.httpStatus).toBeLessThan(300);
        });

        await activiti.step('Then the signal catch process completes in query', async () => {
            const instance = await queryServiceTestUser.waitForProcessInstanceStatus(
                catchProcessInstanceId,
                ProcessInstanceStatus.COMPLETED
            );
            expect(instance.status).toBe(ProcessInstanceStatus.COMPLETED);
        });

        await activiti.step('When the user starts a process with group candidates', async () => {
            await startCatalogProcessWithFirstTask(
                runtimeBundleServiceTestUser,
                taskServiceTestUser,
                'PROCESS_INSTANCE_WITH_SINGLE_TASK_AND_GROUP_CANDIDATES'
            );
        });

        await activiti.step('Then POST /tasks/next returns a claimable task', async () => {
            const nextTask = await taskServiceTestUser.getNextTask();
            expect(nextTask?.id).toBeTruthy();
        });
    });
});
