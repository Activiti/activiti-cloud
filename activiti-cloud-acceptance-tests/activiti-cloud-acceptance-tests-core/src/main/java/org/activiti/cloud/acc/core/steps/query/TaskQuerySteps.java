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
package org.activiti.cloud.acc.core.steps.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.awaitility.Awaitility.await;

import java.util.Collection;
import java.util.List;
import net.thucydides.core.annotations.Step;
import org.activiti.api.model.shared.model.VariableInstance;
import org.activiti.api.task.model.Task;
import org.activiti.cloud.acc.core.rest.feign.EnableRuntimeFeignContext;
import org.activiti.cloud.acc.core.services.query.TaskQueryService;
import org.activiti.cloud.acc.shared.service.BaseService;
import org.activiti.cloud.api.model.shared.CloudVariableInstance;
import org.activiti.cloud.api.task.model.CloudTask;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.PagedModel;

@EnableRuntimeFeignContext
public class TaskQuerySteps {

    @Autowired
    private TaskQueryService taskQueryService;

    @Autowired
    @Qualifier("queryBaseService")
    private BaseService baseService;

    @Step
    public void checkServicesHealth() {
        assertThat(baseService.isServiceUp()).isTrue();
    }

    @Step
    public void checkTaskStatus(String taskId, Task.TaskStatus expectedStatus) {
        await()
            .untilAsserted(() ->
                assertThat(taskQueryService.queryTasksByIdAnsStatus(taskId, expectedStatus).getContent())
                    .isNotNull()
                    .isNotEmpty()
                    .hasSize(1)
            );
    }

    @Step
    public void checkSubtaskHasParentTaskId(String subtaskId, String parentTaskId) {
        await()
            .untilAsserted(() -> {
                final Collection<CloudTask> tasks = taskQueryService.getTask(subtaskId).getContent();

                assertThat(tasks)
                    .isNotNull()
                    .isNotEmpty()
                    .hasSize(1)
                    .extracting(Task::getId, Task::getParentTaskId)
                    .containsOnly(tuple(subtaskId, parentTaskId));
            });
    }

    @Step
    public PagedModel<CloudTask> getAllTasks() {
        return taskQueryService.getTasks();
    }

    @Step
    public CloudTask getTaskById(String id) {
        return taskQueryService.getTask(id).getContent().iterator().next();
    }

    @Step
    public void checkTaskHasVariable(String taskId, String variableName, String variableValue) throws Exception {
        await()
            .untilAsserted(() -> {
                assertThat(variableName).isNotNull();

                final Collection<CloudVariableInstance> variableInstances = taskQueryService
                    .getTaskVariables(taskId)
                    .getContent();

                assertThat(variableInstances).isNotNull();
                assertThat(variableInstances).isNotEmpty();

                //one of the variables should have name matching variableName
                assertThat(variableInstances).extracting(VariableInstance::getName).contains(variableName);

                if (variableValue != null) {
                    assertThat(variableInstances)
                        .extracting(VariableInstance::getName, VariableInstance::getValue)
                        .contains(tuple(variableName, variableValue));
                }
            });
    }

    @Step
    public PagedModel<CloudTask> getTasksByProcessInstance(String processInstanceId) {
        return taskQueryService.getTasksByProcessInstance(processInstanceId);
    }

    @Step
    public PagedModel<CloudTask> getRootTasksByProcessInstance(String processInstanceId) {
        return taskQueryService.getRootTasksByProcessInstance(processInstanceId);
    }

    @Step
    public PagedModel<CloudTask> getStandaloneTasks() {
        return taskQueryService.getStandaloneTasks();
    }

    @Step
    public PagedModel<CloudTask> getNonStandaloneTasks() {
        return taskQueryService.getNonStandaloneTasks();
    }

    @Step
    public PagedModel<CloudTask> getTasksByNameAndDescription(String taskName, String taskDescription) {
        return taskQueryService.getTasksByNameAndDescription(taskName, taskDescription);
    }

    @Step
    public CollectionModel<CloudVariableInstance> getVariables(String taskId) {
        return taskQueryService.getVariables(taskId);
    }

    @Step
    public List<String> getCandidateGroups(String taskId) {
        return taskQueryService.getTaskCandidateGroups(taskId);
    }

    @Step
    public List<String> getCandidateUsers(String taskId) {
        return taskQueryService.getTaskCandidateUsers(taskId);
    }
}
