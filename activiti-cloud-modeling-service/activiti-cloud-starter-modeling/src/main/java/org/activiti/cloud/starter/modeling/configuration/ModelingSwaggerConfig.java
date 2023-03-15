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
package org.activiti.cloud.starter.modeling.configuration;

import io.swagger.v3.oas.models.OpenAPI;
import org.activiti.cloud.common.swagger.springdoc.BaseOpenApiBuilder;
import org.springdoc.core.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ModelingSwaggerConfig {

    @Bean
    @ConditionalOnMissingBean
    public OpenAPI baseOpenApi(
        BaseOpenApiBuilder baseOpenApiBuilder,
        @Value("${activiti.cloud.swagger.modeling-base-path:}") String swaggerBasePath
    ) {
        return baseOpenApiBuilder.build("Modeling ReST API", swaggerBasePath);
    }

    @Bean
    @ConditionalOnMissingBean(name = "modelingApi")
    public GroupedOpenApi modelingApi() {
        return GroupedOpenApi
            .builder()
            .group("Modeling")
            .packagesToScan("org.activiti.cloud.services.modeling.rest")
            .build();
    }
}
