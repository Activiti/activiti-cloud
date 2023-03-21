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
package org.activiti.cloud.common.swagger.springdoc.conf;

import io.swagger.v3.oas.models.security.OAuthFlow;
import org.activiti.cloud.common.swagger.springdoc.BaseOpenApiBuilder;
import org.activiti.cloud.common.swagger.springdoc.customizer.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.PropertySource;

/**
 * Provides base springdoc configuration for swagger auto-generated specification file.
 * It provides swagger specification file under default path `springdoc/v3/api-docs`
 * and provides specification for Alfresco MediaType format
 * This configuration is not self-contained: when adding this as dependency provide following properties
 * <pre>
 *     springdoc.packages-to-scan=[base-package-to-scan]
 *     springdoc.api-docs.path=[path-to-custom-api-docs]
 * </pre>
 * and a bean for OpenApi:
 * <pre>
 *     &#64;Bean
 *     public OpenAPI baseOpenApi(BaseOpenApiBuilder baseOpenApiBuilder) {
 *         return baseOpenApiBuilder.build("title", "service-url-prefix");
 *     }
 * </pre>
 */
@Configuration
@PropertySource("classpath:swagger-config.properties")
public class SwaggerAutoConfiguration {

    @Value("${activiti.cloud.swagger.base-path:/}")
    private String swaggerBasePath;

    @Bean
    @ConditionalOnMissingBean
    @DependsOn("swaggerOAuthFlow")
    public BaseOpenApiBuilder baseOpenApiBuilder(BuildProperties buildProperties, OAuthFlow swaggerOAuthFlow) {
        return new BaseOpenApiBuilder(buildProperties, swaggerOAuthFlow);
    }

    @Bean
    @ConditionalOnMissingBean
    public PathPrefixOpenApiCustomizer pathPrefixCustomizer() {
        return new PathPrefixOpenApiCustomizer(swaggerBasePath);
    }

    @Bean
    @ConditionalOnMissingBean
    public ErrorResponsesOperationCustomizer errorResponsesOperationCustomizer() {
        return new ErrorResponsesOperationCustomizer();
    }

    @Bean
    @ConditionalOnMissingBean
    public SummaryOperationCustomizer summaryOperationCustomizer() {
        return new SummaryOperationCustomizer();
    }

    @Bean
    @ConditionalOnMissingBean
    public SecurityItemOperationCustomizer securityItemOperationCustomizer() {
        return new SecurityItemOperationCustomizer();
    }

    @Bean
    @ConditionalOnMissingBean
    public SchemaTitleOpenApiCustomizer schemaTitleOpenApiCustomizer() {
        return new SchemaTitleOpenApiCustomizer();
    }

    @Bean
    @ConditionalOnMissingBean
    public NamingOperationCustomizer namingOperationCustomizer() {
        return new NamingOperationCustomizer();
    }
}
