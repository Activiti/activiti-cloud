package org.activiti.cloud.acc.core.services.audit.admin;

import feign.Headers;
import feign.Param;
import feign.RequestLine;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.acc.shared.service.BaseService;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.Resources;

public interface AuditAdminService extends BaseService {

    @RequestLine("GET /admin/v1/events?search={search}&sort=timestamp,desc&sort=id,desc")
    @Headers("Content-Type: application/json")
    PagedResources<CloudRuntimeEvent> getEvents(@Param("search") String search);

    @RequestLine("GET /admin/v1/events?sort=timestamp,desc&sort=id,desc")
    @Headers("Content-Type: application/json")
    PagedResources<CloudRuntimeEvent> getEvents();

    @RequestLine("DELETE /admin/v1/events")
    Resources<Resource<CloudRuntimeEvent>> deleteEvents();
}
