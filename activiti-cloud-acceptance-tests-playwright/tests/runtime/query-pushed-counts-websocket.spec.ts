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
import { pollOptions } from '../../config/runtime/timeouts';
import { startCatalogProcessWithFirstTask } from '../../flows/start-process-with-first-task';
import { ProcessInstanceStatus } from '../../models/runtime-bundle.models';
import type { PushedCountsSubscription } from '../../services/notifications';
import { openPushedCountsSubscription } from '../../services/notifications';

activiti.describe('Runtime — Query Pushed Counts WebSocket Subscription', () => {
    let activeSubscription: PushedCountsSubscription | undefined;

    activiti.afterEach(() => {
        activeSubscription?.close();
        activeSubscription = undefined;
    });

    activiti(
        'should push count increases as a process is started and its task is claimed',
        async ({ runtimeBundleServiceTestUser, taskServiceTestUser, testUserContext }) => {
            let taskId = '';
            let processInstanceId = '';
            let runningCountBeforeStart: number | undefined;
            let assignedCountBeforeClaim: number | undefined;
            let queuedCountBeforeStart: number | undefined;
            let runningCountAfterStart = 0;

            await activiti.step('When the user subscribes to all pushed count types', async () => {
                activeSubscription = await openPushedCountsSubscription({
                    accessToken: testUserContext.token,
                    countTypes: ['runningProcesses', 'assignedTasks', 'queuedTasks'],
                });
            });

            await activiti.step('And records any available count baselines', async () => {
                const counts = activeSubscription!.getLatestCounts();
                runningCountBeforeStart = counts.runningProcesses?.count;
                assignedCountBeforeClaim = counts.assignedTasks?.count;
                queuedCountBeforeStart = counts.queuedTasks?.count;
            });

            await activiti.step('When a process with an unassigned task is started', async () => {
                const { processInstance, task } = await startCatalogProcessWithFirstTask(
                    runtimeBundleServiceTestUser,
                    taskServiceTestUser,
                    'PROCESS_INSTANCE_WITH_SINGLE_TASK_AND_USER_CANDIDATES'
                );
                processInstanceId = processInstance.id;
                taskId = task.id;
            });

            await activiti.step('Then the running process count increases', async () => {
                await expect
                    .poll(
                        () => activeSubscription!.getLatestCounts().runningProcesses?.count ?? -1,
                        pollOptions()
                    )
                    .toBeGreaterThan(runningCountBeforeStart ?? 0);
                runningCountAfterStart = activeSubscription!.getLatestCounts().runningProcesses!.count;
            });

            await activiti.step('And the queued task count increases', async () => {
                await expect
                    .poll(
                        () => activeSubscription!.getLatestCounts().queuedTasks?.count ?? -1,
                        pollOptions()
                    )
                    .toBeGreaterThan(queuedCountBeforeStart ?? 0);
                const countsAfterStart = activeSubscription!.getLatestCounts();
                assignedCountBeforeClaim = countsAfterStart.assignedTasks?.count ?? assignedCountBeforeClaim;
            });

            await activiti.step('When the user claims the task', async () => {
                const response = await taskServiceTestUser.tasks.claimTask(taskId);
                expect(response.httpStatus).toBeLessThan(300);
            });

            await activiti.step('Then the assigned task count increases', async () => {
                await expect
                    .poll(
                        () => activeSubscription!.getLatestCounts().assignedTasks?.count ?? -1,
                        pollOptions()
                    )
                    .toBeGreaterThan(assignedCountBeforeClaim ?? 0);
            });
        }
    );
});
