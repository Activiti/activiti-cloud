package org.activiti.cloud.starter.query.configuration;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan({"org.activiti.cloud.services.query",
        "org.activiti.cloud.alfresco",
        "org.activiti.spring.security.policies",
        "org.activiti.cloud.services.common.security",
        "org.activiti.cloud.services", //@TODO: we need to review this -> TaskLookupRestrictionService
        "org.activiti.cloud.services.identity"})
public class ActivitiQueryAutoConfiguration {

}
