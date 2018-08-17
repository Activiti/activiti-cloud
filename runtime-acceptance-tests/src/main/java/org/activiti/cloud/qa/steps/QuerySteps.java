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

package org.activiti.cloud.qa.steps;

import java.util.Collection;
import java.util.Map;

import net.thucydides.core.annotations.Step;
import org.activiti.api.model.shared.model.VariableInstance;
import org.activiti.api.process.model.ProcessInstance;
import org.activiti.api.task.model.Task;
import org.activiti.cloud.api.model.shared.CloudVariableInstance;
import org.activiti.cloud.api.process.model.CloudProcessInstance;
import org.activiti.cloud.api.task.model.CloudTask;
import org.activiti.cloud.qa.rest.feign.EnableRuntimeFeignContext;
import org.activiti.cloud.qa.service.QueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.PagedResources;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.awaitility.Awaitility.await;

/**
 * Query steps
 */
@EnableRuntimeFeignContext
public class QuerySteps {

    @Autowired
    private QueryService queryService;

    @Step
    public void checkServicesHealth() {
        assertThat(queryService.isServiceUp()).isTrue();
    }

    @Step
    public Map<String, Object> health() {
        return queryService.health();
    }

    @Step
    public CloudProcessInstance getProcessInstance(String processInstanceId) throws Exception {
        return queryService.getProcessInstance(processInstanceId);
    }

    @Step
    public PagedResources<CloudProcessInstance> getAllProcessInstances(){
        return queryService.getAllProcessInstances();
    }

    @Step
    public PagedResources<CloudProcessInstance> getAllProcessInstancesAdmin(){
        return queryService.getAllProcessInstancesAdmin();
    }

    @Step
    public void checkProcessInstanceStatus(String processInstanceId,
                                           ProcessInstance.ProcessInstanceStatus expectedStatus) throws Exception {

        await().untilAsserted(() -> {

            assertThat(expectedStatus).isNotNull();
            CloudProcessInstance processInstance = getProcessInstance(processInstanceId);
            assertThat(processInstance).isNotNull();
            assertThat(processInstance.getStatus()).isEqualTo(expectedStatus);
            assertThat(processInstance.getServiceName()).isNotEmpty();
            assertThat(processInstance.getServiceFullName()).isNotEmpty();

        });
    }

    @Step
    public void checkProcessInstanceHasVariable(String processInstanceId, String variableName) throws Exception {

        await().untilAsserted(() -> {

            assertThat(variableName).isNotNull();

            final Collection<CloudVariableInstance> variableInstances = queryService.getProcessInstanceVariables(processInstanceId).getContent();

            assertThat(variableInstances).isNotNull();
            assertThat(variableInstances).isNotEmpty();

            //one of the variables should have name matching variableName
            assertThat(variableInstances).extracting(VariableInstance::getName).contains(variableName);

        });
    }

    @Step
    public void checkTaskStatus(String taskId,
                                Task.TaskStatus expectedStatus) {

        await().untilAsserted(() -> assertThat(queryService.queryTasksByIdAnsStatus(taskId,
                expectedStatus).getContent())
                .isNotNull()
                .isNotEmpty()
                .hasSize(1));
    }

    @Step
    public void checkSubtaskHasParentTaskId(String subtaskId,
                                            String parentTaskId) {

        await().untilAsserted(() -> {

            final Collection<CloudTask> tasks = queryService.queryTasksById(subtaskId).getContent();

            assertThat(tasks).isNotNull().isNotEmpty().hasSize(1).extracting(Task::getId,
                    Task::getParentTaskId).containsOnly(tuple(subtaskId,
                    parentTaskId));

        });


    }

    @Step
    public PagedResources<CloudTask> getAllTasks(){
        return queryService.queryAllTasks();
    }


}
