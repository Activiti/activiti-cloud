package org.activiti.cloud.acc.core.services.query.admin;

import feign.Headers;
import feign.RequestLine;
import org.activiti.cloud.api.process.model.CloudProcessDefinition;
import org.activiti.cloud.api.process.model.CloudProcessInstance;
import org.activiti.cloud.acc.shared.service.BaseService;
import org.activiti.cloud.api.task.model.CloudTask;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.Resources;

import java.util.function.Predicate;

public interface ProcessQueryAdminService extends BaseService {

    @RequestLine("GET /admin/v1/process-definitions")
    @Headers("Content-Type: application/json")
    PagedResources<CloudProcessDefinition> getProcessDefinitions();

    @RequestLine("GET /admin/v1/process-instances?sort=startDate,desc&sort=id,desc")
    @Headers("Content-Type: application/json")
    PagedResources<CloudProcessInstance> getProcessInstances();

    @RequestLine("DELETE /admin/v1/process-instances")
    Resources<Resource<CloudProcessInstance>> deleteProcessInstances();
}
