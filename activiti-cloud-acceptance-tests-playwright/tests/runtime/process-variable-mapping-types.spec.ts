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
import { CloudVariableInstance } from '../../models/process-variable.models';

const TASK_DATE_VAR_MAPPING_PROCESS = 'taskDateVarMapping';
const PROCESS_START_EVENT_VARIABLE_MAPPING_PROCESS = 'process-b42a166d-605b-4eec-8b96-82b1253666bf';

// Mirrors Java DEFAULT_FORMAT "yyyy-MM-dd'T'HH:mm:ss.SSSZ" in UTC (e.g. "2019-09-09T00:00:00.000+0000").
function formatDefaultUtc(date: Date): string {
    const pad = (n: number, w = 2) => String(n).padStart(w, '0');
    const y = date.getUTCFullYear();
    const M = pad(date.getUTCMonth() + 1);
    const d = pad(date.getUTCDate());
    const h = pad(date.getUTCHours());
    const m = pad(date.getUTCMinutes());
    const s = pad(date.getUTCSeconds());
    const ms = pad(date.getUTCMilliseconds(), 3);
    return `${y}-${M}-${d}T${h}:${m}:${s}.${ms}+0000`;
}

// Mirrors Java ISO_FORMAT "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'" in UTC (e.g. "2026-05-29T13:00:00.000Z").
function formatIsoUtc(date: Date): string {
    return date.toISOString();
}

function findVar(vars: CloudVariableInstance[], name: string): CloudVariableInstance | undefined {
    return vars.find((v) => v.name === name);
}

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
                        [processVariableDateTime]: formatIsoUtc(testDateTime),
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
            expect(findVar(vars, processVariableString)?.value).toBe('stringValue1');
            expect(findVar(vars, processVariableInteger)?.value).toBe(123);
            expect(findVar(vars, processVariableBoolean)?.value).toBe(true);
            expect(findVar(vars, processVariableDate)?.value).toBe(expectedDate);
            expect(findVar(vars, processVariableDateTime)?.value).toBe(expectedDateTime);
        });

        await activiti.step('And variables have correct types in rb', async () => {
            const vars = await runtimeBundleServiceHrUser.getProcessInstanceVariables(processInstanceId);
            expect(findVar(vars, processVariableString)?.type).toBe('string');
            expect(findVar(vars, processVariableInteger)?.type).toBe('integer');
            expect(findVar(vars, processVariableBoolean)?.type).toBe('boolean');
            expect(findVar(vars, processVariableDate)?.type).toBe('date');
            expect(findVar(vars, processVariableDateTime)?.type).toBe('date');
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
            const task = await expect
                .poll(async () => {
                    const tasks = await taskServiceHrUser.getTasksByProcessInstanceId(processInstanceId);
                    return tasks.find((t) => t.name === 'My task1');
                }, pollOptions('querySync'))
                .toBeDefined();
            void task;

            const tasks = await taskServiceHrUser.getTasksByProcessInstanceId(processInstanceId);
            const matched = tasks.find((t) => t.name === 'My task1')!;
            taskId = matched.id;
            expect(matched.status).toBe('CREATED');

            const taskVars = await taskServiceHrUser.getTaskVariables(taskId);
            expect(findVar(taskVars, taskVariableString)?.value).toBe('stringValue1');
            expect(findVar(taskVars, taskVariableInteger)?.value).toBe(123);
            expect(findVar(taskVars, taskVariableBoolean)?.value).toBe(true);
            expect(findVar(taskVars, taskVariableDate)?.value).toBe(expectedDate);
            expect(findVar(taskVars, taskVariableDateTime)?.value).toBe(expectedDateTime);
        });

        await activiti.step('And variables types in task are correct', async () => {
            const taskVars = await taskServiceHrUser.getTaskVariables(taskId);
            expect(findVar(taskVars, taskVariableString)?.type).toBe('string');
            expect(findVar(taskVars, taskVariableInteger)?.type).toBe('integer');
            expect(findVar(taskVars, taskVariableBoolean)?.type).toBe('boolean');
            expect(findVar(taskVars, taskVariableDate)?.type).toBe('date');
            expect(findVar(taskVars, taskVariableDateTime)?.type).toBe('date');
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
                        s: findVar(vars, processVariableString)?.value,
                        i: findVar(vars, processVariableInteger)?.value,
                        b: findVar(vars, processVariableBoolean)?.value,
                        d: findVar(vars, processVariableDate)?.value,
                        dt: findVar(vars, processVariableDateTime)?.value,
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
            const tasks = await expect
                .poll(async () => {
                    return taskServiceHrUser.getTasksByProcessInstanceId(processInstanceId);
                }, pollOptions('querySync'))
                .toHaveLength(1);
            void tasks;

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
