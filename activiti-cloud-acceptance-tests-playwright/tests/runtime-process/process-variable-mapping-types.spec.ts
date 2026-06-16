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
import { pollOptions } from '../../config/runtime/timeouts';
import { EventType } from '../../models/audit.models';
import { formatDefaultUtc } from '../../helpers/date-format';

const TASK_DATE_VAR_MAPPING_PROCESS = 'taskDateVarMapping';
const PROCESS_START_EVENT_VARIABLE_MAPPING_PROCESS = 'process-b42a166d-605b-4eec-8b96-82b1253666bf';

activiti.describe('Process Variable Mapping Types', () => {
    activiti('variable types are correct for variables', async ({
        runtimeBundleServiceHrUser,
        taskServiceHrUser,
        queryServiceHrUser,
        auditServiceHrUser,
    }) => {
        const processVariableString = 'process_variable_string';
        const processVariableInteger = 'process_variable_integer';
        const processVariableBoolean = 'process_variable_boolean';
        const processVariableDate = 'process_variable_date';
        const processVariableDateTime = 'process_variable_datetime';

        const taskVariableString = 'task_variable_string';
        const taskVariableInteger = 'task_variable_integer';
        const taskVariableBoolean = 'task_variable_boolean';
        const taskVariableDate = 'task_variable_date';
        const taskVariableDateTime = 'task_variable_datetime';

        const testDate = new Date(Date.UTC(2019, 8, 9)); // 2019-09-09
        const testDateTime = new Date();
        const expectedDate = formatDefaultUtc(testDate);
        const expectedDateTime = formatDefaultUtc(testDateTime);

        let processInstanceId: string;
        let taskId: string;

        await activiti.step('When services are started', async () => {
            await queryServiceHrUser.checkServicesHealth();
            await auditServiceHrUser.checkServicesHealth();
        });

        await activiti.step(
            `When the user starts the process ${TASK_DATE_VAR_MAPPING_PROCESS}`,
            async () => {
                const processInstance = await runtimeBundleServiceHrUser.startProcessWithVariables(
                    TASK_DATE_VAR_MAPPING_PROCESS,
                    {
                        [processVariableString]: 'stringValue1',
                        [processVariableInteger]: 123,
                        [processVariableBoolean]: true,
                        [processVariableDate]: '2019-09-09',
                        [processVariableDateTime]: testDateTime.toISOString(),
                    }
                );
                processInstanceId = processInstance.id;
                expect(processInstanceId).toBeTruthy();
            }
        );

        await activiti.step('And the process variables are created', async () => {
            await expect
                .poll(async () => {
                    const vars = await runtimeBundleServiceHrUser.getProcessInstanceVariables(processInstanceId);
                    return vars.map((v) => v.name).sort();
                }, pollOptions('querySync'))
                .toEqual(
                    expect.arrayContaining([
                        processVariableString,
                        processVariableInteger,
                        processVariableBoolean,
                        processVariableDate,
                        processVariableDateTime,
                    ])
                );
        });

        await activiti.step('And variables have correct values', async () => {
            const vars = await runtimeBundleServiceHrUser.getProcessInstanceVariables(processInstanceId);
            expect(vars.find((v) => v.name === processVariableString)?.value).toBe('stringValue1');
            expect(vars.find((v) => v.name === processVariableInteger)?.value).toBe(123);
            expect(vars.find((v) => v.name === processVariableBoolean)?.value).toBe(true);
            expect(vars.find((v) => v.name === processVariableDate)?.value).toBe(expectedDate);
            expect(vars.find((v) => v.name === processVariableDateTime)?.value).toBe(expectedDateTime);
        });

        await activiti.step('And variables have correct types in rb', async () => {
            const vars = await runtimeBundleServiceHrUser.getProcessInstanceVariables(processInstanceId);
            expect(vars.find((v) => v.name === processVariableString)?.type).toBe('string');
            expect(vars.find((v) => v.name === processVariableInteger)?.type).toBe('integer');
            expect(vars.find((v) => v.name === processVariableBoolean)?.type).toBe('boolean');
            expect(vars.find((v) => v.name === processVariableDate)?.type).toBe('date');
            expect(vars.find((v) => v.name === processVariableDateTime)?.type).toBe('date');
        });

        await activiti.step('And check variables in query', async () => {
            await expect
                .poll(async () => {
                    const vars = await queryServiceHrUser.getProcessInstanceVariables(processInstanceId);
                    return vars.map((v) => v.name).sort();
                }, pollOptions('querySync'))
                .toEqual(
                    expect.arrayContaining([
                        processVariableString,
                        processVariableInteger,
                        processVariableBoolean,
                        processVariableDate,
                        processVariableDateTime,
                    ])
                );
        });

        await activiti.step('And variables was created event in audit', async () => {
            const expectedNames = [
                processVariableString,
                processVariableInteger,
                processVariableBoolean,
                processVariableDate,
                processVariableDateTime,
            ];
            await expect
                .poll(async () => {
                    const events = await auditServiceHrUser.getEvents({
                        processInstanceId,
                        eventType: EventType.VARIABLE_CREATED,
                    });
                    return events
                        .map((e) => (e.entity as { name?: string } | undefined)?.name)
                        .filter((n): n is string => typeof n === 'string');
                }, pollOptions('auditEvents'))
                .toEqual(expect.arrayContaining(expectedNames));
        });

        await activiti.step('And variables values created in task with variable mapping are correct', async () => {
            await expect
                .poll(async () => {
                    const tasks = await taskServiceHrUser.getTasksByProcessInstanceId(processInstanceId);
                    return tasks.find((t) => t.name === 'My task1');
                }, pollOptions('querySync'))
                .toBeDefined();

            const tasks = await taskServiceHrUser.getTasksByProcessInstanceId(processInstanceId);
            const matched = tasks.find((t) => t.name === 'My task1')!;
            taskId = matched.id;
            expect(matched.status).toBe('CREATED');

            const taskVars = await taskServiceHrUser.getTaskVariables(taskId);
            expect(taskVars.find((v) => v.name === taskVariableString)?.value).toBe('stringValue1');
            expect(taskVars.find((v) => v.name === taskVariableInteger)?.value).toBe(123);
            expect(taskVars.find((v) => v.name === taskVariableBoolean)?.value).toBe(true);
            expect(taskVars.find((v) => v.name === taskVariableDate)?.value).toBe(expectedDate);
            expect(taskVars.find((v) => v.name === taskVariableDateTime)?.value).toBe(expectedDateTime);
        });

        await activiti.step('And variables types in task are correct', async () => {
            const taskVars = await taskServiceHrUser.getTaskVariables(taskId);
            expect(taskVars.find((v) => v.name === taskVariableString)?.type).toBe('string');
            expect(taskVars.find((v) => v.name === taskVariableInteger)?.type).toBe('integer');
            expect(taskVars.find((v) => v.name === taskVariableBoolean)?.type).toBe('boolean');
            expect(taskVars.find((v) => v.name === taskVariableDate)?.type).toBe('date');
            expect(taskVars.find((v) => v.name === taskVariableDateTime)?.type).toBe('date');
        });

        await activiti.step('When the user ask to claim the task', async () => {
            await taskServiceHrUser.claimTask(taskId);
        });

        await activiti.step('When update task variables', async () => {
            await taskServiceHrUser.updateTaskVariable(taskId, taskVariableString, 'string321');
            await taskServiceHrUser.updateTaskVariable(taskId, taskVariableInteger, 321);
            await taskServiceHrUser.updateTaskVariable(taskId, taskVariableBoolean, false);
        });

        await activiti.step('And the user ask to complete the task', async () => {
            await taskServiceHrUser.completeTask(taskId);
        });

        await activiti.step('Then variables have correct values in process', async () => {
            await expect
                .poll(async () => {
                    const vars = await runtimeBundleServiceHrUser.getProcessInstanceVariables(processInstanceId);
                    return {
                        s: vars.find((v) => v.name === processVariableString)?.value,
                        i: vars.find((v) => v.name === processVariableInteger)?.value,
                        b: vars.find((v) => v.name === processVariableBoolean)?.value,
                        d: vars.find((v) => v.name === processVariableDate)?.value,
                        dt: vars.find((v) => v.name === processVariableDateTime)?.value,
                    };
                }, pollOptions('querySync'))
                .toEqual({
                    s: 'string321',
                    i: 321,
                    b: false,
                    d: expectedDate,
                    dt: expectedDateTime,
                });
        });
    });

    activiti('variables mapping for the process start event', async ({
        runtimeBundleServiceHrUser,
        taskServiceHrUser,
    }) => {
        let processInstanceId: string;
        let taskId: string;

        await activiti.step('When the user starts variables mapping process on start event', async () => {
            const processInstance = await runtimeBundleServiceHrUser.startProcessWithVariables(
                PROCESS_START_EVENT_VARIABLE_MAPPING_PROCESS,
                { Text0xfems: 'Form name', Text0rvs0o: 'Form email' }
            );
            processInstanceId = processInstance.id;
            expect(processInstanceId).toBeTruthy();
        });

        await activiti.step('Then process variables are properly mapped on start event', async () => {
            await expect
                .poll(async () => {
                    const vars = await runtimeBundleServiceHrUser.getProcessInstanceVariables(processInstanceId);
                    return vars
                        .map((v) => ({ name: v.name, value: v.value }))
                        .sort((a, b) => a.name.localeCompare(b.name));
                }, pollOptions('querySync'))
                .toEqual([
                    { name: 'email', value: 'Form email' },
                    { name: 'name', value: 'Form name' },
                ]);
        });

        await activiti.step('And process variables are properly mapped to the task variables', async () => {
            await expect
                .poll(async () => {
                    return taskServiceHrUser.getTasksByProcessInstanceId(processInstanceId);
                }, pollOptions('querySync'))
                .toHaveLength(1);

            const all = await taskServiceHrUser.getTasksByProcessInstanceId(processInstanceId);
            taskId = all[0].id;

            await expect
                .poll(async () => {
                    const vars = await taskServiceHrUser.getTaskVariables(taskId);
                    return vars
                        .map((v) => ({ name: v.name, value: v.value }))
                        .sort((a, b) => a.name.localeCompare(b.name));
                }, pollOptions('querySync'))
                .toEqual([
                    { name: 'Text0rvs0o', value: 'Form email' },
                    { name: 'Text0xfems', value: 'Form name' },
                ]);
        });

        await activiti.step('And the user may complete the task', async () => {
            await taskServiceHrUser.completeTask(taskId);
        });
    });
});
