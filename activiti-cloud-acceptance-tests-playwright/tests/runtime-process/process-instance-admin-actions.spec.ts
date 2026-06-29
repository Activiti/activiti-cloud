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

import { randomUUID } from 'node:crypto';

import { activiti, expect } from '../../fixtures/services.fixture';
import { ProcessInstanceStatus } from '../../models/runtime-bundle.models';
import { startCatalogProcess } from '../../flows/start-catalog-process';

activiti.describe('Runtime — Process Instance Admin Actions', { tag: '@slow' }, () => {
    activiti('should cover RB admin process instance CRUD and lifecycle endpoints', async ({
        runtimeBundleServiceTestAdmin,
        runtimeAdminServiceTestAdmin,
    }) => {
        let processInstanceId = '';
        const updatedName = `admin-updated-${Date.now()}`;

        await activiti.step('Given a running process instance with variables', async () => {
            const processInstance = await startCatalogProcess(
                runtimeBundleServiceTestAdmin,
                'PROCESS_INSTANCE_WITH_VARIABLES',
                { variables: { start1: 'value1', start2: 'value2' } }
            );
            processInstanceId = processInstance.id;
            expect(processInstanceId).toBeTruthy();
        });

        await activiti.step('When the admin fetches the process instance by id', async () => {
            const instance = await runtimeAdminServiceTestAdmin.getProcessInstance(processInstanceId);
            expect(instance.id).toBe(processInstanceId);
        });

        await activiti.step('And updates the process instance name', async () => {
            const updated = await runtimeAdminServiceTestAdmin.updateProcessInstance(processInstanceId, {
                name: updatedName,
            });
            expect(updated.name).toBe(updatedName);
        });

        await activiti.step('Then the admin reads process instance variables', async () => {
            const variables = await runtimeAdminServiceTestAdmin.getProcessInstanceVariables(processInstanceId);
            expect(variables.map((variable) => variable.name)).toEqual(
                expect.arrayContaining(['start1', 'start2'])
            );
        });

        await activiti.step('When the admin suspends and resumes the process instance', async () => {
            const suspended = await runtimeAdminServiceTestAdmin.suspendProcessInstance(processInstanceId);
            expect(suspended.status).toBe(ProcessInstanceStatus.SUSPENDED);

            const resumed = await runtimeAdminServiceTestAdmin.resumeProcessInstance(processInstanceId);
            expect(resumed.status).toBe(ProcessInstanceStatus.RUNNING);
        });

        await activiti.step('Then the process instance is still running under the updated name', async () => {
            const instance = await runtimeAdminServiceTestAdmin.getProcessInstance(processInstanceId);
            expect(instance.status).toBe(ProcessInstanceStatus.RUNNING);
            expect(instance.name).toBe(updatedName);
        });
    });

    activiti('should cover RB admin subprocess and message endpoints', async ({
        runtimeBundleServiceTestAdmin,
        runtimeAdminServiceTestAdmin,
    }) => {
        const businessId = randomUUID();
        let parentProcessInstanceId = '';
        let messageProcessInstanceId = '';

        await activiti.step('Given a parent process with call activities', async () => {
            const parentProcess = await startCatalogProcess(
                runtimeBundleServiceTestAdmin,
                'PROCESS_INSTANCE_WITH_CALL_ACTIVITIES'
            );
            parentProcessInstanceId = parentProcess.id;
            await runtimeBundleServiceTestAdmin.waitForSubProcesses(parentProcessInstanceId);
        });

        await activiti.step('When the admin lists subprocesses', async () => {
            const subprocesses = await runtimeAdminServiceTestAdmin.getSubProcesses(parentProcessInstanceId);
            expect(subprocesses.length).toBeGreaterThan(0);
        });

        await activiti.step('And starts a process via admin start message', async () => {
            const processInstance = await runtimeAdminServiceTestAdmin.sendStartMessage({
                name: 'startMessage',
                businessKey: businessId,
            });
            messageProcessInstanceId = processInstance.id;
            expect(messageProcessInstanceId).toBeTruthy();
        });

        await activiti.step('Then the admin can deliver boundary and catch messages', async () => {
            const boundaryResponse = await runtimeAdminServiceTestAdmin.sendReceiveMessage({
                name: 'boundaryMessage',
                correlationKey: businessId,
            });
            expect(boundaryResponse.httpStatus).toBeLessThan(300);

            const catchResponse = await runtimeAdminServiceTestAdmin.sendReceiveMessage({
                name: 'catchMessage',
                correlationKey: businessId,
            });
            expect(catchResponse.httpStatus).toBeLessThan(300);
        });
    });
});
