package steps.audit.admin;

import net.thucydides.core.annotations.Step;
import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.springframework.beans.factory.annotation.Autowired;
import rest.feign.EnableRuntimeFeignContext;
import services.audit.admin.AuditAdminService;

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
}
