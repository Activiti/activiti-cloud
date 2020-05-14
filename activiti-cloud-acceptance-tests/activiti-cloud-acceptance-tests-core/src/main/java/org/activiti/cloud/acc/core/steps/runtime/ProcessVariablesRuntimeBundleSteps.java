/*
 * Copyright 2017-2020 Alfresco.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.activiti.cloud.acc.core.steps.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import net.thucydides.core.annotations.Step;
import org.activiti.api.process.model.payloads.SetProcessVariablesPayload;
import org.activiti.cloud.acc.core.rest.feign.EnableRuntimeFeignContext;
import org.activiti.cloud.acc.core.services.runtime.ProcessVariablesRuntimeService;
import org.activiti.cloud.api.model.shared.CloudVariableInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Resources;
import org.springframework.http.ResponseEntity;

@EnableRuntimeFeignContext
public class ProcessVariablesRuntimeBundleSteps {

    @Autowired
    private ProcessVariablesRuntimeService processVariablesRuntimeService;

    @Step
    public void checkServicesHealth() {
        assertThat(processVariablesRuntimeService.isServiceUp()).isTrue();
    }
    
    @Step
    public Resources<CloudVariableInstance> getVariables(String id) {
        return processVariablesRuntimeService.getVariables(id);
    }
    
    @Step
    public ResponseEntity<Void> setVariables(String id,
                                      SetProcessVariablesPayload setProcessVariablesPayload) {
        return processVariablesRuntimeService.setVariables(id, setProcessVariablesPayload);
    }
    
}
