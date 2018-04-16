package org.activiti.cloud.starter.audit.configuration;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySources;
import org.springframework.context.annotation.PropertySource;

@Configuration
@ComponentScan({"org.activiti.cloud.services.audit","org.activiti.cloud.services.security","org.activiti.cloud.services.identity","org.activiti.cloud.alfresco"})
public class ActivitiAuditAutoConfiguration {

}
