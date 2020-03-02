package org.activiti.cloud.starter.audit.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;

@Configuration
@Import(SwaggerConfig.class)
@PropertySource("classpath:activiti-cloud-audit-service.properties")
public class ActivitiAuditAutoConfiguration {

}
