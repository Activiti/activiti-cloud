/*
 * Copyright 2017-2020 Alfresco Software, Ltd.
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
package org.activiti.cloud.acc.core.steps.runtime.admin;

import static org.assertj.core.api.Assertions.assertThat;

import net.thucydides.core.annotations.Step;
import org.activiti.api.process.model.payloads.ReceiveMessagePayload;
import org.activiti.api.process.model.payloads.StartMessagePayload;
import org.activiti.cloud.acc.core.rest.feign.EnableRuntimeFeignContext;
import org.activiti.cloud.acc.core.services.runtime.admin.ProcessRuntimeAdminService;
import org.activiti.cloud.api.process.model.CloudProcessInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.PagedResources;

import java.io.IOException;

@EnableRuntimeFeignContext
public class ProcessRuntimeAdminSteps {

    @Autowired
    private ProcessRuntimeAdminService processRuntimeAdminService;

    @Step
    public void checkServicesHealth() {
        assertThat(processRuntimeAdminService.isServiceUp()).isTrue();
    }

    @Step
    public PagedResources<CloudProcessInstance> getProcessInstances(){
        return processRuntimeAdminService.getProcessInstances();
    }
    
    @Step
    public void deleteProcessInstance(String id) {
        processRuntimeAdminService.deleteProcess(id);
    }
    
    @Step
    public CloudProcessInstance message(StartMessagePayload payload) throws IOException {
        return processRuntimeAdminService.message(payload);
    }
    
    @Step
    public void message(ReceiveMessagePayload payload) throws IOException {
        processRuntimeAdminService.message(payload);
    }

}
