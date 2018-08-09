package org.activiti.cloud.starter.rb.configuration;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan({"org.activiti.cloud.services",
        "org.activiti.cloud.alfresco",
        "org.activiti.spring.security.policies",
        "org.activiti.cloud.services.common.security",
        "org.activiti.cloud.services.identity"})
public class ActivitiRuntimeBundleAutoConfiguration {

}
