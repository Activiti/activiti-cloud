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
package org.activiti.cloud.services.query.util;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.activiti.api.task.model.Task;
import org.activiti.cloud.services.query.app.repository.TaskCandidateGroupRepository;
import org.activiti.cloud.services.query.app.repository.TaskCandidateUserRepository;
import org.activiti.cloud.services.query.app.repository.TaskRepository;
import org.activiti.cloud.services.query.app.repository.TaskVariableRepository;
import org.activiti.cloud.services.query.model.ProcessInstanceEntity;
import org.activiti.cloud.services.query.model.TaskCandidateGroupEntity;
import org.activiti.cloud.services.query.model.TaskCandidateUserEntity;
import org.activiti.cloud.services.query.model.TaskEntity;
import org.activiti.cloud.services.query.model.TaskVariableEntity;

public class TaskBuilder {

    private final TaskEntity task;
    private final Set<String> candidateUserIds = new HashSet<>();
    private final Set<String> candidateGroupsIds = new HashSet<>();

    private final TaskRepository taskRepository;
    private final TaskVariableRepository taskVariableRepository;
    private final TaskCandidateUserRepository taskCandidateUserRepository;
    private final TaskCandidateGroupRepository taskCandidateGroupRepository;

    public TaskBuilder(
        TaskRepository taskRepository,
        TaskVariableRepository taskVariableRepository,
        TaskCandidateUserRepository taskCandidateUserRepository,
        TaskCandidateGroupRepository taskCandidateGroupRepository
    ) {
        this.taskRepository = taskRepository;
        this.taskVariableRepository = taskVariableRepository;
        this.taskCandidateUserRepository = taskCandidateUserRepository;
        this.taskCandidateGroupRepository = taskCandidateGroupRepository;
        this.task = new TaskEntity();
        this.task.setId(UUID.randomUUID().toString());
        this.task.setName(UUID.randomUUID().toString());
    }

    public TaskBuilder withId(String id) {
        task.setId(id);
        return this;
    }

    public TaskBuilder withName(String name) {
        task.setName(name);
        return this;
    }

    public TaskBuilder withDescription(String description) {
        task.setDescription(description);
        return this;
    }

    public TaskBuilder withParentTask(TaskEntity parentTask) {
        task.setParentTaskId(parentTask.getId());
        return this;
    }

    public TaskBuilder withPriority(Integer priority) {
        task.setPriority(priority);
        return this;
    }

    public TaskBuilder withAssignee(String assignee) {
        task.setAssignee(assignee);
        task.setStatus(Task.TaskStatus.ASSIGNED);
        return this;
    }

    public TaskBuilder withStatus(Task.TaskStatus status) {
        task.setStatus(status);
        return this;
    }

    public TaskBuilder withVariables(QueryTestUtils.VariableInput... variables) {
        task.setVariables(
            Arrays
                .stream(variables)
                .map(variable -> {
                    TaskVariableEntity taskVariable = new TaskVariableEntity();
                    taskVariable.setName(variable.name());
                    taskVariable.setType(variable.type().name().toLowerCase());
                    taskVariable.setValue(variable.value());
                    return taskVariable;
                })
                .collect(Collectors.toSet())
        );
        return this;
    }

    public TaskBuilder withParentProcess(ProcessInstanceEntity process) {
        process.getTasks().add(task);
        task.setProcessInstanceId(process.getId());
        task.setProcessDefinitionId(process.getProcessDefinitionId());
        task.setProcessDefinitionName(process.getProcessDefinitionName());
        task.setProcessVariables(process.getVariables());
        return this;
    }

    public TaskBuilder withTaskCandidateUsers(String... users) {
        candidateUserIds.addAll(Arrays.asList(users));
        return this;
    }

    public TaskBuilder withTaskCandidateGroups(String... groups) {
        candidateGroupsIds.addAll(Arrays.asList(groups));
        return this;
    }

    public TaskBuilder withCompletedBy(String completedBy) {
        task.setCompletedBy(completedBy);
        return this;
    }

    public TaskBuilder withCompletedDate(Date completedDate) {
        task.setCompletedDate(completedDate);
        return this;
    }

    public TaskBuilder withCreatedDate(Date createdDate) {
        task.setCreatedDate(createdDate);
        return this;
    }

    public TaskBuilder withDueDate(Date dueDate) {
        task.setDueDate(dueDate);
        return this;
    }

    public TaskBuilder withClaimedDate(Date claimedDate) {
        task.setClaimedDate(claimedDate);
        return this;
    }

    public TaskBuilder withLastModifiedDate(Date modifiedDate) {
        task.setLastModified(modifiedDate);
        return this;
    }

    public TaskBuilder withOwner(String owner) {
        task.setOwner(owner);
        return this;
    }

    public TaskEntity buildAndSave() {
        Set<TaskCandidateUserEntity> candidateUsers =
            this.candidateUserIds.stream()
                .map(user -> new TaskCandidateUserEntity(task.getId(), user))
                .collect(Collectors.toSet());
        taskCandidateUserRepository.saveAll(candidateUsers);
        task.setTaskCandidateUsers(candidateUsers);
        Set<TaskCandidateGroupEntity> candidateGroups =
            this.candidateGroupsIds.stream()
                .map(group -> new TaskCandidateGroupEntity(task.getId(), group))
                .collect(Collectors.toSet());
        taskCandidateGroupRepository.saveAll(candidateGroups);
        task.setTaskCandidateGroups(candidateGroups);
        task
            .getVariables()
            .forEach(variable -> {
                variable.setTaskId(task.getId());
                variable.setProcessInstanceId(task.getProcessInstanceId());
            });
        taskVariableRepository.saveAll(task.getVariables());
        if (task.getCreatedDate() == null) {
            task.setCreatedDate(new Date());
        }
        return taskRepository.save(task);
    }

    public TaskBuilder withProcessDefinitionName(String processDefinitionName) {
        task.setProcessDefinitionName(processDefinitionName);
        return this;
    }
}
