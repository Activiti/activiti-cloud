package org.activiti.cloud.acc.core.services.runtime.admin;

import feign.Headers;
import feign.Param;
import feign.RequestLine;
import org.activiti.cloud.acc.shared.service.BaseService;
import org.activiti.cloud.api.process.model.CloudProcessInstance;
import org.springframework.hateoas.PagedResources;

public interface ProcessRuntimeAdminService extends BaseService {

    @RequestLine("GET /admin/v1/process-instances")
    @Headers("Accept: application/hal+json;charset=UTF-8")
    PagedResources<CloudProcessInstance> getProcessInstances();
    
    @RequestLine("DELETE /admin/v1/process-instances/{id}")
    void deleteProcess(@Param("id") String id);

}
