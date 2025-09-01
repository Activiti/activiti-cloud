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
package org.activiti.cloud.acc.core.operations.steps.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import net.serenitybdd.core.Serenity;
import net.thucydides.core.annotations.Step;
import org.activiti.api.process.model.builders.ProcessPayloadBuilder;
import org.activiti.api.process.model.builders.StartProcessPayloadBuilder;
import org.activiti.cloud.acc.core.rest.RuntimeDirtyContextHandler;
import org.activiti.cloud.acc.core.rest.feign.EnableRuntimeFeignContext;
import org.activiti.cloud.acc.shared.service.BaseService;
import org.activiti.cloud.api.process.model.CloudProcessInstance;
import org.activiti.cloud.services.rest.api.ProcessInstanceApiClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

@EnableRuntimeFeignContext
public class ProcessRuntimeSteps {

    @Autowired
    private RuntimeDirtyContextHandler dirtyContextHandler;

    @Autowired
    private ProcessInstanceApiClient processInstanceApiClient;

    @Autowired
    @Qualifier("runtimeBundleBaseService")
    private BaseService baseService;

    @Step
    public void checkServicesHealth() {
        assertThat(baseService.isServiceUp()).isTrue();
    }

    @Step
    public CloudProcessInstance startProcess(String processDefinitionName) {
        StartProcessPayloadBuilder payload = ProcessPayloadBuilder
            .start()
            .withProcessDefinitionKey(processDefinitionName)
            .withName("processInstanceName")
            .withBusinessKey("businessKey");

        if (Serenity.sessionVariableCalled("variables") != null) {
            payload.withVariable("test-variable-name", "test-variable-value");
        }

        return dirtyContextHandler.dirty(processInstanceApiClient.startProcess(payload.build()).getContent());
    }

    @Step
    public void deleteProcessInstance(String processInstanceId) {
        processInstanceApiClient.deleteProcessInstance(processInstanceId);
    }

    @Step
    public void suspendProcessInstance(String processInstanceId) {
        processInstanceApiClient.suspend(processInstanceId);
    }
}
