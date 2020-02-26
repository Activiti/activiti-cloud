/*
 * Copyright 2019 Alfresco, Inc. and/or its affiliates.
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

package org.activiti.cloud.acc.core.steps.runtime.admin;

import net.thucydides.core.annotations.Step;
import org.activiti.api.task.model.builders.TaskPayloadBuilder;
import org.activiti.cloud.acc.core.rest.feign.EnableRuntimeFeignContext;
import org.activiti.cloud.acc.core.services.runtime.admin.TaskVariablesRuntimeAdminService;
import org.activiti.cloud.api.model.shared.CloudVariableInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Resources;

@EnableRuntimeFeignContext
public class TaskVariablesRuntimeAdminSteps {

    @Autowired
    private TaskVariablesRuntimeAdminService taskRuntimeService;

    @Step
    public void updateVariable(String taskId, String name, Object value){

        taskRuntimeService.updateTaskVariable(taskId, name, TaskPayloadBuilder.updateVariable().withTaskId(taskId)
                .withVariable(name, value).build());
    }

    @Step
    public void createVariable(String taskId,
                               String name,
                               Object value) {

        taskRuntimeService.createTaskVariable(taskId,
                                              TaskPayloadBuilder
                                                      .createVariable()
                                                      .withTaskId(taskId)
                                                      .withVariable(name,
                                                                    value)
                                                      .build());
    }

    @Step
    public Resources<CloudVariableInstance> getVariables(String taskId) {
        return taskRuntimeService.getVariables(taskId);
    }

}
