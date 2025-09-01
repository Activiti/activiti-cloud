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
package org.activiti.cloud.acc.core.steps.runtime;

import static org.activiti.cloud.acc.core.assertions.RestErrorAssert.assertThatRestBadRequestErrorIsThrownBy;
import static org.activiti.cloud.acc.core.assertions.RestErrorAssert.assertThatRestNotFoundErrorIsThrownBy;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;
import java.util.Date;
import java.util.stream.Collectors;
import net.thucydides.core.annotations.Step;
import org.activiti.api.task.model.Task;
import org.activiti.api.task.model.builders.TaskPayloadBuilder;
import org.activiti.api.task.model.payloads.AssignTaskPayload;
import org.activiti.api.task.model.payloads.CompleteTaskPayload;
import org.activiti.api.task.model.payloads.CreateTaskPayload;
import org.activiti.api.task.model.payloads.SaveTaskPayload;
import org.activiti.cloud.acc.core.rest.PageRequest;
import org.activiti.cloud.acc.core.rest.RuntimeDirtyContextHandler;
import org.activiti.cloud.acc.core.rest.feign.EnableRuntimeFeignContext;
import org.activiti.cloud.acc.shared.service.BaseService;
import org.activiti.cloud.api.task.model.CloudTask;
import org.activiti.cloud.services.rest.api.TaskApiClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.EntityModel;

@EnableRuntimeFeignContext
public class TaskRuntimeBundleSteps {

    private static final Pageable DEFAULT_PAGEABLE = PageRequest.of(0, 100);

    @Autowired
    private RuntimeDirtyContextHandler dirtyContextHandler;

    @Autowired
    private TaskApiClient taskApiClient;

    @Autowired
    @Qualifier("runtimeBundleBaseService")
    private BaseService baseService;

    @Step
    public void checkServicesHealth() {
        assertThat(baseService.isServiceUp()).isTrue();
    }

    @Step
    public void claimTask(String id) {
        taskApiClient.claimTask(id);
    }

    @Step
    public void cannotClaimTask(String id) {
        assertThatRestNotFoundErrorIsThrownBy(() -> taskApiClient.claimTask(id))
            .withMessageContaining("Unable to find task for the given id: " + id);
    }

    @Step
    public void completeTask(String id, CompleteTaskPayload completeTaskPayload) {
        taskApiClient.completeTask(id, completeTaskPayload);
    }

    @Step
    public void saveTask(String id, SaveTaskPayload saveTaskPayload) {
        taskApiClient.saveTask(id, saveTaskPayload);
    }

    @Step
    public void cannotCompleteTask(String id, CompleteTaskPayload createTaskPayload) {
        assertThatRestNotFoundErrorIsThrownBy(() -> taskApiClient.completeTask(id, createTaskPayload))
            .withMessageContaining("Unable to find task for the given id: " + id);
    }

    @Step
    public CloudTask createNewTask() {
        CreateTaskPayload createTask = TaskPayloadBuilder
            .create()
            .withName("new-task")
            .withDescription("task-description")
            .withAssignee("testuser")
            .build();
        return dirtyContextHandler.dirty(taskApiClient.createNewTask(createTask).getContent());
    }

    @Step
    public CloudTask createNewUnassignedTask() {
        CreateTaskPayload createTask = TaskPayloadBuilder
            .create()
            .withName("unassigned-task")
            .withDescription("unassigned-task-description")
            .build();
        return dirtyContextHandler.dirty(taskApiClient.createNewTask(createTask).getContent());
    }

    public CloudTask createSubtask(String parentTaskId) {
        CreateTaskPayload subTask = TaskPayloadBuilder
            .create()
            .withName("subtask")
            .withDescription("subtask-description")
            .withAssignee("testuser")
            .withParentTaskId(parentTaskId)
            .build();
        return taskApiClient.createNewTask(subTask).getContent();
    }

    public Collection<CloudTask> getSubtasks(String parentTaskId) {
        return taskApiClient
            .getSubtasks(DEFAULT_PAGEABLE, parentTaskId)
            .getContent()
            .stream()
            .map(EntityModel::getContent)
            .collect(Collectors.toSet());
    }

    @Step
    public CloudTask getTaskById(String id) {
        return taskApiClient.getTaskById(id).getContent();
    }

    @Step
    public void deleteTask(String taskId) {
        taskApiClient.deleteTask(taskId);
    }

    @Step
    public void checkTaskNotFound(String taskId) {
        assertThatRestNotFoundErrorIsThrownBy(() -> taskApiClient.getTaskById(taskId))
            .withMessageContaining("Unable to find task");
    }

    @Step
    public Collection<CloudTask> getAllTasks() {
        return taskApiClient
            .getTasks(DEFAULT_PAGEABLE)
            .getContent()
            .stream()
            .map(EntityModel::getContent)
            .collect(Collectors.toSet());
    }

    @Step
    public void checkTaskStatus(String id, Task.TaskStatus status) {
        //once a task is completed, it disappears from the runtime bundle
        if (!status.equals(Task.TaskStatus.COMPLETED)) {
            assertThat(taskApiClient.getTaskById(id).getContent().getStatus()).isEqualTo(status);
        }
    }

    @Step
    public CloudTask setTaskName(String taskId, String taskName) {
        return taskApiClient.updateTask(taskId, TaskPayloadBuilder.update().withName(taskName).build()).getContent();
    }

    @Step
    public CloudTask setTaskFormKey(String taskId, String formKey) {
        return taskApiClient.updateTask(taskId, TaskPayloadBuilder.update().withFormKey(formKey).build()).getContent();
    }

    @Step
    public CloudTask setTaskPriority(String taskId, int priority) {
        return taskApiClient
            .updateTask(taskId, TaskPayloadBuilder.update().withPriority(priority).build())
            .getContent();
    }

    @Step
    public CloudTask setTaskDueDate(String taskId, Date dueDate) {
        return taskApiClient.updateTask(taskId, TaskPayloadBuilder.update().withDueDate(dueDate).build()).getContent();
    }

    @Step
    public void releaseTask(String taskId) {
        taskApiClient.releaseTask(taskId);
    }

    @Step
    public Collection<CloudTask> getTaskWithStandalone(boolean standalone) {
        return taskApiClient
            .getTasks(DEFAULT_PAGEABLE)
            .getContent()
            .stream()
            .filter(cloudTask -> cloudTask.getContent().isStandalone() == standalone)
            .map(EntityModel::getContent)
            .collect(Collectors.toSet());
    }

    @Step
    public void assignTask(String id, AssignTaskPayload assignTaskPayload) {
        taskApiClient.assign(id, assignTaskPayload);
    }

    @Step
    public void cannotAssignTask(String id, AssignTaskPayload assignTaskPayload) {
        assertThatRestBadRequestErrorIsThrownBy(() -> taskApiClient.assign(id, assignTaskPayload))
            .withMessageContaining("You cannot assign a task to " + assignTaskPayload.getAssignee());
    }
}
