package org.activiti.cloud.acc.core.services.runtime.admin;

import feign.Headers;
import feign.Param;
import feign.RequestLine;
import org.activiti.api.process.model.payloads.ReceiveMessagePayload;
import org.activiti.api.process.model.payloads.StartMessagePayload;
import org.activiti.cloud.acc.shared.service.BaseService;
import org.activiti.cloud.api.process.model.CloudProcessInstance;
import org.springframework.hateoas.PagedResources;

public interface ProcessRuntimeAdminService extends BaseService {

    @RequestLine("GET /admin/v1/process-instances?sort=startDate,desc&sort=id,desc")
    @Headers("Accept: application/hal+json;charset=UTF-8")
    PagedResources<CloudProcessInstance> getProcessInstances();
    
    @RequestLine("DELETE /admin/v1/process-instances/{id}")
    void deleteProcess(@Param("id") String id);
    
    @RequestLine("POST /admin/v1/process-instances/message")
    @Headers("Content-Type: application/json")
    CloudProcessInstance message(StartMessagePayload startProcess);

    @RequestLine("PUT /admin/v1/process-instances/message")
    @Headers("Content-Type: application/json")
    void message(ReceiveMessagePayload startProcess);    
}
