/*
 * Copyright 2017-2020 Alfresco Software, Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.activiti.cloud.query.configuration;

import io.swagger.v3.oas.models.OpenAPI;
import org.activiti.api.process.model.payloads.GetProcessDefinitionsPayload;
import org.activiti.api.process.model.payloads.GetProcessInstancesPayload;
import org.activiti.api.runtime.shared.security.SecurityManager;
import org.activiti.cloud.common.swagger.springdoc.BaseOpenApiBuilder;
import org.activiti.core.common.spring.security.policies.ProcessSecurityPoliciesManager;
import org.activiti.core.common.spring.security.policies.ProcessSecurityPoliciesManagerImpl;
import org.activiti.core.common.spring.security.policies.SecurityPoliciesRestrictionApplier;
import org.activiti.core.common.spring.security.policies.conf.SecurityPoliciesProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class QueryConfiguration {

    @Bean(name = "baseOpenApi")
    public OpenAPI baseOpenApi(
        BaseOpenApiBuilder baseOpenApiBuilder,
        @Value("${server.servlet.context-path:/}") String swaggerBasePath,
        BuildProperties buildProperties
    ) {
        return baseOpenApiBuilder.build(String.format("%s ReST API", buildProperties.getName()), swaggerBasePath);
    }

    @Bean
    @Primary
    public ProcessSecurityPoliciesManager processSecurityPoliciesManager(
        SecurityManager securityManager,
        SecurityPoliciesProperties securityPoliciesProperties,
        SecurityPoliciesRestrictionApplier<GetProcessDefinitionsPayload> processDefinitionRestrictionApplier,
        SecurityPoliciesRestrictionApplier<GetProcessInstancesPayload> processInstanceRestrictionApplier
    ) {
        return new ProcessSecurityPoliciesManagerImpl(
            securityManager,
            securityPoliciesProperties,
            processDefinitionRestrictionApplier,
            processInstanceRestrictionApplier
        );
    }
}
