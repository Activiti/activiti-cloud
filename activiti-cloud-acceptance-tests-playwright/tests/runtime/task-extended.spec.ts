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
import { catalogProcessKey, startCatalogProcess } from '../../flows/start-catalog-process';
import { startCatalogProcessWithFirstTask } from '../../flows/start-process-with-first-task';
import { ProcessInstanceStatus } from '../../models/runtime-bundle.models';
import { TaskStatus } from '../../models/task.models';
import { scopedName } from '../../helpers/test-isolation';
import { pollOptions } from '../../config/runtime/timeouts';
import {
    expectClientError,
    expectProcessAndTaskCompleted,
    expectTaskStatusInRbAndQuery,
    getFirstProcessTask,
} from '../../helpers/task-assertions';

activiti.describe('Runtime — Task Actions (wave 2)', () => {
    activiti('should not claim a task that has already been claimed', async ({
        runtimeBundleServiceTestUser,
        taskServiceTestUser,
        taskServiceHrUser,
        queryServiceTestUser,
        queryServiceHrUser,
    }) => {
        let taskId: string;

        await activiti.step(
            'When the user starts PROCESS_INSTANCE_WITH_SINGLE_TASK_AND_USER_CANDIDATES and claims the task',
            async () => {
                const { task } = await startCatalogProcessWithFirstTask(
                    runtimeBundleServiceTestUser,
                    taskServiceTestUser,
                    'PROCESS_INSTANCE_WITH_SINGLE_TASK_AND_USER_CANDIDATES'
                );
                taskId = task.id;
                await expectTaskStatusInRbAndQuery(
                    taskServiceTestUser,
                    queryServiceTestUser,
                    taskId,
                    TaskStatus.CREATED
                );
                await taskServiceTestUser.claimTask(taskId);
                await expectTaskStatusInRbAndQuery(
                    taskServiceTestUser,
                    queryServiceTestUser,
                    taskId,
                    TaskStatus.ASSIGNED
                );
            }
        );

        await activiti.step('Then hruser cannot claim the task', async () => {
            const response = await taskServiceHrUser.claimTask(taskId);
            expectClientError(response, 'Unable to find task');
            const hrTasks = await queryServiceHrUser.getAllTasks();
            expect(hrTasks.map((task) => task.id)).not.toContain(taskId);
        });
    });

    activiti(
        'should not see tasks that belong to a different candidate group after completion',
        async ({
            runtimeBundleServiceTestUser,
            taskServiceTestUser,
            queryServiceTestUser,
            queryServiceHrUser,
        }) => {
            const processKey = catalogProcessKey(
                'PROCESS_INSTANCE_WITH_SINGLE_TASK_AND_GROUP_CANDIDATES_FOR_TESTGROUP'
            );

            await activiti.step('When testuser completes the process task', async () => {
                const processInstance = await startCatalogProcess(
                    runtimeBundleServiceTestUser,
                    'PROCESS_INSTANCE_WITH_SINGLE_TASK_AND_GROUP_CANDIDATES_FOR_TESTGROUP'
                );
                const task = await getFirstProcessTask(taskServiceTestUser, processInstance.id);
                await taskServiceTestUser.claimTask(task.id);
                await taskServiceTestUser.completeTask(task.id);
                await expectProcessAndTaskCompleted(
                    runtimeBundleServiceTestUser,
                    queryServiceTestUser,
                    processInstance.id
                );
            });

            await activiti.step('Then hruser does not see tasks for that process definition', async () => {
                const hrTasks = await queryServiceHrUser.getAllTasks();
                const processDefinitionIds = hrTasks
                    .map((task) => task.processDefinitionKey ?? task.processDefinitionId)
                    .filter(Boolean);
                expect(processDefinitionIds).not.toContain(processKey);
            });
        }
    );

    activiti('should release a standalone task', async ({
        taskServiceTestUser,
        queryServiceTestUser,
    }) => {
        let taskId: string;

        await activiti.step('When the user creates, claims, and releases a standalone task', async () => {
            const task = await taskServiceTestUser.createUnassignedStandaloneTask();
            taskId = task.id;
            await taskServiceTestUser.claimTask(taskId);
            await taskServiceTestUser.releaseTask(taskId);
        });

        await activiti.step('Then the task status is CREATED in RB and Query', async () => {
            await expectTaskStatusInRbAndQuery(
                taskServiceTestUser,
                queryServiceTestUser,
                taskId,
                TaskStatus.CREATED
            );
        });
    });

    activiti('should allow admin to delete a standalone task', async ({
        taskServiceTestAdmin,
        taskAdminServiceTestAdmin,
        queryServiceTestAdmin,
    }) => {
        let taskId: string;

        await activiti.step('Given testadmin creates a standalone task', async () => {
            const task = await taskServiceTestAdmin.createStandaloneTask();
            taskId = task.id;
        });

        await activiti.step('When the admin deletes the standalone task', async () => {
            await taskAdminServiceTestAdmin.deleteTask(taskId);
        });

        await activiti.step('Then the standalone task is deleted', async () => {
            expect(await taskServiceTestAdmin.isTaskNotFoundInRuntime(taskId)).toBe(true);
            await expect
                .poll(async () => (await queryServiceTestAdmin.getTaskById(taskId))?.status)
                .toBe(TaskStatus.CANCELLED);
        });
    });

    activiti('should query standalone tasks by name and description using LIKE operator', async ({
        testScope,
        taskServiceTestUser,
        queryServiceTestUser,
    }) => {
        const taskName = scopedName(testScope, 'like-task');
        const taskDescription = scopedName(testScope, 'like-desc');
        let taskId: string;

        await activiti.step('When the user creates a standalone task with a unique name', async () => {
            const task = await taskServiceTestUser.createStandaloneTask({
                name: taskName,
                description: taskDescription,
            });
            taskId = task.id;
        });

        await activiti.step('Then the task can be queried by name/description prefix', async () => {
            const namePrefix = taskName.substring(0, 8);
            const descriptionPrefix = taskDescription.substring(0, 8);
            await expect
                .poll(async () => {
                    const tasks = await queryServiceTestUser.getTasksByNameAndDescription(
                        namePrefix,
                        descriptionPrefix
                    );
                    return tasks.some((task) => task.id === taskId);
                })
                .toBe(true);

            const tasks = await queryServiceTestUser.getTasksByNameAndDescription(
                namePrefix,
                descriptionPrefix
            );
            for (const task of tasks) {
                expect(task.name).toContain(namePrefix);
                expect(task.description).toContain(descriptionPrefix);
            }
        });
    });

    activiti('should let admin complete tasks in a running process', async ({
        runtimeBundleServiceTestAdmin,
        taskServiceTestUser,
        taskAdminServiceTestAdmin,
        queryServiceTestUser,
    }) => {
        let processInstanceId: string;
        let taskId: string;

        await activiti.step('When testadmin starts PROCESS_INSTANCE_WITH_VARIABLES', async () => {
            const { processInstance, task } = await startCatalogProcessWithFirstTask(
                runtimeBundleServiceTestAdmin,
                taskServiceTestUser,
                'PROCESS_INSTANCE_WITH_VARIABLES'
            );
            processInstanceId = processInstance.id;
            taskId = task.id;
        });

        await activiti.step('And the admin completes the task', async () => {
            await taskAdminServiceTestAdmin.completeTask(taskId);
        });

        await activiti.step('Then the process is completed', async () => {
            await expect
                .poll(async () => {
                    try {
                        const instance = await queryServiceTestUser.getProcessInstance(processInstanceId);
                        return instance.status;
                    } catch {
                        return undefined;
                    }
                }, pollOptions('querySync'))
                .toBe(ProcessInstanceStatus.COMPLETED);
        });
    });

    activiti('should return only standalone tasks when querying standalone tasks', async ({
        runtimeBundleServiceTestUser,
        taskServiceTestUser,
        queryServiceTestUser,
    }) => {
        let standaloneTaskId: string;

        await activiti.step('When the user starts a process, claims its task, and creates a standalone task', async () => {
            const { task: processTask } = await startCatalogProcessWithFirstTask(
                runtimeBundleServiceTestUser,
                taskServiceTestUser,
                'PROCESS_INSTANCE_WITH_VARIABLES'
            );
            await taskServiceTestUser.claimTask(processTask.id);

            const standalone = await taskServiceTestUser.createStandaloneTask();
            standaloneTaskId = standalone.id;
        });

        await activiti.step('Then query standalone tasks contains only standalone tasks', async () => {
            await expect
                .poll(async () => {
                    const standaloneTasks = await queryServiceTestUser.getStandaloneTasks();
                    return standaloneTasks.some((task) => task.id === standaloneTaskId);
                })
                .toBe(true);

            const standaloneTasks = await queryServiceTestUser.getStandaloneTasks();
            expect(standaloneTasks.length).toBeGreaterThan(0);
            standaloneTasks.forEach((task) => expect(task.standalone).toBe(true));
        });
    });

    activiti('should retrieve process tasks and standalone tasks separately', async ({
        runtimeBundleServiceTestUser,
        taskServiceTestUser,
    }) => {
        await activiti.step('When the user starts a process and creates a standalone task', async () => {
            await startCatalogProcessWithFirstTask(
                runtimeBundleServiceTestUser,
                taskServiceTestUser,
                'PROCESS_INSTANCE_WITH_SINGLE_TASK_ASSIGNED'
            );
            await taskServiceTestUser.createStandaloneTask();
        });

        await activiti.step('Then RB task lists partition into standalone and process tasks', async () => {
            const allTasks = await taskServiceTestUser.getAllTasks();
            const standaloneTasks = taskServiceTestUser.filterByStandalone(allTasks, true);
            const processTasks = taskServiceTestUser.filterByStandalone(allTasks, false);

            expect(allTasks.length).toBe(standaloneTasks.length + processTasks.length);
            standaloneTasks.forEach((task) => expect(task.standalone).toBe(true));
            processTasks.forEach((task) => expect(task.standalone).toBeFalsy());
        });
    });
});
