/*
 * Copyright 2018 Alfresco, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.activiti.cloud.services.core.pageable;

import java.util.List;

import org.activiti.cloud.services.common.security.SpringSecurityAuthenticationWrapper;
import org.activiti.runtime.api.TaskRuntime;
import org.activiti.runtime.api.identity.IdentityLookup;
import org.activiti.runtime.api.model.Task;
import org.activiti.runtime.api.model.VariableInstance;
import org.activiti.runtime.api.model.builders.TaskPayloadBuilder;
import org.activiti.runtime.api.model.payloads.ClaimTaskPayload;
import org.activiti.runtime.api.model.payloads.CompleteTaskPayload;
import org.activiti.runtime.api.model.payloads.CreateTaskPayload;
import org.activiti.runtime.api.model.payloads.DeleteTaskPayload;
import org.activiti.runtime.api.model.payloads.GetTasksPayload;
import org.activiti.runtime.api.model.payloads.ReleaseTaskPayload;
import org.activiti.runtime.api.model.payloads.SetTaskVariablesPayload;
import org.activiti.runtime.api.model.payloads.UpdateTaskPayload;
import org.activiti.runtime.api.query.Page;
import org.activiti.runtime.api.query.Pageable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SecurityAwareTaskService {

    private final SpringSecurityAuthenticationWrapper authenticationWrapper;

    private final TaskRuntime taskRuntime;

    @Autowired
    private IdentityLookup identityLookup;

    @Autowired
    public SecurityAwareTaskService(SpringSecurityAuthenticationWrapper authenticationWrapper,
                                    TaskRuntime taskRuntime) {
        this.authenticationWrapper = authenticationWrapper;
        this.taskRuntime = taskRuntime;
    }

    public Page<Task> getAuthorizedTasks(Pageable pageable) {

        String userId = authenticationWrapper.getAuthenticatedUserId();
        GetTasksPayload getTasksPayload;
        if (userId != null) {
            List<String> groups = null;
            if (identityLookup != null) {
                groups = identityLookup.getGroupsForCandidateUser(userId);
            }
            getTasksPayload = TaskPayloadBuilder.tasks().withAssignee(userId).withGroups(groups).build();
        } else {
            getTasksPayload = TaskPayloadBuilder.tasks().build();
        }
        return taskRuntime.tasks(pageable,
                                 getTasksPayload);
    }

    public Page<Task> getAllTasks(Pageable pageable) {
        return taskRuntime.tasks(pageable);
    }

    public Page<Task> getTasks(String processInstanceId,
                               Pageable pageable) {
        return taskRuntime.tasks(pageable,
                                 TaskPayloadBuilder.tasks().withProcessInstanceId(processInstanceId).build());
    }

    public Task getTaskById(String taskId) {
        return taskRuntime.task(taskId);
    }

    public Task claimTask(ClaimTaskPayload claimTaskPayload) {
        return taskRuntime.claim(claimTaskPayload);
    }

    public Task releaseTask(ReleaseTaskPayload releaseTaskPayload) {
        return taskRuntime
                .release(releaseTaskPayload);
    }

    public Task completeTask(CompleteTaskPayload completeTaskPayload) {
        return taskRuntime.complete(completeTaskPayload);
    }

    public Task createNewTask(CreateTaskPayload createTaskPayload) {
        if (createTaskPayload.getAssignee() == null) {
            createTaskPayload.setAssignee(authenticationWrapper.getAuthenticatedUserId());
        }
        return taskRuntime.create(createTaskPayload);
    }

    public Task createNewSubtask(String parentTaskId,
                                 CreateTaskPayload createTaskPayload) {
        if (createTaskPayload.getAssignee() == null) {
            createTaskPayload.setAssignee(authenticationWrapper.getAuthenticatedUserId());
        }
        if (createTaskPayload.getParentTaskId() == null || createTaskPayload.getParentTaskId().isEmpty()) {
            createTaskPayload.setParentTaskId(parentTaskId);
        }
        return taskRuntime.create(createTaskPayload);
    }

    public Task deleteTask(DeleteTaskPayload deleteTaskPayload) {
        String reason = "Cancelled by " + authenticationWrapper.getAuthenticatedUserId();
        deleteTaskPayload.setReason(reason);
        return taskRuntime.delete(deleteTaskPayload);
    }

    public Task updateTask(UpdateTaskPayload updateTaskPayload) {
        return taskRuntime.update(updateTaskPayload);
    }

    public void setTaskVariables(SetTaskVariablesPayload setTaskVariablesPayload) {
        taskRuntime.setVariables(setTaskVariablesPayload);
    }

    public List<VariableInstance> getVariableInstances(String taskId) {
        return taskRuntime.variables(TaskPayloadBuilder.variables().withTaskId(taskId).build());
    }

    public List<VariableInstance> getVariableInstancesLocal(String taskId) {
        return taskRuntime.variables(TaskPayloadBuilder.variables().withTaskId(taskId).withLocalOnly(true).build());
    }

    public Page<Task> tasks(Pageable pageable,
                            GetTasksPayload getTasksPayload) {
        return taskRuntime.tasks(pageable,
                                 getTasksPayload);
    }
}
