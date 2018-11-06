package services.query.admin;

import feign.Headers;
import feign.RequestLine;
import org.activiti.cloud.api.process.model.CloudProcessInstance;
import org.activiti.cloud.qa.service.BaseService;
import org.springframework.hateoas.PagedResources;

public interface ProcessQueryAdminService extends BaseService {

    @RequestLine("GET /admin/v1/process-instances?sort=startDate,desc&sort=id,desc")
    @Headers("Content-Type: application/json")
    PagedResources<CloudProcessInstance> getProcessInstances();
}
