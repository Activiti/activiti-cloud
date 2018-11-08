package services.audit;

import feign.Headers;
import feign.Param;
import feign.RequestLine;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.qa.service.BaseService;
import org.springframework.hateoas.PagedResources;

public interface AuditService extends BaseService {

    @RequestLine("GET /v1/events?search={search}")
    @Headers("Content-Type: application/json")
    PagedResources<CloudRuntimeEvent> getEvents(@Param("search") String search);

    @RequestLine("GET /v1/events?sort=timestamp,desc&sort=id,desc")
    @Headers("Content-Type: application/json")
    PagedResources<CloudRuntimeEvent> getEvents();
}
