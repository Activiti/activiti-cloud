package org.activiti.cloud.services.audit.jpa.controllers.config;

import org.activiti.cloud.services.audit.jpa.controllers.AuditEventsAdminControllerImpl;
import org.activiti.cloud.services.audit.jpa.controllers.AuditEventsControllerImpl;
import org.activiti.cloud.services.audit.jpa.controllers.AuditEventsDeleteController;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({
    AuditEventsAdminControllerImpl.class,
    AuditEventsControllerImpl.class,
    AuditEventsDeleteController.class
})
public class AuditJPAControllersAutoConfiguration {

}
