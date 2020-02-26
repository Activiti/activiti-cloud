package org.activiti.cloud.starter.audit.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(SwaggerConfig.class)
public class ActivitiAuditAutoConfiguration {

}
