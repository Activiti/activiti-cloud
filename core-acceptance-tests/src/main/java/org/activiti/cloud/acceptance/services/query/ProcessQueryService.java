package org.activiti.cloud.acceptance.services.query;

import feign.Headers;
import feign.Param;
import feign.RequestLine;
import org.activiti.cloud.api.model.shared.CloudVariableInstance;
import org.activiti.cloud.api.process.model.CloudProcessInstance;
import org.activiti.cloud.qa.service.BaseService;
import org.springframework.hateoas.PagedResources;

public interface ProcessQueryService extends BaseService {

    @RequestLine("GET /v1/process-instances/{processInstanceId}")
    @Headers("Content-Type: application/json")
    CloudProcessInstance getProcessInstance(@Param("processInstanceId") String processInstanceId);

    @RequestLine("GET /v1/process-instances?sort=startDate,desc&sort=id,desc")
    @Headers("Content-Type: application/json")
    PagedResources<CloudProcessInstance> getProcessInstances();

    @RequestLine("GET /v1/process-instances/{processInstanceId}/variables")
    @Headers("Content-Type: application/json")
    PagedResources<CloudVariableInstance> getProcessInstanceVariables(@Param("processInstanceId") String processInstanceId);
}
