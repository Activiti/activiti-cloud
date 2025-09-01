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

import net.thucydides.core.annotations.Step;
import org.activiti.cloud.acc.core.rest.feign.EnableRuntimeFeignContext;
import org.activiti.cloud.acc.core.services.runtime.admin.ServiceTasksAdminService;
import org.activiti.cloud.services.rest.api.ReplayServiceTaskRequest;
import org.springframework.beans.factory.annotation.Autowired;

@EnableRuntimeFeignContext
public class ServiceTasksAdminSteps {

    @Autowired
    private ServiceTasksAdminService serviceTasksAdminService;

    @Step
    public void replayServiceTask(String executionId, ReplayServiceTaskRequest request) {
        serviceTasksAdminService.replayServiceTask(executionId, request);
    }
}
