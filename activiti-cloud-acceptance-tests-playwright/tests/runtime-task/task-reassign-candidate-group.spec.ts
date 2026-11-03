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
import { startCatalogProcessWithFirstTask } from '../../flows/start-process-with-first-task';

activiti.describe('Runtime — Task Reassignment to Candidate Group Member [AAE-52557]', () => {
    for (const candidateGroup of ['hr', 'sales']) {
        activiti(`allows a non-admin candidate to assign an unclaimed task to a member of the ${candidateGroup} candidate group`, async ({
            runtimeBundleServiceTestUser,
            taskServiceTestUser,
            taskAdminServiceTestAdmin,
            identityManagementServiceTestUser,
        }) => {
            let taskId = '';
            let candidateGroupMember = '';

            await activiti.step(`Given an unclaimed task with candidate group ${candidateGroup}`, async () => {
                const { task } = await startCatalogProcessWithFirstTask(
                    runtimeBundleServiceTestUser,
                    taskServiceTestUser,
                    'PROCESS_INSTANCE_WITH_SINGLE_TASK_AND_GROUP_CANDIDATES'
                );
                taskId = task.id;
                const existingGroups = await taskAdminServiceTestAdmin.tasks.getCandidateGroups(taskId);
                if (!existingGroups.includes(candidateGroup)) {
                    await taskAdminServiceTestAdmin.tasks.addCandidateGroups(taskId, [candidateGroup]);
                }
                const candidateGroups = await taskAdminServiceTestAdmin.tasks.getCandidateGroups(taskId);
                expect(candidateGroups).toContain(candidateGroup);
            });

            await activiti.step(`And a member of the ${candidateGroup} candidate group is resolved via identity`, async () => {
                const members = await identityManagementServiceTestUser.searchUsers({ group: [candidateGroup] });
                candidateGroupMember =
                    members.map((user) => user.username).find((username) => username !== 'testuser') ?? '';
                expect(candidateGroupMember).toBeTruthy();
            });

            await activiti.step('When the non-admin candidate assigns the task to the candidate-group member', async () => {
                const assignResponse = await taskServiceTestUser.tasks.assignTask(taskId, candidateGroupMember);
                expect(assignResponse.httpStatus).toBeLessThan(300);
            });

            await activiti.step('Then the task is assigned to the candidate-group member', async () => {
                const assignedTask = await taskAdminServiceTestAdmin.tasks.getTaskById(taskId);
                expect(assignedTask.assignee).toBe(candidateGroupMember);
            });
        });
    }
});
