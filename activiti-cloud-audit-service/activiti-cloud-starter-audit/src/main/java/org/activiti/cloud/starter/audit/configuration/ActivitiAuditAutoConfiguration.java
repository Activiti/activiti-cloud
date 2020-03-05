package org.activiti.cloud.starter.audit.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(AuditSwaggerConfig.class)
public class ActivitiAuditAutoConfiguration {

}
