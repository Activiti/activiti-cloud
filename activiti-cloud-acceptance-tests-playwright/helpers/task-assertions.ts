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

import { expect } from '@playwright/test';
import { pollOptions } from '../config/runtime/timeouts';
import { ProcessInstanceStatus } from '../models/runtime-bundle.models';
import { CloudTask, TaskStatus } from '../models/task.models';
import { QueryService } from '../services/query.service';
import { RuntimeBundleService } from '../services/runtime-bundle.service';
import { RequestResponse } from '../services/base.service';
import { TaskService } from '../services/task.service';
import { getQueryProcessInstanceWhenSynced } from './query-sync';

export function expectClientError(response: RequestResponse, messageFragment?: string): void {
    expect(response.httpStatus).toBeGreaterThanOrEqual(400);
    expect(response.httpStatus).toBeLessThan(500);
    if (messageFragment) {
        const body = JSON.stringify(response);
        expect(body).toContain(messageFragment);
    }
}

export async function getFirstProcessTask(
    taskService: TaskService,
    processInstanceId: string
): Promise<CloudTask> {
    const tasks = await taskService.getTasksByProcessInstanceId(processInstanceId);
    expect(tasks.length).toBeGreaterThan(0);
    return tasks[0];
}

export async function expectTaskStatusInRbAndQuery(
    taskService: TaskService,
    queryService: QueryService,
    taskId: string,
    expectedStatus: TaskStatus
): Promise<void> {
    if (expectedStatus !== TaskStatus.COMPLETED) {
        await expect
            .poll(async () => (await taskService.getTaskById(taskId)).status, pollOptions('querySync'))
            .toBe(expectedStatus);
    }

    await expect
        .poll(async () => {
            const task = await queryService.getTaskById(taskId);
            return task?.status;
        }, pollOptions('querySync'))
        .toBe(expectedStatus);
}

export async function expectProcessAndTaskCompleted(
    runtimeBundleService: RuntimeBundleService,
    queryService: QueryService,
    processInstanceId: string
): Promise<void> {
    await expect
        .poll(async () => {
            const instance = await getQueryProcessInstanceWhenSynced(queryService, processInstanceId);
            return instance?.status;
        }, pollOptions('querySync'))
        .toBe(ProcessInstanceStatus.COMPLETED);

    await expect(async () => {
        await runtimeBundleService.getProcessInstance(processInstanceId);
    }).rejects.toThrow();
}
