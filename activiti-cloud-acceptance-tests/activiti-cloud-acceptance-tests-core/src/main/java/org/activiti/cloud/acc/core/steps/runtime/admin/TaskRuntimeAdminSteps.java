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
package org.activiti.cloud.acc.core.steps.runtime.admin;

import static org.assertj.core.api.Assertions.assertThat;

import net.thucydides.core.annotations.Step;
import org.activiti.api.task.model.payloads.AssignTaskPayload;
import org.activiti.api.task.model.payloads.CompleteTaskPayload;
import org.activiti.api.task.model.payloads.UpdateTaskPayload;
import org.activiti.cloud.acc.core.rest.feign.EnableRuntimeFeignContext;
import org.activiti.cloud.acc.core.services.runtime.admin.TaskRuntimeAdminService;
import org.activiti.cloud.acc.shared.service.BaseService;
import org.activiti.cloud.api.task.model.CloudTask;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.hateoas.PagedModel;

@EnableRuntimeFeignContext
public class TaskRuntimeAdminSteps {

    @Autowired
    private TaskRuntimeAdminService taskRuntimeAdminService;

    @Autowired
    @Qualifier("runtimeBundleBaseService")
    private BaseService baseService;

    @Step
    public void checkServicesHealth() {
        assertThat(baseService.isServiceUp()).isTrue();
    }

    @Step
    public void completeTask(String id, CompleteTaskPayload completeTaskPayload) {
        taskRuntimeAdminService.completeTask(id, completeTaskPayload);
    }

    @Step
    public void deleteTask(String taskId) {
        taskRuntimeAdminService.deleteTask(taskId);
    }

    @Step
    public PagedModel<CloudTask> getAllTasks() {
        return taskRuntimeAdminService.getTasks();
    }

    @Step
    public CloudTask updateTask(String taskId, UpdateTaskPayload updateTaskPayload) {
        return taskRuntimeAdminService.updateTask(taskId, updateTaskPayload);
    }

    @Step
    public CloudTask assignTask(String taskId, AssignTaskPayload assignTaskPayload) {
        return taskRuntimeAdminService.assign(taskId, assignTaskPayload);
    }
}
