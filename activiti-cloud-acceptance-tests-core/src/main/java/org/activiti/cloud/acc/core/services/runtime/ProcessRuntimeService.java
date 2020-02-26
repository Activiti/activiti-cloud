package org.activiti.cloud.acc.core.services.runtime;

import feign.Headers;
import feign.Param;
import feign.RequestLine;
import org.activiti.api.process.model.ProcessDefinition;
import org.activiti.api.process.model.payloads.ReceiveMessagePayload;
import org.activiti.api.process.model.payloads.StartMessagePayload;
import org.activiti.api.process.model.payloads.StartProcessPayload;
import org.activiti.api.process.model.payloads.UpdateProcessPayload;
import org.activiti.cloud.acc.shared.service.BaseService;
import org.activiti.cloud.api.process.model.CloudProcessInstance;
import org.activiti.cloud.api.task.model.CloudTask;
import org.springframework.hateoas.PagedResources;

public interface ProcessRuntimeService extends BaseService {

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
    PagedResources<CloudProcessInstance> getAllProcessInstances();

    @RequestLine("GET /v1/process-instances/{id}")
    @Headers("Accept: application/hal+json;charset=UTF-8")
    CloudProcessInstance getProcessInstance(@Param("id") String id);

    @RequestLine("GET /v1/process-instances/{id}/subprocesses")
    @Headers("Content-Type: application/json")
    PagedResources<CloudProcessInstance> getSubProcesses(@Param("id") String id);

    @RequestLine("GET /v1/process-definitions")
    @Headers("Accept: application/hal+json;charset=UTF-8")
    PagedResources<ProcessDefinition> getProcessDefinitions();

    @RequestLine("GET /v1/process-definitions/{processDefinitionKey}")
    @Headers("Accept: application/hal+json;charset=UTF-8")
    ProcessDefinition getProcessDefinitionByKey(@Param("processDefinitionKey") String processDefinitionKey);

    @RequestLine("GET /v1/process-instances/{id}/tasks")
    @Headers("Accept: application/hal+json;charset=UTF-8")
    PagedResources<CloudTask> getProcessInstanceTasks(@Param("id") String id);

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
