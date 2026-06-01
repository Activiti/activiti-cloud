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

import { activiti, expect } from '../../fixtures/services.fixture';
import { TaskStatus } from '../../models/task.models';
import { startCatalogProcessWithFirstTask } from '../../flows/start-process-with-first-task';
import {
    expectClientError,
    expectProcessAndTaskCompleted,
    expectTaskStatusInRbAndQuery,
} from '../../helpers/task-assertions';

activiti.describe('Runtime — Task Actions (wave 1)', () => {
    activiti('should claim and complete tasks in a running process', async ({
        runtimeBundleServiceTestUser,
        taskServiceTestUser,
        queryServiceTestUser,
    }) => {
        let processInstanceId: string;
        let taskId: string;

        await activiti.step(
            'When the user starts an instance of the process called PROCESS_INSTANCE_WITH_VARIABLES',
            async () => {
                const { processInstance, task } = await startCatalogProcessWithFirstTask(
                    runtimeBundleServiceTestUser,
                    taskServiceTestUser,
                    'PROCESS_INSTANCE_WITH_VARIABLES'
                );
                processInstanceId = processInstance.id;
                taskId = task.id;
            }
        );

        await activiti.step('And the user claims the task', async () => {
            await taskServiceTestUser.claimTask(taskId);
        });

        await activiti.step('And the user completes the task', async () => {
            await taskServiceTestUser.completeTask(taskId);
        });

        await activiti.step('Then the status of the process and the task is changed to completed', async () => {
            await expectProcessAndTaskCompleted(
                runtimeBundleServiceTestUser,
                queryServiceTestUser,
                processInstanceId
            );
            await expectTaskStatusInRbAndQuery(
                taskServiceTestUser,
                queryServiceTestUser,
                taskId,
                TaskStatus.COMPLETED
            );
        });
    });

    activiti('should create a standalone task', async ({
        taskServiceTestUser,
        queryServiceTestUser,
    }) => {
        let taskId: string;

        await activiti.step('When the user creates a standalone task', async () => {
            const task = await taskServiceTestUser.createStandaloneTask();
            taskId = task.id;
            expect(taskId).toBeTruthy();
        });

        await activiti.step('Then the created task has a status assigned', async () => {
            await expectTaskStatusInRbAndQuery(
                taskServiceTestUser,
                queryServiceTestUser,
                taskId,
                TaskStatus.ASSIGNED
            );
        });
    });

    activiti('should delete a standalone task', async ({
        taskServiceTestUser,
        queryServiceTestUser,
    }) => {
        let taskId: string;

        await activiti.step('Given the user creates a standalone task', async () => {
            const task = await taskServiceTestUser.createStandaloneTask();
            taskId = task.id;
        });

        await activiti.step('When the user deletes the standalone task', async () => {
            await taskServiceTestUser.deleteTask(taskId);
        });

        await activiti.step('Then the standalone task is deleted', async () => {
            expect(await taskServiceTestUser.isTaskNotFoundInRuntime(taskId)).toBe(true);
            const queryTask = await queryServiceTestUser.getTaskById(taskId);
            expect(queryTask?.status).toBe(TaskStatus.CANCELLED);
        });
    });

    activiti('should create a subtask', async ({ taskServiceTestUser, queryServiceTestUser }) => {
        let parentTaskId: string;
        let subtaskId: string;

        await activiti.step('When the user creates a standalone task', async () => {
            const task = await taskServiceTestUser.createStandaloneTask();
            parentTaskId = task.id;
        });

        await activiti.step('And user creates a subtask for the previously created task', async () => {
            const subtask = await taskServiceTestUser.createSubtask(parentTaskId);
            subtaskId = subtask.id;
            expect(subtask.parentTaskId).toBeTruthy();
        });

        await activiti.step('Then the subtask is created and references another task', async () => {
            const created = await taskServiceTestUser.getTaskById(subtaskId);
            expect(created.parentTaskId?.toLowerCase()).toBe(parentTaskId.toLowerCase());
            const querySubtask = await queryServiceTestUser.getTaskById(subtaskId);
            expect(querySubtask?.parentTaskId?.toLowerCase()).toBe(parentTaskId.toLowerCase());
        });
    });

    activiti('should get a list of subtasks', async ({ taskServiceTestUser }) => {
        let parentTaskId: string;
        let subtaskId: string;

        await activiti.step('When the user creates a standalone task', async () => {
            const task = await taskServiceTestUser.createStandaloneTask();
            parentTaskId = task.id;
        });

        await activiti.step('And user creates a subtask for the previously created task', async () => {
            const subtask = await taskServiceTestUser.createSubtask(parentTaskId);
            subtaskId = subtask.id;
        });

        await activiti.step('Then a list of one subtask is be available for the task', async () => {
            const subtasks = await taskServiceTestUser.getSubtasks(parentTaskId);
            expect(subtasks.map((task) => task.id)).toEqual([subtaskId]);
        });
    });

    activiti('should create a process with assigned tasks and complete it', async ({
        runtimeBundleServiceTestUser,
        taskServiceTestUser,
        queryServiceTestUser,
    }) => {
        let processInstanceId: string;
        let taskId: string;

        await activiti.step(
            'When the user starts an instance of the process called PROCESS_INSTANCE_WITH_SINGLE_TASK_ASSIGNED',
            async () => {
                const { processInstance, task } = await startCatalogProcessWithFirstTask(
                    runtimeBundleServiceTestUser,
                    taskServiceTestUser,
                    'PROCESS_INSTANCE_WITH_SINGLE_TASK_ASSIGNED'
                );
                processInstanceId = processInstance.id;
                taskId = task.id;
            }
        );

        await activiti.step('And the status of the task since the beginning is ASSIGNED', async () => {
            await expectTaskStatusInRbAndQuery(
                taskServiceTestUser,
                queryServiceTestUser,
                taskId,
                TaskStatus.ASSIGNED
            );
        });

        await activiti.step('And the user completes the task', async () => {
            await taskServiceTestUser.completeTask(taskId);
        });

        await activiti.step('Then the status of the process and the task is changed to completed', async () => {
            await expectProcessAndTaskCompleted(
                runtimeBundleServiceTestUser,
                queryServiceTestUser,
                processInstanceId
            );
        });
    });

    activiti('should create a process with user candidates, claim a task and complete it', async ({
        runtimeBundleServiceTestUser,
        taskServiceTestUser,
        queryServiceTestUser,
    }) => {
        let processInstanceId: string;
        let taskId: string;

        await activiti.step(
            'When the user starts an instance of the process called PROCESS_INSTANCE_WITH_SINGLE_TASK_AND_USER_CANDIDATES',
            async () => {
                const { processInstance, task } = await startCatalogProcessWithFirstTask(
                    runtimeBundleServiceTestUser,
                    taskServiceTestUser,
                    'PROCESS_INSTANCE_WITH_SINGLE_TASK_AND_USER_CANDIDATES'
                );
                processInstanceId = processInstance.id;
                taskId = task.id;
            }
        );

        await activiti.step('And the status of the task is CREATED', async () => {
            await expectTaskStatusInRbAndQuery(
                taskServiceTestUser,
                queryServiceTestUser,
                taskId,
                TaskStatus.CREATED
            );
        });

        await activiti.step('And the user claims the task', async () => {
            await taskServiceTestUser.claimTask(taskId);
        });

        await activiti.step('And the user completes the task', async () => {
            await taskServiceTestUser.completeTask(taskId);
        });

        await activiti.step('Then the status of the process and the task is changed to completed', async () => {
            await expectProcessAndTaskCompleted(
                runtimeBundleServiceTestUser,
                queryServiceTestUser,
                processInstanceId
            );
        });
    });

    activiti('should create a process with group candidates, claim a task and complete it', async ({
        runtimeBundleServiceTestUser,
        taskServiceTestUser,
        queryServiceTestUser,
    }) => {
        let processInstanceId: string;
        let taskId: string;

        await activiti.step(
            'When the user starts an instance of the process called PROCESS_INSTANCE_WITH_SINGLE_TASK_AND_GROUP_CANDIDATES',
            async () => {
                const { processInstance, task } = await startCatalogProcessWithFirstTask(
                    runtimeBundleServiceTestUser,
                    taskServiceTestUser,
                    'PROCESS_INSTANCE_WITH_SINGLE_TASK_AND_GROUP_CANDIDATES'
                );
                processInstanceId = processInstance.id;
                taskId = task.id;
            }
        );

        await activiti.step('And the status of the task is CREATED', async () => {
            await expectTaskStatusInRbAndQuery(
                taskServiceTestUser,
                queryServiceTestUser,
                taskId,
                TaskStatus.CREATED
            );
        });

        await activiti.step('And the user claims the task', async () => {
            await taskServiceTestUser.claimTask(taskId);
        });

        await activiti.step('And the user completes the task', async () => {
            await taskServiceTestUser.completeTask(taskId);
        });

        await activiti.step('Then the status of the process and the task is changed to completed', async () => {
            await expectProcessAndTaskCompleted(
                runtimeBundleServiceTestUser,
                queryServiceTestUser,
                processInstanceId
            );
        });
    });

    activiti('should not complete a task that has already been completed', async ({
        runtimeBundleServiceTestUser,
        taskServiceTestUser,
    }) => {
        let taskId: string;

        await activiti.step(
            'When the user starts an instance of the process called PROCESS_INSTANCE_WITH_SINGLE_TASK_ASSIGNED',
            async () => {
                const { task } = await startCatalogProcessWithFirstTask(
                    runtimeBundleServiceTestUser,
                    taskServiceTestUser,
                    'PROCESS_INSTANCE_WITH_SINGLE_TASK_ASSIGNED'
                );
                taskId = task.id;
            }
        );

        await activiti.step('And the user completes the task', async () => {
            await taskServiceTestUser.completeTask(taskId);
        });

        await activiti.step('Then the user cannot complete the task', async () => {
            const response = await taskServiceTestUser.completeTask(taskId);
            expectClientError(response, 'Unable to find task');
        });
    });

    activiti('should not claim a task that belongs to different candidate group', async ({
        runtimeBundleServiceTestUser,
        taskServiceTestUser,
        taskServiceHrUser,
        queryServiceHrUser,
    }) => {
        let taskId: string;

        await activiti.step(
            'When the user starts an instance of the process called PROCESS_INSTANCE_WITH_SINGLE_TASK_AND_GROUP_CANDIDATES_FOR_TESTGROUP',
            async () => {
                const { task } = await startCatalogProcessWithFirstTask(
                    runtimeBundleServiceTestUser,
                    taskServiceTestUser,
                    'PROCESS_INSTANCE_WITH_SINGLE_TASK_AND_GROUP_CANDIDATES_FOR_TESTGROUP'
                );
                taskId = task.id;
            }
        );

        await activiti.step('And another user is authenticated as hruser', async () => {
            // hruser context is provided by taskServiceHrUser fixture
        });

        await activiti.step('Then the task cannot be claimed by user', async () => {
            const response = await taskServiceHrUser.claimTask(taskId);
            expectClientError(response, 'Unable to find task');
            const hrTasks = await queryServiceHrUser.getAllTasks();
            expect(hrTasks.map((task) => task.id)).not.toContain(taskId);
        });
    });
});
