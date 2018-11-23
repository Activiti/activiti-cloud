package org.activiti.cloud.starter.rb.configuration;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@ComponentScan({"org.activiti.cloud.services",
        "org.activiti.cloud.alfresco",
        "org.activiti.core.common.spring.security.policies",
        "org.activiti.cloud.services.common.security",
        "org.activiti.cloud.services.identity"})
@Import(SwaggerConfig.class)
public class ActivitiRuntimeBundleAutoConfiguration {

}
