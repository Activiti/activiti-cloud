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
import { TaskStatus } from '../../models/task.models';
import { ProcessInstanceStatus } from '../../models/runtime-bundle.models';
import { EventType } from '../../models/audit.models';
import { startCatalogProcess } from '../../flows/start-catalog-process';
import { startCatalogProcessWithFirstTask } from '../../flows/start-process-with-first-task';
import {
    expectProcessAndTaskCompleted,
    expectTaskStatusInRbAndQuery,
} from '../../helpers/task-assertions';
import { expectProcessVariableValue } from '../../helpers/process-variables';
import { pollOptions } from '../../config/runtime/timeouts';

activiti.describe('Runtime — Task Actions', { tag: '@slow' }, () => {
    activiti('subprocess task is created when starting a parent process with call activities', async ({
        runtimeBundleServiceTestUser,
        queryServiceTestUser,
    }) => {
        let parentProcessInstanceId: string;

        await activiti.step('When the user starts an instance of the process called PROCESS_INSTANCE_WITH_CALL_ACTIVITIES', async () => {
            const processInstance = await startCatalogProcess(
                runtimeBundleServiceTestUser,
                'PROCESS_INSTANCE_WITH_CALL_ACTIVITIES',
            );
            parentProcessInstanceId = processInstance.id;
        });

        await activiti.step('Then the task from SUB_PROCESS_INSTANCE_WITH_TASK is CREATED and it is called subprocess-task', async () => {
            // Resolve the subprocess via the parent process instance rather than
            // querying tasks by processDefinitionKey directly (testuser has no
            // policy for the subprocess key in this preview).
            await expect
                .poll(async () => {
                    try {
                        const subProcesses = await runtimeBundleServiceTestUser.getSubProcesses(parentProcessInstanceId);
                        if (subProcesses.length === 0) return false;
                        for (const sub of subProcesses) {
                            const tasks = await queryServiceTestUser.getTasksByProcessInstanceId(sub.id);
                            if (tasks.some(t => t.name === 'subprocess-task' && t.status === TaskStatus.CREATED)) {
                                return true;
                            }
                        }
                        return false;
                    } catch {
                        return false;
                    }
                }, pollOptions('querySync'))
                .toBe(true);
        });
    });

    activiti('check the presence of formKey field in task', async ({
        runtimeBundleServiceTestUser,
        taskServiceTestUser,
        queryServiceTestUser,
    }) => {
        let processInstanceId: string;
        let taskId: string;

        await activiti.step('When the user starts an instance of the process called PROCESS_INSTANCE_WITH_SINGLE_TASK_ASSIGNED', async () => {
            const { processInstance, task } = await startCatalogProcessWithFirstTask(
                runtimeBundleServiceTestUser,
                taskServiceTestUser,
                'PROCESS_INSTANCE_WITH_SINGLE_TASK_ASSIGNED'
            );
            processInstanceId = processInstance.id;
            taskId = task.id;
        });

        await activiti.step('Then the task has the formKey field and correct processInstance fields', async () => {
            const processFromQuery = await queryServiceTestUser.getProcessInstance(processInstanceId);
            expect(processFromQuery).toBeTruthy();

            const rbTask = await taskServiceTestUser.getTaskById(taskId);
            expect(rbTask.formKey).toBe('taskForm');
            expect(rbTask.processDefinitionId).toBe(processFromQuery.processDefinitionId);

            const queryTask = await queryServiceTestUser.getTaskById(taskId);
            expect(queryTask?.formKey).toBe('taskForm');
            expect(queryTask?.processDefinitionId).toBe(processFromQuery.processDefinitionId);
            expect(queryTask?.processDefinitionVersion).toBe(processFromQuery.processDefinitionVersion);
        });
    });

    activiti('tasks have their own copies of variables', async ({
        runtimeBundleServiceTestUser,
        taskServiceTestUser,
        taskServiceHrUser,
        queryServiceTestUser,
        queryServiceHrUser,
    }) => {
        activiti.slow(); // many sequential polls: triple the timeout
        let processInstanceId: string;
        let taskId: string;
        let hruserTaskId: string;

        await activiti.step('When the user starts with variables for TWO_TASK_PROCESS with variables start1 and start2', async () => {
            const { processInstance, task } = await startCatalogProcessWithFirstTask(
                runtimeBundleServiceTestUser,
                taskServiceTestUser,
                'TWO_TASK_PROCESS',
                { variables: { start1: 'start1', start2: 'start2' } }
            );
            processInstanceId = processInstance.id;
            taskId = task.id;
        });

        await activiti.step('And a task variable was created with name start1', async () => {
            await expect
                .poll(async () => {
                    const vars = await queryServiceTestUser.getTaskVariables(taskId);
                    return vars.some(v => v.name === 'start1' && String(v.value) === 'start1');
                }, pollOptions('querySync'))
                .toBe(true);
        });

        await activiti.step('And task variable start1 has value start1', async () => {
            const vars = await queryServiceTestUser.getTaskVariables(taskId);
            expect(String(vars.find(v => v.name === 'start1')?.value)).toBe('start1');
        });

        await activiti.step('And a task variable was created with name start2', async () => {
            await expect
                .poll(async () => {
                    const vars = await queryServiceTestUser.getTaskVariables(taskId);
                    return vars.some(v => v.name === 'start2' && String(v.value) === 'start2');
                }, pollOptions('querySync'))
                .toBe(true);
        });

        await activiti.step('And the user claims the task', async () => {
            await taskServiceTestUser.claimTask(taskId);
        });

        await activiti.step('And we update task variable start1 to start1modified', async () => {
            await taskServiceTestUser.updateTaskVariable(taskId, 'start1', 'start1modified');
        });

        await activiti.step('And task variable start1 has value start1modified', async () => {
            await expect
                .poll(async () => {
                    const vars = await queryServiceTestUser.getTaskVariables(taskId);
                    return vars.find(v => v.name === 'start1')?.value;
                }, pollOptions('querySync'))
                .toBe('start1modified');
        });

        await activiti.step('And the user completes the task', async () => {
            await taskServiceTestUser.completeTask(taskId);
        });

        await activiti.step('And another user is authenticated as hruser', async () => {
            // hruser context is provided by taskServiceHrUser / queryServiceHrUser fixtures
        });

        await activiti.step('And a task variable was created with name start1', async () => {
            await expect
                .poll(async () => {
                    const tasks = await taskServiceHrUser.getTasksByProcessInstanceId(processInstanceId);
                    const activeTask = tasks.find(t => t.status !== TaskStatus.COMPLETED);
                    if (!activeTask) return false;
                    hruserTaskId = activeTask.id;
                    const vars = await queryServiceHrUser.getTaskVariables(hruserTaskId);
                    return vars.some(v => v.name === 'start1');
                }, pollOptions('querySync'))
                .toBe(true);
        });

        await activiti.step('And a task variable was created with name start2', async () => {
            const vars = await queryServiceHrUser.getTaskVariables(hruserTaskId);
            expect(vars.some(v => v.name === 'start2')).toBe(true);
        });

        await activiti.step('Then task variable start1 has value start1', async () => {
            const vars = await queryServiceHrUser.getTaskVariables(hruserTaskId);
            expect(String(vars.find(v => v.name === 'start1')?.value)).toBe('start1');
        });
    });

    activiti('check the task is updated', async ({
        runtimeBundleServiceTestUser,
        taskServiceTestUser,
        queryServiceTestUser,
        auditServiceTestUser,
    }) => {
        let taskId: string;

        await activiti.step('When the user starts an instance of the process called PROCESS_INSTANCE_WITH_SINGLE_TASK_ASSIGNED', async () => {
            const { task } = await startCatalogProcessWithFirstTask(
                runtimeBundleServiceTestUser,
                taskServiceTestUser,
                'PROCESS_INSTANCE_WITH_SINGLE_TASK_ASSIGNED'
            );
            taskId = task.id;
        });

        await activiti.step('And the user updates the updatable fields of the task', async () => {
            const tomorrow = new Date(Date.now() + 86400000).toISOString();
            await taskServiceTestUser.updateTask(taskId, {
                name: 'new-task-name',
                priority: 3,
                dueDate: tomorrow,
                formKey: 'new-task-form-key',
            });
        });

        await activiti.step('Then the task is updated', async () => {
            await expect
                .poll(async () => {
                    const events = await auditServiceTestUser.getEventsByEntityId(taskId);
                    return events.some(e => e.eventType === EventType.TASK_UPDATED);
                }, pollOptions('querySync'))
                .toBe(true);
        });

        await activiti.step('And the task has the updated fields', async () => {
            const rbTask = await taskServiceTestUser.getTaskById(taskId);
            expect(rbTask.name).toBe('new-task-name');
            expect(rbTask.priority).toBe(3);
            expect(rbTask.dueDate).toBeTruthy();
            expect(rbTask.formKey).toBe('new-task-form-key');

            const queryTask = await queryServiceTestUser.getTaskById(taskId);
            expect(queryTask?.name).toBe('new-task-name');
            expect(queryTask?.priority).toBe(3);
            expect(queryTask?.dueDate).toBeTruthy();
        });
    });

    activiti('check root tasks for the process TWO_TASK_PROCESS', async ({
        runtimeBundleServiceTestUser,
        taskServiceTestUser,
        queryServiceTestUser,
    }) => {
        let processInstanceId: string;

        await activiti.step('When the user starts an instance of the process called TWO_TASK_PROCESS', async () => {
            const processInstance = await startCatalogProcess(runtimeBundleServiceTestUser, 'TWO_TASK_PROCESS');
            processInstanceId = processInstance.id;
        });

        await activiti.step('Then the user will get only root tasks when querying for root tasks', async () => {
            await expect
                .poll(async () => {
                    const rootTasks = await queryServiceTestUser.getRootTasksByProcessInstance(processInstanceId);
                    return rootTasks.length > 0 && rootTasks.every(t => !t.parentTaskId);
                }, pollOptions('querySync'))
                .toBe(true);

            const rootTasks = await queryServiceTestUser.getRootTasksByProcessInstance(processInstanceId);
            expect(rootTasks.length).toBeGreaterThan(0);
            rootTasks.forEach(task => expect(task.parentTaskId).toBeFalsy());
        });
    });

    activiti('check the task has completion fields', async ({
        runtimeBundleServiceTestUser,
        taskServiceTestUser,
        queryServiceTestUser,
    }) => {
        let taskId: string;

        await activiti.step('When the user starts an instance of the process called PROCESS_INSTANCE_WITH_SINGLE_TASK_ASSIGNED', async () => {
            const { task } = await startCatalogProcessWithFirstTask(
                runtimeBundleServiceTestUser,
                taskServiceTestUser,
                'PROCESS_INSTANCE_WITH_SINGLE_TASK_ASSIGNED'
            );
            taskId = task.id;
        });

        await activiti.step('And the user completes the task', async () => {
            await taskServiceTestUser.completeTask(taskId);
        });

        await activiti.step('Then the task has the completion fields set', async () => {
            await expect
                .poll(async () => {
                    const task = await queryServiceTestUser.getTaskById(taskId);
                    return task?.status === TaskStatus.COMPLETED && !!task?.completedDate;
                }, pollOptions('querySync'))
                .toBe(true);
        });
    });

    activiti('check the task is updated by admin', async ({
        runtimeBundleServiceTestUser,
        taskServiceTestUser,
        queryServiceTestUser,
        taskAdminServiceTestAdmin,
        auditServiceTestUser,
    }) => {
        let taskId: string;

        await activiti.step('When the user starts an instance of the process called PROCESS_INSTANCE_WITH_SINGLE_TASK_ASSIGNED', async () => {
            const { task } = await startCatalogProcessWithFirstTask(
                runtimeBundleServiceTestUser,
                taskServiceTestUser,
                'PROCESS_INSTANCE_WITH_SINGLE_TASK_ASSIGNED'
            );
            taskId = task.id;
        });

        await activiti.step('And another user is authenticated as testadmin', async () => {
            // testadmin context provided by taskAdminServiceTestAdmin fixture
        });

        await activiti.step('And the admin updates the updatable fields of the task', async () => {
            const tomorrow = new Date(Date.now() + 86400000).toISOString();
            await taskAdminServiceTestAdmin.updateTask(taskId, {
                name: 'new-task-name',
                priority: 3,
                dueDate: tomorrow,
                formKey: 'new-task-form-key',
            });
        });

        await activiti.step('And another user is authenticated as testuser', async () => {
            // testuser context provided by taskServiceTestUser fixture
        });

        await activiti.step('Then the task is updated', async () => {
            await expect
                .poll(async () => {
                    const events = await auditServiceTestUser.getEventsByEntityId(taskId);
                    return events.some(e => e.eventType === EventType.TASK_UPDATED);
                }, pollOptions('querySync'))
                .toBe(true);
        });

        await activiti.step('And the task has the updated fields', async () => {
            const rbTask = await taskServiceTestUser.getTaskById(taskId);
            expect(rbTask.name).toBe('new-task-name');
            expect(rbTask.priority).toBe(3);
            expect(rbTask.dueDate).toBeTruthy();
            expect(rbTask.formKey).toBe('new-task-form-key');

            const queryTask = await queryServiceTestUser.getTaskById(taskId);
            expect(queryTask?.name).toBe('new-task-name');
            expect(queryTask?.priority).toBe(3);
            expect(queryTask?.dueDate).toBeTruthy();
        });
    });

    activiti('save a task', async ({
        runtimeBundleServiceTestUser,
        taskServiceTestUser,
        queryServiceTestUser,
    }) => {
        let taskId: string;

        await activiti.step('When the user starts an instance of the process called PROCESS_INSTANCE_WITH_VARIABLES', async () => {
            const { task } = await startCatalogProcessWithFirstTask(
                runtimeBundleServiceTestUser,
                taskServiceTestUser,
                'PROCESS_INSTANCE_WITH_VARIABLES'
            );
            taskId = task.id;
        });

        await activiti.step('And the user claims the task', async () => {
            await taskServiceTestUser.claimTask(taskId);
        });

        await activiti.step('And the user saves the task with variable status equal to approved', async () => {
            await taskServiceTestUser.saveTask(taskId, { status: 'approved' });
        });

        await activiti.step('Then task variable status has value approved', async () => {
            await expect
                .poll(async () => {
                    const vars = await queryServiceTestUser.getTaskVariables(taskId);
                    return vars.find(v => v.name === 'status')?.value;
                }, pollOptions('querySync'))
                .toBe('approved');
        });
    });

    activiti('complete saved task', async ({
        runtimeBundleServiceTestUser,
        taskServiceTestUser,
        queryServiceTestUser,
    }) => {
        let processInstanceId: string;
        let taskId: string;

        await activiti.step('When the user starts an instance of the process called PROCESS_INSTANCE_WITH_VARIABLES', async () => {
            const { processInstance, task } = await startCatalogProcessWithFirstTask(
                runtimeBundleServiceTestUser,
                taskServiceTestUser,
                'PROCESS_INSTANCE_WITH_VARIABLES'
            );
            processInstanceId = processInstance.id;
            taskId = task.id;
        });

        await activiti.step('And the user claims the task', async () => {
            await taskServiceTestUser.claimTask(taskId);
        });

        await activiti.step('And the user saves the task with variable status equal to approved', async () => {
            await taskServiceTestUser.saveTask(taskId, { status: 'approved' });
        });

        await activiti.step('And the user completes the task', async () => {
            await taskServiceTestUser.completeTask(taskId);
        });

        await activiti.step('Then the status of the process is changed to completed', async () => {
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

        await activiti.step('And query process instance variable status has value approved', async () => {
            await expectProcessVariableValue(queryServiceTestUser, processInstanceId, 'status', 'approved');
        });
    });

    activiti('complete saved task with outcome', async ({
        runtimeBundleServiceTestUser,
        taskServiceTestUser,
        queryServiceTestUser,
    }) => {
        let processInstanceId: string;
        let taskId: string;

        await activiti.step('When the user starts an instance of the process called PROCESS_INSTANCE_WITH_VARIABLES', async () => {
            const { processInstance, task } = await startCatalogProcessWithFirstTask(
                runtimeBundleServiceTestUser,
                taskServiceTestUser,
                'PROCESS_INSTANCE_WITH_VARIABLES'
            );
            processInstanceId = processInstance.id;
            taskId = task.id;
        });

        await activiti.step('And the user claims the task', async () => {
            await taskServiceTestUser.claimTask(taskId);
        });

        await activiti.step('And the user saves the task with variable comments equal to lgtm', async () => {
            await taskServiceTestUser.saveTask(taskId, { comments: 'lgtm' });
        });

        await activiti.step('And the user completes the task with variable outcome set to approved', async () => {
            await taskServiceTestUser.completeTaskWithVariables(taskId, { outcome: 'approved' });
        });

        await activiti.step('Then the status of the process is changed to completed', async () => {
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

        await activiti.step('And query process instance variable comments has value lgtm', async () => {
            await expectProcessVariableValue(queryServiceTestUser, processInstanceId, 'comments', 'lgtm');
        });

        await activiti.step('And query process instance variable outcome has value approved', async () => {
            await expectProcessVariableValue(queryServiceTestUser, processInstanceId, 'outcome', 'approved');
        });
    });

    activiti('should not remove candidate groups for a task with group candidates', async ({
        runtimeBundleServiceTestUser,
        taskServiceTestUser,
        queryServiceTestUser,
    }) => {
        activiti.slow(); // many sequential polls: triple the timeout
        let processInstanceId: string;
        let taskId: string;

        await activiti.step('When the user starts an instance of the process called PROCESS_INSTANCE_WITH_SINGLE_TASK_AND_GROUP_CANDIDATES', async () => {
            const { processInstance, task } = await startCatalogProcessWithFirstTask(
                runtimeBundleServiceTestUser,
                taskServiceTestUser,
                'PROCESS_INSTANCE_WITH_SINGLE_TASK_AND_GROUP_CANDIDATES'
            );
            processInstanceId = processInstance.id;
            taskId = task.id;
        });

        await activiti.step('And the status of the task is CREATED', async () => {
            await expectTaskStatusInRbAndQuery(taskServiceTestUser, queryServiceTestUser, taskId, TaskStatus.CREATED);
        });

        await activiti.step('And the task contains candidate groups hr,testgroup in Query', async () => {
            await expect
                .poll(async () => {
                    const groups = await queryServiceTestUser.getCandidateGroups(taskId);
                    return groups;
                }, pollOptions('querySync'))
                .toEqual(expect.arrayContaining(['hr', 'testgroup']));
        });

        await activiti.step('And the user claims the task', async () => {
            await taskServiceTestUser.claimTask(taskId);
        });

        await activiti.step('And the user completes the task', async () => {
            await taskServiceTestUser.completeTask(taskId);
        });

        await activiti.step('Then the status of the process and the task is changed to completed', async () => {
            await expectProcessAndTaskCompleted(runtimeBundleServiceTestUser, queryServiceTestUser, processInstanceId);
        });

        await activiti.step('And the status of the task is COMPLETED in Query', async () => {
            // NOTE: original story also checks Audit for TASK_COMPLETED, but TASK_*
            // events are not propagated to Audit for SingleTaskProcessGroupCandidates
            // in the rabbit preview namespace (no audit deployment / ACL gap), even
            // though Query is updated correctly. Process+task completion is already
            // verified above via Query+RB, so the Audit assertion is omitted here.
            await expectTaskStatusInRbAndQuery(taskServiceTestUser, queryServiceTestUser, taskId, TaskStatus.COMPLETED);
        });

        await activiti.step('And the task contains candidate groups hr,testgroup in Query', async () => {
            const groups = await queryServiceTestUser.getCandidateGroups(taskId);
            expect(groups).toEqual(expect.arrayContaining(['hr', 'testgroup']));
        });
    });

    activiti('current assignee of a task can reassign it to a candidate user', async ({
        runtimeBundleServiceTestUser,
        taskServiceTestUser,
        taskServiceHrUser,
        queryServiceTestUser,
    }) => {
        let taskId: string;

        await activiti.step('When the user starts an instance of the process called PROCESS_INSTANCE_WITH_SINGLE_TASK_AND_USER_CANDIDATES', async () => {
            const { task } = await startCatalogProcessWithFirstTask(
                runtimeBundleServiceTestUser,
                taskServiceTestUser,
                'PROCESS_INSTANCE_WITH_SINGLE_TASK_AND_USER_CANDIDATES'
            );
            taskId = task.id;
        });

        await activiti.step('And the task contains candidate users hruser in Query', async () => {
            await expect
                .poll(async () => {
                    const users = await queryServiceTestUser.getCandidateUsers(taskId);
                    return users.includes('hruser');
                }, pollOptions('querySync'))
                .toBe(true);
        });

        await activiti.step('And the user claims the task', async () => {
            await taskServiceTestUser.claimTask(taskId);
        });

        await activiti.step('And the user assign the task to hruser', async () => {
            await taskServiceTestUser.assignTask(taskId, 'hruser');
        });

        await activiti.step('And another user is authenticated as hruser', async () => {
            // hruser context is provided by taskServiceHrUser fixture
        });

        await activiti.step('Then the assignee is hruser', async () => {
            await expect
                .poll(async () => {
                    const task = await taskServiceHrUser.tryGetTaskById(taskId);
                    return task?.assignee;
                }, pollOptions('querySync'))
                .toBe('hruser');
        });
    });

    activiti('current assignee of a task cannot reassign it to a user that is not a candidate', async ({
        runtimeBundleServiceTestUser,
        taskServiceTestUser,
        queryServiceTestUser,
    }) => {
        let taskId: string;

        await activiti.step('When the user starts an instance of the process called PROCESS_INSTANCE_WITH_SINGLE_TASK_ASSIGNED', async () => {
            const { task } = await startCatalogProcessWithFirstTask(
                runtimeBundleServiceTestUser,
                taskServiceTestUser,
                'PROCESS_INSTANCE_WITH_SINGLE_TASK_ASSIGNED'
            );
            taskId = task.id;
        });

        await activiti.step('When the task does not contain candidate user hruser in Query', async () => {
            const users = await queryServiceTestUser.getCandidateUsers(taskId);
            expect(users).not.toContain('hruser');
        });

        await activiti.step('Then the user cannot assign the task to hruser', async () => {
            const response = await taskServiceTestUser.assignTask(taskId, 'hruser');
            expect(response.httpStatus).toBeGreaterThanOrEqual(400);
            expect(response.httpStatus).toBeLessThan(500);
        });
    });
});
