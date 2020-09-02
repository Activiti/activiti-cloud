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
package org.activiti.cloud.acc.core.services.runtime;

import feign.Headers;
import feign.Param;
import feign.RequestLine;
import org.activiti.api.process.model.ProcessDefinition;
import org.activiti.api.process.model.payloads.ReceiveMessagePayload;
import org.activiti.api.process.model.payloads.StartMessagePayload;
import org.activiti.api.process.model.payloads.StartProcessPayload;
import org.activiti.api.process.model.payloads.UpdateProcessPayload;
import org.activiti.cloud.api.process.model.CloudProcessInstance;
import org.activiti.cloud.api.task.model.CloudTask;
import org.springframework.hateoas.PagedModel;

public interface ProcessRuntimeService
{

    String PROCESS_INSTANCES_PATH = "/v1/process-instances/";

    @RequestLine("POST /v1/process-instances")
    @Headers("Content-Type: application/json")
    CloudProcessInstance startProcess(StartProcessPayload startProcess);

    @RequestLine("POST /v1/process-instances/{id}/suspend")
    @Headers("Content-Type: application/json")
    void suspendProcess(@Param("id") String id);

    @RequestLine("POST /v1/process-instances/{id}/resume")
    @Headers("Content-Type: application/json")
    void resumeProcess(@Param("id") String id);

    @RequestLine("DELETE /v1/process-instances/{id}")
    void deleteProcess(@Param("id") String id);

    @RequestLine("GET /v1/process-instances?sort=startDate,desc&sort=id,desc")
    @Headers("Accept: application/hal+json;charset=UTF-8")
    PagedModel<CloudProcessInstance> getAllProcessInstances();

    @RequestLine("GET /v1/process-instances/{id}")
    @Headers("Accept: application/hal+json;charset=UTF-8")
    CloudProcessInstance getProcessInstance(@Param("id") String id);

    @RequestLine("GET /v1/process-instances/{id}/subprocesses")
    @Headers("Content-Type: application/json")
    PagedModel<CloudProcessInstance> getSubProcesses(@Param("id") String id);

    @RequestLine("GET /v1/process-definitions")
    @Headers("Accept: application/hal+json;charset=UTF-8")
    PagedModel<ProcessDefinition> getProcessDefinitions();

    @RequestLine("GET /v1/process-definitions/{processDefinitionKey}")
    @Headers("Accept: application/hal+json;charset=UTF-8")
    ProcessDefinition getProcessDefinitionByKey(@Param("processDefinitionKey") String processDefinitionKey);

    @RequestLine("GET /v1/process-instances/{id}/tasks")
    @Headers("Accept: application/hal+json;charset=UTF-8")
    PagedModel<CloudTask> getProcessInstanceTasks(@Param("id") String id);

    @RequestLine("PUT /v1/process-instances/{id}")
    @Headers("Content-Type: application/json")
    CloudProcessInstance updateProcess(@Param("id") String id,
                                       UpdateProcessPayload updateProcessPayload);

    @RequestLine("POST /v1/process-instances/message")
    @Headers("Content-Type: application/json")
    CloudProcessInstance message(StartMessagePayload startProcess);

    @RequestLine("PUT /v1/process-instances/message")
    @Headers("Content-Type: application/json")
    void message(ReceiveMessagePayload startProcess);

}
