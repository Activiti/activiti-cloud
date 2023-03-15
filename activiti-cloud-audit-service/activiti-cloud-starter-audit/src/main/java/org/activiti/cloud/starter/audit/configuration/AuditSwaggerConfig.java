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
package org.activiti.cloud.starter.audit.configuration;

import org.activiti.cloud.api.model.shared.events.CloudRuntimeEvent;
import org.activiti.cloud.common.swagger.springdoc.BaseOpenApiBuilder;
import org.activiti.cloud.common.swagger.springdoc.SwaggerDocUtils;
import org.activiti.cloud.services.audit.api.converters.CloudRuntimeEventModel;
import org.springdoc.core.GroupedOpenApi;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AuditSwaggerConfig implements InitializingBean {

    @Bean
    @ConditionalOnMissingBean(name = "auditApi")
    public GroupedOpenApi auditApi(@Value("${activiti.cloud.swagger.audit-base-path:}") String swaggerBasePath) {
        return GroupedOpenApi
            .builder()
            .group("Audit")
            .packagesToScan("org.activiti.cloud.services.audit")
            .addOpenApiCustomiser(openApi ->
                openApi.addExtension(BaseOpenApiBuilder.SERVICE_URL_PREFIX, swaggerBasePath)
            )
            .build();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        SwaggerDocUtils.replaceWithClass(CloudRuntimeEvent.class, CloudRuntimeEventModel.class);
    }
}
