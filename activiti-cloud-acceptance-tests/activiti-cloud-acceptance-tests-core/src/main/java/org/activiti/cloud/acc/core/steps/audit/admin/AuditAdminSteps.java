package org.activiti.cloud.acc.core.steps.audit.admin;

import net.thucydides.core.annotations.Step;
import org.activiti.cloud.acc.core.rest.feign.EnableRuntimeFeignContext;
import org.activiti.cloud.acc.core.services.audit.admin.AuditAdminService;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.CollectionModel;

import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;

@EnableRuntimeFeignContext
public class AuditAdminSteps {

    @Autowired
    private AuditAdminService auditAdminService;

    @Step
    public void checkServicesHealth() {
        assertThat(auditAdminService.isServiceUp()).isTrue();
    }

    @Step
    public Collection<CloudRuntimeEvent> getEventsByEntityIdAdmin(String entityId){
        String filter = "entityId:";
        return auditAdminService.getEvents(filter + entityId).getContent();
    }

    @Step
    public CollectionModel<EntityModel<CloudRuntimeEvent>> deleteEvents(){
        return auditAdminService.deleteEvents();
    }

    @Step
    public PagedModel<CloudRuntimeEvent> getEventsAdmin(){
        return auditAdminService.getEvents();
    }
}
