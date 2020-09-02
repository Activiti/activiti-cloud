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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;
import java.util.stream.Collectors;
import net.thucydides.core.annotations.Step;
import org.activiti.api.task.model.builders.TaskPayloadBuilder;
import org.activiti.cloud.acc.core.rest.feign.EnableRuntimeFeignContext;
import org.activiti.cloud.acc.shared.service.BaseService;
import org.activiti.cloud.api.model.shared.CloudVariableInstance;
import org.activiti.cloud.services.rest.api.TaskVariableApiClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.hateoas.EntityModel;

@EnableRuntimeFeignContext
public class TaskVariableRuntimeBundleSteps {

    @Autowired
    private TaskVariableApiClient taskVariableApiClient;

    @Autowired
    @Qualifier("runtimeBundleBaseService")
    private BaseService baseService;

    @Step
    public void checkServicesHealth() {
        assertThat(baseService.isServiceUp()).isTrue();
    }

    @Step
    public void updateVariable(String taskId, String name, Object value) {

        taskVariableApiClient.updateVariable(taskId, name, TaskPayloadBuilder.updateVariable().withTaskId(taskId)
            .withVariable(name, value).build());
    }

    @Step
    public void createVariable(String taskId,
        String name,
        Object value) {

        taskVariableApiClient.createVariable(taskId,
            TaskPayloadBuilder
                .createVariable()
                .withTaskId(taskId)
                .withVariable(name,
                    value)
                .build());
    }

    @Step
    public Collection<CloudVariableInstance> getVariables(String taskId) {
        return taskVariableApiClient.getVariables(taskId)
            .getContent()
            .stream()
            .map(EntityModel::getContent)
            .collect(Collectors.toList());
    }
}
