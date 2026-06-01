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
import { expectPoll } from '../../helpers/expect-poll';
import { catalogProcessKey, startCatalogProcess } from '../../flows/start-catalog-process';
import { startCatalogProcessWithFirstTask } from '../../flows/start-process-with-first-task';
import { getQueryProcessInstanceWhenSynced } from '../../helpers/query-sync';
import { ProcessInstanceStatus } from '../../models/runtime-bundle.models';
import { TaskStatus } from '../../models/task.models';
import { scopedName } from '../../helpers/test-isolation';
import {
    expectClientError,
    expectProcessAndTaskCompleted,
    expectTaskStatusInRbAndQuery,
    getFirstProcessTask,
} from '../../helpers/task-assertions';
import { expectProcessVariableValue } from '../../helpers/process-variables';
import { expectTaskVariable, expectTaskVariableValue } from '../../helpers/task-variables';
import { ProcessDefinitionRegistry } from '../../models/process-definition-registry';

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
            await expectPoll(
                async () => (await queryServiceTestAdmin.getTaskById(taskId))?.status,
                'querySync'
            ).toBe(TaskStatus.CANCELLED);
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
            await expectPoll(async () => {
                const tasks = await queryServiceTestUser.getTasksByNameAndDescription(
                    namePrefix,
                    descriptionPrefix
                );
                return tasks.some((task) => task.id === taskId);
            }, 'querySync').toBe(true);

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
            await expectPoll(async () => {
                const instance = await getQueryProcessInstanceWhenSynced(
                    queryServiceTestUser,
                    processInstanceId
                );
                return instance?.status;
            }, 'querySync').toBe(ProcessInstanceStatus.COMPLETED);
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
            await expectPoll(async () => {
                const standaloneTasks = await queryServiceTestUser.getStandaloneTasks();
                return standaloneTasks.some((task) => task.id === standaloneTaskId);
            }, 'querySync').toBe(true);

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

    activiti(
        'should create subprocess task when starting parent process with call activities',
        async ({ runtimeBundleServiceTestUser, queryServiceTestUser }) => {
            let parentProcessInstanceId: string;

            await activiti.step(
                'When the user starts an instance of the process called PROCESS_INSTANCE_WITH_CALL_ACTIVITIES',
                async () => {
                    const processInstance = await startCatalogProcess(
                        runtimeBundleServiceTestUser,
                        'PROCESS_INSTANCE_WITH_CALL_ACTIVITIES'
                    );
                    parentProcessInstanceId = processInstance.id;
                }
            );

            await activiti.step(
                'Then the task from SUB_PROCESS_INSTANCE_WITH_TASK is CREATED and it is called subprocess-task',
                async () => {
                    await expectPoll(async () => {
                        const subprocesses =
                            await runtimeBundleServiceTestUser.getSubProcesses(parentProcessInstanceId);
                        if (subprocesses.length === 0) {
                            return false;
                        }
                        const subprocessTasks = await queryServiceTestUser.getTasksByProcessInstanceId(
                            subprocesses[0].id
                        );
                        return subprocessTasks.some(
                            (task) => task.name === 'subprocess-task' && task.status === TaskStatus.CREATED
                        );
                    }, 'querySync').toBe(true);
                }
            );
        }
    );

    activiti('should expose formKey on task and matching process instance fields', async ({
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

        await activiti.step('Then the task has the formKey field and correct processInstance fields', async () => {
            const processFromQuery = await queryServiceTestUser.getProcessInstance(processInstanceId);
            const taskFromRb = await taskServiceTestUser.getTaskById(taskId);
            const taskFromQuery = await queryServiceTestUser.getTaskById(taskId);

            expect(taskFromRb.formKey).toBe('taskForm');
            expect(taskFromQuery?.formKey).toBe('taskForm');
            expect(taskFromRb.processDefinitionId).toBe(processFromQuery.processDefinitionId);
            expect(taskFromQuery?.processDefinitionId).toBe(processFromQuery.processDefinitionId);
        });
    });

    activiti('should save a task with variables', async ({
        runtimeBundleServiceTestUser,
        taskServiceTestUser,
        queryServiceTestUser,
    }) => {
        let taskId: string;

        await activiti.step(
            'When the user starts PROCESS_INSTANCE_WITH_VARIABLES, claims the task, and saves variable status',
            async () => {
                const { task } = await startCatalogProcessWithFirstTask(
                    runtimeBundleServiceTestUser,
                    taskServiceTestUser,
                    'PROCESS_INSTANCE_WITH_VARIABLES'
                );
                taskId = task.id;
                await taskServiceTestUser.claimTask(taskId);
                await taskServiceTestUser.saveTask(taskId, { status: 'approved' });
            }
        );

        await activiti.step('Then task variable status has value approved', async () => {
            await expectTaskVariableValue(queryServiceTestUser, taskId, 'status', 'approved');
        });
    });

    activiti('should complete a saved task and propagate variables to the process', async ({
        runtimeBundleServiceTestUser,
        taskServiceTestUser,
        queryServiceTestUser,
    }) => {
        let processInstanceId: string;
        let taskId: string;

        await activiti.step('When the user saves and completes the task', async () => {
            const { processInstance, task } = await startCatalogProcessWithFirstTask(
                runtimeBundleServiceTestUser,
                taskServiceTestUser,
                'PROCESS_INSTANCE_WITH_VARIABLES'
            );
            processInstanceId = processInstance.id;
            taskId = task.id;
            await taskServiceTestUser.claimTask(taskId);
            await taskServiceTestUser.saveTask(taskId, { status: 'approved' });
            await taskServiceTestUser.completeTask(taskId);
        });

        await activiti.step('Then the process is completed with variable status approved', async () => {
            await expectPoll(async () => {
                const instance = await getQueryProcessInstanceWhenSynced(
                    queryServiceTestUser,
                    processInstanceId
                );
                return instance?.status;
            }, 'querySync').toBe(ProcessInstanceStatus.COMPLETED);
            await expectProcessVariableValue(queryServiceTestUser, processInstanceId, 'status', 'approved');
        });
    });

    activiti('should complete a saved task with outcome variables', async ({
        runtimeBundleServiceTestUser,
        taskServiceTestUser,
        queryServiceTestUser,
    }) => {
        let processInstanceId: string;
        let taskId: string;

        await activiti.step('When the user saves comments and completes with outcome', async () => {
            const { processInstance, task } = await startCatalogProcessWithFirstTask(
                runtimeBundleServiceTestUser,
                taskServiceTestUser,
                'PROCESS_INSTANCE_WITH_VARIABLES'
            );
            processInstanceId = processInstance.id;
            taskId = task.id;
            await taskServiceTestUser.claimTask(taskId);
            await taskServiceTestUser.saveTask(taskId, { comments: 'lgtm' });
            await taskServiceTestUser.completeTaskWithVariables(taskId, { outcome: 'approved' });
        });

        await activiti.step('Then query process variables comments and outcome are set', async () => {
            await expectPoll(async () => {
                const instance = await getQueryProcessInstanceWhenSynced(
                    queryServiceTestUser,
                    processInstanceId
                );
                return instance?.status;
            }, 'querySync').toBe(ProcessInstanceStatus.COMPLETED);
            await expectProcessVariableValue(queryServiceTestUser, processInstanceId, 'comments', 'lgtm');
            await expectProcessVariableValue(queryServiceTestUser, processInstanceId, 'outcome', 'approved');
        });
    });

    activiti('should update task fields in runtime and query', async ({
        runtimeBundleServiceTestUser,
        taskServiceTestUser,
        queryServiceTestUser,
    }) => {
        let taskId: string;
        const tomorrow = new Date(Date.now() + 86_400_000).toISOString();

        await activiti.step(
            'When the user starts PROCESS_INSTANCE_WITH_SINGLE_TASK_ASSIGNED and updates task fields',
            async () => {
                const { task } = await startCatalogProcessWithFirstTask(
                    runtimeBundleServiceTestUser,
                    taskServiceTestUser,
                    'PROCESS_INSTANCE_WITH_SINGLE_TASK_ASSIGNED'
                );
                taskId = task.id;
                await taskServiceTestUser.updateTask(taskId, {
                    name: 'new-task-name',
                    priority: 3,
                    formKey: 'new-task-form-key',
                    dueDate: tomorrow,
                });
            }
        );

        await activiti.step('Then the task has the updated fields', async () => {
            await expectPoll(async () => {
                const queryTask = await queryServiceTestUser.getTaskById(taskId);
                return (
                    queryTask?.name === 'new-task-name' &&
                    queryTask?.priority === 3 &&
                    queryTask?.formKey === 'new-task-form-key'
                );
            }, 'querySync').toBe(true);

            const rbTask = await taskServiceTestUser.getTaskById(taskId);
            expect(rbTask.name).toBe('new-task-name');
            expect(rbTask.priority).toBe(3);
            expect(rbTask.formKey).toBe('new-task-form-key');
            expect(rbTask.dueDate).toBeTruthy();
        });
    });

    activiti('should let admin update task fields', async ({
        runtimeBundleServiceTestUser,
        taskServiceTestUser,
        taskAdminServiceTestAdmin,
        queryServiceTestUser,
    }) => {
        let taskId: string;
        const tomorrow = new Date(Date.now() + 86_400_000).toISOString();

        await activiti.step('When testadmin updates task fields on a running process task', async () => {
            const { task } = await startCatalogProcessWithFirstTask(
                runtimeBundleServiceTestUser,
                taskServiceTestUser,
                'PROCESS_INSTANCE_WITH_SINGLE_TASK_ASSIGNED'
            );
            taskId = task.id;
            await taskAdminServiceTestAdmin.updateTask(taskId, {
                name: 'new-task-name',
                priority: 3,
                formKey: 'new-task-form-key',
                dueDate: tomorrow,
            });
        });

        await activiti.step('Then the task is updated for testuser in runtime and query', async () => {
            const rbTask = await taskServiceTestUser.getTaskById(taskId);
            const queryTask = await queryServiceTestUser.getTaskById(taskId);
            expect(rbTask.name).toBe('new-task-name');
            expect(queryTask?.name).toBe('new-task-name');
            expect(rbTask.formKey).toBe('new-task-form-key');
        });
    });

    activiti('should set completion fields when task is completed', async ({
        runtimeBundleServiceTestUser,
        taskServiceTestUser,
        queryServiceTestUser,
    }) => {
        let taskId: string;

        await activiti.step('When the user completes PROCESS_INSTANCE_WITH_SINGLE_TASK_ASSIGNED task', async () => {
            const { task } = await startCatalogProcessWithFirstTask(
                runtimeBundleServiceTestUser,
                taskServiceTestUser,
                'PROCESS_INSTANCE_WITH_SINGLE_TASK_ASSIGNED'
            );
            taskId = task.id;
            await taskServiceTestUser.completeTask(taskId);
        });

        await activiti.step('Then the task has completion fields set in query', async () => {
            await expectPoll(async () => {
                const queryTask = await queryServiceTestUser.getTaskById(taskId);
                return (
                    queryTask?.status === TaskStatus.COMPLETED &&
                    Boolean(queryTask.completedDate ?? queryTask.endDate)
                );
            }, 'querySync').toBe(true);
        });
    });

    activiti('should return only root tasks when querying root tasks for TWO_TASK_PROCESS', async ({
        runtimeBundleServiceTestUser,
        queryServiceTestUser,
    }) => {
        let processInstanceId: string;

        await activiti.step('When the user starts TWO_TASK_PROCESS', async () => {
            const processInstance = await startCatalogProcess(
                runtimeBundleServiceTestUser,
                'TWO_TASK_PROCESS'
            );
            processInstanceId = processInstance.id;
        });

        await activiti.step('Then query returns only root tasks for the process instance', async () => {
            await expectPoll(async () => {
                const rootTasks = await queryServiceTestUser.getRootTasksByProcessInstance(processInstanceId);
                return rootTasks.length > 0 && rootTasks.every((task) => !task.parentTaskId);
            }, 'querySync').toBe(true);
        });
    });

    activiti('should keep candidate groups on completed group-candidate task in query', async ({
        runtimeBundleServiceTestUser,
        taskServiceTestUser,
        queryServiceTestUser,
    }) => {
        let taskId: string;
        let processInstanceId: string;

        await activiti.step(
            'When the user completes PROCESS_INSTANCE_WITH_SINGLE_TASK_AND_GROUP_CANDIDATES',
            async () => {
                const { processInstance, task } = await startCatalogProcessWithFirstTask(
                    runtimeBundleServiceTestUser,
                    taskServiceTestUser,
                    'PROCESS_INSTANCE_WITH_SINGLE_TASK_AND_GROUP_CANDIDATES'
                );
                processInstanceId = processInstance.id;
                taskId = task.id;
                await expectTaskStatusInRbAndQuery(
                    taskServiceTestUser,
                    queryServiceTestUser,
                    taskId,
                    TaskStatus.CREATED
                );

                await expectPoll(async () => {
                    const groups = await queryServiceTestUser.getCandidateGroups(taskId);
                    return groups.includes('hr') && groups.includes('testgroup');
                }, 'querySync').toBe(true);

                await taskServiceTestUser.claimTask(taskId);
                await taskServiceTestUser.completeTask(taskId);
                await expectProcessAndTaskCompleted(
                    runtimeBundleServiceTestUser,
                    queryServiceTestUser,
                    processInstanceId
                );
            }
        );

        await activiti.step('Then candidate groups hr and testgroup remain in query', async () => {
            await expectPoll(async () => {
                const groups = await queryServiceTestUser.getCandidateGroups(taskId);
                return groups.includes('hr') && groups.includes('testgroup');
            }, 'querySync').toBe(true);
            await expectTaskStatusInRbAndQuery(
                taskServiceTestUser,
                queryServiceTestUser,
                taskId,
                TaskStatus.COMPLETED
            );
        });
    });

    activiti('should let assignee reassign task to a candidate user', async ({
        runtimeBundleServiceTestUser,
        taskServiceTestUser,
        taskServiceHrUser,
        queryServiceTestUser,
    }) => {
        let taskId: string;

        await activiti.step(
            'When the user claims PROCESS_INSTANCE_WITH_SINGLE_TASK_AND_USER_CANDIDATES and assigns to hruser',
            async () => {
                const { task } = await startCatalogProcessWithFirstTask(
                    runtimeBundleServiceTestUser,
                    taskServiceTestUser,
                    'PROCESS_INSTANCE_WITH_SINGLE_TASK_AND_USER_CANDIDATES'
                );
                taskId = task.id;

                await expectPoll(async () => {
                    const candidates = await queryServiceTestUser.getCandidateUsers(taskId);
                    return candidates.includes('hruser');
                }, 'querySync').toBe(true);

                await taskServiceTestUser.claimTask(taskId);
                await taskServiceTestUser.assignTask(taskId, 'hruser');
            }
        );

        await activiti.step('Then hruser is the assignee', async () => {
            const hrTask = await taskServiceHrUser.getTaskById(taskId);
            expect(hrTask.assignee).toBe('hruser');
        });
    });

    activiti('should not assign task to a user who is not a candidate', async ({
        runtimeBundleServiceTestUser,
        taskServiceTestUser,
        queryServiceTestUser,
    }) => {
        let taskId: string;

        await activiti.step(
            'When the user starts PROCESS_INSTANCE_WITH_SINGLE_TASK_ASSIGNED',
            async () => {
                const { task } = await startCatalogProcessWithFirstTask(
                    runtimeBundleServiceTestUser,
                    taskServiceTestUser,
                    'PROCESS_INSTANCE_WITH_SINGLE_TASK_ASSIGNED'
                );
                taskId = task.id;
            }
        );

        await activiti.step('And hruser is not a candidate in query', async () => {
            await expectPoll(async () => {
                const candidates = await queryServiceTestUser.getCandidateUsers(taskId);
                return !candidates.includes('hruser');
            }, 'querySync').toBe(true);
        });

        await activiti.step('Then the user cannot assign the task to hruser', async () => {
            const response = await taskServiceTestUser.assignTask(taskId, 'hruser');
            expectClientError(response);
        });
    });

    activiti('should keep separate task variable copies across TWO_TASK_PROCESS tasks', async ({
        runtimeBundleServiceTestUser,
        taskServiceTestUser,
        queryServiceTestUser,
        queryServiceHrUser,
    }) => {
        let firstTaskId: string;

        await activiti.step(
            'When the user starts TWO_TASK_PROCESS with variables start1 and start2',
            async () => {
                const processInstance = await startCatalogProcess(
                    runtimeBundleServiceTestUser,
                    'TWO_TASK_PROCESS',
                    { variables: { start1: 'start1', start2: 'start2' } }
                );
                const firstTask = await getFirstProcessTask(taskServiceTestUser, processInstance.id);
                firstTaskId = firstTask.id;
            }
        );

        await activiti.step('Then the first task has variables start1 and start2', async () => {
            await expectTaskVariable(queryServiceTestUser, firstTaskId, 'start1');
            await expectTaskVariable(queryServiceTestUser, firstTaskId, 'start2');
            await expectTaskVariableValue(queryServiceTestUser, firstTaskId, 'start1', 'start1');
        });

        await activiti.step('When testuser claims, modifies start1, and completes the first task', async () => {
            await taskServiceTestUser.claimTask(firstTaskId);
            await taskServiceTestUser.updateTaskVariable(firstTaskId, 'start1', 'start1modified');
            await expectTaskVariableValue(queryServiceTestUser, firstTaskId, 'start1', 'start1modified');
            await taskServiceTestUser.completeTask(firstTaskId);
        });

        await activiti.step('Then hruser sees start1 with original value on the next task', async () => {
            await expectPoll(async () => {
                const hrTasks = await queryServiceHrUser.getAllTasks();
                const nextTask = hrTasks.find(
                    (task) =>
                        task.processDefinitionKey ===
                            ProcessDefinitionRegistry.getProcessDefinitionKey('TWO_TASK_PROCESS') &&
                        task.id !== firstTaskId &&
                        task.status === TaskStatus.CREATED
                );
                if (!nextTask?.id) {
                    return false;
                }
                const variables = await queryServiceHrUser.getTaskVariables(nextTask.id);
                const start1 = variables.find((variable) => variable.name === 'start1');
                return String(start1?.value) === 'start1';
            }, 'querySync').toBe(true);
        });
    });
});
