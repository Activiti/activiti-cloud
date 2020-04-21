package org.activiti.cloud.acc.core.services.audit.admin;

import feign.Param;
import feign.RequestLine;
import org.activiti.cloud.acc.shared.service.BaseService;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.CollectionModel;

public interface AuditAdminService extends BaseService {

    @RequestLine("GET /admin/v1/events?search={search}&sort=timestamp,desc&sort=id,desc")
    PagedModel<CloudRuntimeEvent> getEvents(@Param("search") String search);

    @RequestLine("GET /admin/v1/events?sort=timestamp,desc&sort=id,desc")
    PagedModel<CloudRuntimeEvent> getEvents();

    @RequestLine("DELETE /admin/v1/events")
    CollectionModel<EntityModel<CloudRuntimeEvent>> deleteEvents();
}
