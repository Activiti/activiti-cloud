/*
 * Copyright 2017-2025 Hyland Software, Inc. and its affiliates.
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
import org.activiti.api.process.model.payloads.SetProcessVariablesPayload;
import org.activiti.cloud.acc.core.rest.feign.EnableRuntimeFeignContext;
import org.activiti.cloud.acc.shared.service.BaseService;
import org.activiti.cloud.api.model.shared.CloudVariableInstance;
import org.activiti.cloud.services.rest.api.ProcessInstanceVariableApiClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;

@EnableRuntimeFeignContext
public class ProcessVariablesRuntimeBundleSteps {

    @Autowired
    private ProcessInstanceVariableApiClient processInstanceVariableApiClient;

    @Autowired
    @Qualifier("runtimeBundleBaseService")
    private BaseService baseService;

    @Step
    public void checkServicesHealth() {
        assertThat(baseService.isServiceUp()).isTrue();
    }

    @Step
    public Collection<CloudVariableInstance> getVariables(String processInstanceId) {
        return processInstanceVariableApiClient
            .getVariables(processInstanceId)
            .getContent()
            .stream()
            .map(EntityModel::getContent)
            .collect(Collectors.toList());
    }

    @Step
    public ResponseEntity<Void> updateVariables(String id, SetProcessVariablesPayload setProcessVariablesPayload) {
        return processInstanceVariableApiClient.updateVariables(id, setProcessVariablesPayload);
    }
}
