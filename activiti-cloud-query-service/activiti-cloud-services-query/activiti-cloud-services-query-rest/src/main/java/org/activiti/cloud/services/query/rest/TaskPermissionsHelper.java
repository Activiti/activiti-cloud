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
package org.activiti.cloud.services.query.rest;

import java.util.ArrayList;
import java.util.List;
import org.activiti.api.runtime.shared.security.SecurityManager;
import org.activiti.cloud.api.task.model.QueryCloudTask.TaskPermissions;
import org.activiti.cloud.services.query.model.QTaskEntity;
import org.activiti.cloud.services.query.model.TaskEntity;

public class TaskPermissionsHelper {

    private final SecurityManager securityManager;

    private final TaskControllerHelper taskControllerHelper;

    public TaskPermissionsHelper(SecurityManager securityManager, TaskControllerHelper taskControllerHelper) {
        this.securityManager = securityManager;
        this.taskControllerHelper = taskControllerHelper;
    }

    public void setCurrentUserTaskPermissions(TaskEntity task) {
        String userId = securityManager.getAuthenticatedUserId();
        if (userId != null) {
            List<String> userGroups = securityManager.getAuthenticatedUserGroups();
            List<TaskPermissions> permissions = new ArrayList<>();
            if (!canUserViewTask(task)) return;

            permissions.add(TaskPermissions.VIEW);

            if (canUserClaimTask(task, userId, userGroups)) {
                permissions.add(TaskPermissions.CLAIM);
            }

            if (canUserReleaseTask(task, userId)) {
                permissions.add(TaskPermissions.RELEASE);
            }

            if (canUserUpdateTask(task, userId)) {
                permissions.add(TaskPermissions.UPDATE);
            }

            task.setPermissions(permissions);
        }
    }

    private boolean canUserViewTask(TaskEntity task) {
        return taskControllerHelper.canUserViewTask(QTaskEntity.taskEntity.id.eq(task.getId()));
    }

    private boolean canUserClaimTask(TaskEntity task, String userId, List<String> userGroups) {
        return !isTaskAssigned(task) && isUserCandidate(task, userId, userGroups);
    }

    private boolean canUserReleaseTask(TaskEntity task, String userId) {
        boolean taskHasCandidatesUser = task.getCandidateUsers() != null && !task.getCandidateUsers().isEmpty();
        boolean taskHasCandidateGroups = task.getCandidateGroups() != null && !task.getCandidateGroups().isEmpty();
        return isUserAssignee(task, userId) && (taskHasCandidatesUser || taskHasCandidateGroups);
    }

    private boolean canUserUpdateTask(TaskEntity task, String userId) {
        boolean isUserOwner = task.getOwner() != null && task.getOwner().equals(userId);
        return isUserAssignee(task, userId) || isUserOwner;
    }

    private boolean isUserCandidate(TaskEntity task, String userId, List<String> userGroups) {
        boolean isCandidateUser = task.getCandidateUsers() != null && task.getCandidateUsers().contains(userId);
        boolean isCandidateGroup =
            task.getCandidateGroups() != null && task.getCandidateGroups().stream().anyMatch(userGroups::contains);
        return isCandidateUser || isCandidateGroup;
    }

    private boolean isUserAssignee(TaskEntity task, String userId) {
        return isTaskAssigned(task) && task.getAssignee().equals(userId);
    }

    private boolean isTaskAssigned(TaskEntity task) {
        return task.getAssignee() != null;
    }
}
