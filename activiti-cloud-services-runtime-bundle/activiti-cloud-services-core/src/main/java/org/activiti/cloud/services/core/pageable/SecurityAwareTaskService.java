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

import org.activiti.cloud.services.api.commands.UpdateTaskCmd;
import org.activiti.cloud.services.common.security.SpringSecurityAuthenticationWrapper;
import org.activiti.runtime.api.TaskRuntime;
import org.activiti.runtime.api.cmd.ClaimTask;
import org.activiti.runtime.api.cmd.CompleteTask;
import org.activiti.runtime.api.cmd.CreateTask;
import org.activiti.runtime.api.cmd.ReleaseTask;
import org.activiti.runtime.api.cmd.SetTaskVariables;
import org.activiti.runtime.api.identity.IdentityLookup;
import org.activiti.runtime.api.model.FluentTask;
import org.activiti.runtime.api.model.VariableInstance;
import org.activiti.runtime.api.query.Page;
import org.activiti.runtime.api.query.Pageable;
import org.activiti.runtime.api.query.TaskFilter;
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

    public Page<FluentTask> getAuthorizedTasks(Pageable pageable) {

        String userId = authenticationWrapper.getAuthenticatedUserId();
        TaskFilter taskFilter;
        if (userId != null) {
            List<String> groups = null;
            if (identityLookup != null) {
                groups = identityLookup.getGroupsForCandidateUser(userId);
            }
            taskFilter = TaskFilter.filteredOnAssigneeOrCandiate(userId,
                                                                 groups);
        } else {
            taskFilter = TaskFilter.unfiltered();
        }
        return taskRuntime.tasks(pageable,
                                 taskFilter);
    }

    public Page<FluentTask> getAllTasks(Pageable pageable) {
        return taskRuntime.tasks(pageable);
    }

    public Page<FluentTask> getTasks(String processInstanceId,
                                     Pageable pageable) {
        return taskRuntime.tasks(pageable,
                                 TaskFilter.filteredOnProcessInstanceId(processInstanceId));
    }

    public FluentTask getTaskById(String taskId) {
        return taskRuntime.task(taskId);
    }

    public FluentTask claimTask(ClaimTask claimTaskCmd) {
        taskRuntime.task(claimTaskCmd.getTaskId())
                .claim(claimTaskCmd.getAssignee());
        return taskRuntime.task(claimTaskCmd.getTaskId());
    }

    public FluentTask releaseTask(ReleaseTask releaseTaskCmd) {
        taskRuntime.task(releaseTaskCmd.getTaskId())
                .release();
        return taskRuntime.task(releaseTaskCmd.getTaskId());
    }

    public void completeTask(CompleteTask completeTaskCmd) {
        if (completeTaskCmd != null) {
            taskRuntime.task(completeTaskCmd.getTaskId())
                    .completeWith()
                    .variables(completeTaskCmd.getOutputVariables())
                    .doIt();
        }
    }

    public FluentTask createNewTask(CreateTask createTaskCmd) {
        return taskRuntime
                .createTaskWith()
                .name(createTaskCmd.getName())
                .description(createTaskCmd.getDescription())
                .dueDate(createTaskCmd.getDueDate())
                .priority(createTaskCmd.getPriority())
                .assignee(createTaskCmd.getAssignee() == null ? authenticationWrapper.getAuthenticatedUserId() : createTaskCmd.getAssignee())
                .create();
    }

    public FluentTask createNewSubtask(String parentTaskId,
                                       CreateTask createSubtaskCmd) {
        return taskRuntime.task(parentTaskId)
                .createSubTaskWith()
                .name(createSubtaskCmd.getName())
                .description(createSubtaskCmd.getDescription())
                .dueDate(createSubtaskCmd.getDueDate())
                .priority(createSubtaskCmd.getPriority())
                .assignee(createSubtaskCmd.getAssignee() == null ? authenticationWrapper.getAuthenticatedUserId() : createSubtaskCmd.getAssignee())
                .create();
    }

    public List<FluentTask> getSubtasks(String parentTaskId) {
        return taskRuntime.task(parentTaskId).subTasks();
    }

    public void deleteTask(String taskId) {
        FluentTask task = getTaskById(taskId);
        task.delete("Cancelled by " + authenticationWrapper.getAuthenticatedUserId());
    }

    public void updateTask(String taskId,
                           UpdateTaskCmd updateTaskCmd) {
        FluentTask task = taskRuntime.task(taskId);
        if (updateTaskCmd.getAssignee() != null) {
            task.claim(updateTaskCmd.getAssignee());
        }
        if (updateTaskCmd.getName() != null) {
            task.updateName(updateTaskCmd.getName());
        }
        if (updateTaskCmd.getDescription() != null) {
            task.updateDescription(updateTaskCmd.getDescription());
        }
        if (updateTaskCmd.getDueDate() != null) {
            task.updateDueDate(updateTaskCmd.getDueDate());
        }
        if (updateTaskCmd.getPriority() != null) {
            task.updatePriority(updateTaskCmd.getPriority());
        }
        if (updateTaskCmd.getParentTaskId() != null) {
            task.updateParentTaskId(updateTaskCmd.getParentTaskId());
        }
    }

    public void setTaskVariables(SetTaskVariables setTaskVariablesCmd) {
        taskRuntime.task(setTaskVariablesCmd.getTaskId())
                .variables(setTaskVariablesCmd.getVariables());
    }

    public void setTaskVariablesLocal(SetTaskVariables setTaskVariablesCmd) {
        taskRuntime.task(setTaskVariablesCmd.getTaskId())
                .localVariables(setTaskVariablesCmd.getVariables());
    }


    public List<VariableInstance> getVariableInstances(String taskId) {
        return taskRuntime.task(taskId).variables();
    }

    public List<VariableInstance> getLocalVariableInstances(String taskId) {
        return taskRuntime.task(taskId).localVariables();
    }

}
