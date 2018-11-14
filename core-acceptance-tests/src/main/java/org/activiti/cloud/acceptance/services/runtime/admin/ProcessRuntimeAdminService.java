package org.activiti.cloud.acceptance.services.runtime.admin;

import feign.Headers;
import feign.RequestLine;
import org.activiti.cloud.api.process.model.CloudProcessInstance;
import org.activiti.cloud.qa.service.BaseService;
import org.springframework.hateoas.PagedResources;

public interface ProcessRuntimeAdminService extends BaseService {

    @RequestLine("GET /admin/v1/process-instances")
    @Headers("Content-Type: application/json")
    PagedResources<CloudProcessInstance> getProcessInstances();

}
