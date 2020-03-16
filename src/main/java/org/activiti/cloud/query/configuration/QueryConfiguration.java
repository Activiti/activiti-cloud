/*
 * Copyright 2005-2019 Alfresco Software, Ltd. All rights reserved.
 * License rights for this program may be obtained from Alfresco Software, Ltd.
 * pursuant to a written agreement and any use of this program without such an
 * agreement is prohibited.
 */
package org.activiti.cloud.query.configuration;

import org.activiti.api.process.model.payloads.GetProcessDefinitionsPayload;
import org.activiti.api.process.model.payloads.GetProcessInstancesPayload;
import org.activiti.api.runtime.shared.security.SecurityManager;
import org.activiti.core.common.spring.security.policies.ProcessSecurityPoliciesManager;
import org.activiti.core.common.spring.security.policies.ProcessSecurityPoliciesManagerImpl;
import org.activiti.core.common.spring.security.policies.SecurityPoliciesRestrictionApplier;
import org.activiti.core.common.spring.security.policies.conf.SecurityPoliciesProperties;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import springfox.documentation.RequestHandler;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;

import java.util.function.Predicate;

@Configuration
public class QueryConfiguration {

    @Bean
    public Predicate<RequestHandler> apiSelector() {
        return RequestHandlerSelectors.basePackage("org.activiti.cloud.services.query.rest")
                       .or(RequestHandlerSelectors.basePackage("org.activiti.cloud.services.audit"));
    }

    @Bean
    public ApiInfo apiInfo(BuildProperties buildProperties) {
        return new ApiInfoBuilder()
                       .title(String.format("%s ReST API", buildProperties.getName()))
                       .description(buildProperties.get("description"))
                       .version(buildProperties.getVersion())
                       .license(String.format("© %s-%s %s. All rights reserved",
                                              buildProperties.get("inceptionYear"),
                                              buildProperties.get("year"),
                                              buildProperties.get("organization.name")))
                       .termsOfServiceUrl(buildProperties.get("organization.url"))
                       .build();
    }

    @Bean
    @Primary
    public ProcessSecurityPoliciesManager processSecurityPoliciesManager(SecurityManager securityManager, SecurityPoliciesProperties securityPoliciesProperties, SecurityPoliciesRestrictionApplier<GetProcessDefinitionsPayload> processDefinitionRestrictionApplier, SecurityPoliciesRestrictionApplier<GetProcessInstancesPayload> processInstanceRestrictionApplier) {
        return new ProcessSecurityPoliciesManagerImpl(securityManager, securityPoliciesProperties, processDefinitionRestrictionApplier, processInstanceRestrictionApplier);
    }

}