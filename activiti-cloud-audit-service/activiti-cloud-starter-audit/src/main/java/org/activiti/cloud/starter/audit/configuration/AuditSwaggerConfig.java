/*
 * Copyright 2018 Alfresco, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.activiti.cloud.starter.audit.configuration;

import java.util.function.Predicate;
import org.activiti.cloud.common.swagger.DocketCustomizer;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.RequestHandler;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;

@Configuration
public class AuditSwaggerConfig {

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
    public Predicate<RequestHandler> auditApiSelector() {
        return RequestHandlerSelectors.basePackage("org.activiti.cloud.services.audit")::apply;
    }

    @Bean
    public DocketCustomizer PayloadsDocketCustomizer() {
        return new PayloadsDocketCustomizer();
    }

}
