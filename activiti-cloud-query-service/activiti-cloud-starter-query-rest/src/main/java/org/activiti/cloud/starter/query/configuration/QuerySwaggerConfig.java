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
package org.activiti.cloud.starter.query.configuration;

import org.activiti.cloud.common.swagger.springdoc.BaseOpenApiBuilder;
import org.activiti.cloud.common.swagger.springdoc.SwaggerDocUtils;
import org.activiti.cloud.services.query.rest.VariableSearch;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QuerySwaggerConfig implements InitializingBean {

    @Bean
    @ConditionalOnMissingBean(name = "queryApi")
    public GroupedOpenApi queryApi(@Value("${activiti.cloud.swagger.query-base-path:}") String querySwaggerBasePath) {
        return GroupedOpenApi
            .builder()
            .group("Query")
            .packagesToScan("org.activiti.cloud.services.query.rest")
            .addOpenApiCustomizer(openApi ->
                openApi.addExtension(BaseOpenApiBuilder.SERVICE_URL_PREFIX, querySwaggerBasePath)
            )
            .build();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        SwaggerDocUtils.replaceParameterObjectWithClass(VariableSearch.class, VariableSearchWrapperMixin.class);
    }
}
