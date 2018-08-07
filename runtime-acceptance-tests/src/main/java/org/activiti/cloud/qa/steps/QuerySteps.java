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
import org.activiti.cloud.qa.rest.feign.EnableRuntimeFeignContext;
import org.activiti.cloud.qa.service.QueryService;
import org.activiti.runtime.api.model.CloudProcessInstance;
import org.activiti.runtime.api.model.CloudTask;
import org.activiti.runtime.api.model.ProcessInstance.ProcessInstanceStatus;

import org.activiti.runtime.api.model.Task;
import org.activiti.runtime.api.model.Task.TaskStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.PagedResources;

import static org.assertj.core.api.Assertions.*;
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
                                           ProcessInstanceStatus expectedStatus) throws Exception {

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
    public void checkTaskStatus(String taskId,
                                TaskStatus expectedStatus) {

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
