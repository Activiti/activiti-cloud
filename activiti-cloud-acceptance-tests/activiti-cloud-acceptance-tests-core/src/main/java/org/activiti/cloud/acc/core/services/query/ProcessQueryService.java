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
package org.activiti.cloud.acc.core.services.query;

import feign.Headers;
import feign.Param;
import feign.RequestLine;
import org.activiti.api.process.model.ProcessDefinition;
import org.activiti.cloud.api.model.shared.CloudVariableInstance;
import org.activiti.cloud.api.process.model.CloudProcessInstance;
import org.springframework.hateoas.PagedModel;

public interface ProcessQueryService {
    @RequestLine("GET /v1/process-instances/{processInstanceId}")
    @Headers("Content-Type: application/json")
    CloudProcessInstance getProcessInstance(@Param("processInstanceId") String processInstanceId);

    @RequestLine("GET /v1/process-instances?sort=startDate%2Cdesc&sort=id%2Cdesc")
    @Headers("Content-Type: application/json")
    PagedModel<CloudProcessInstance> getProcessInstances();

    @RequestLine("GET /v1/process-instances/{processInstanceId}/variables")
    @Headers("Content-Type: application/json")
    PagedModel<CloudVariableInstance> getProcessInstanceVariables(@Param("processInstanceId") String processInstanceId);

    @RequestLine("GET /v1/process-definitions")
    @Headers("Content-Type: application/json")
    PagedModel<ProcessDefinition> getProcessDefinitions();

    @RequestLine("GET /v1/process-instances?name={processName}")
    PagedModel<CloudProcessInstance> getProcessInstancesByName(@Param("processName") String processName);

    @RequestLine("GET /v1/process-instances?processDefinitionKey={processDefinitionKey}")
    PagedModel<CloudProcessInstance> getProcessInstancesByProcessDefinitionKey(
        @Param("processDefinitionKey") String processDefinitionKey
    );
}
