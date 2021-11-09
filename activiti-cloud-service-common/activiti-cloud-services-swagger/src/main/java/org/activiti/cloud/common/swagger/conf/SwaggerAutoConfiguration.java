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
package org.activiti.cloud.common.swagger.conf;

import java.util.function.Predicate;
import org.activiti.cloud.common.swagger.BaseAPIInfoBuilder;
import org.activiti.cloud.common.swagger.PathPrefixTransformationFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.RequestHandler;
import springfox.documentation.oas.annotations.EnableOpenApi;
import springfox.documentation.spring.web.plugins.Docket;

/**
 * Provides base springfox configuration for swagger auto-generated specification file. It provides two
 * swagger specification files: the default one is available under `v2/api-docs` or `v2/api-docs?group=default`
 * and provides specification for Alfresco MediaType format
 *
 * This configuration is not self-contained: the one adding this as dependency should provide a bean of type
 * {@link Predicate<RequestHandler>} that will be injected under {@link Docket#select()}. I.e
 * <code>test</code>
 * {@code test}
 * <pre>
 *     &#64;Bean
 *     public Predicate&#60;RequestHandler&#62; apiSelector() {
 *         return RequestHandlerSelectors.basePackage("org.activiti.cloud.services");
 *     }
 *  </pre>
 *
 */
@Configuration
@EnableOpenApi
public class SwaggerAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public BaseAPIInfoBuilder baseAPIInfoBuilder(BuildProperties buildProperties) {
        return new BaseAPIInfoBuilder(buildProperties);
    }

    @Bean
    @ConditionalOnMissingBean
    public PathPrefixTransformationFilter pathPrefixTransformationFilter(@Value("${activiti.cloud.swagger.base-path:/}") String swaggerBasePath) {
        return new PathPrefixTransformationFilter(swaggerBasePath);
    }

}
