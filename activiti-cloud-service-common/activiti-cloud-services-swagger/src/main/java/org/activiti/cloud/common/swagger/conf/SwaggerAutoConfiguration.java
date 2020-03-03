/*
 * Copyright 2019 Alfresco, Inc. and/or its affiliates.
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

package org.activiti.cloud.common.swagger.conf;

import com.fasterxml.classmate.TypeResolver;
import java.util.List;
import java.util.function.Predicate;
import org.activiti.cloud.common.swagger.DocketCustomizer;
import org.activiti.cloud.common.swagger.SwaggerDocketBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.RequestHandler;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

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
 *         return RequestHandlerSelectors.basePackage("org.activiti.cloud.services")::apply;
 *     }
 *  </pre>
 *
 *  Also you need to have spring-boot-maven-plugin for {@link BuildProperties}.
 */
@Configuration
@EnableSwagger2
public class SwaggerAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public SwaggerDocketBuilder swaggerDocketBuilder(Predicate<RequestHandler> apiSelector,
                                                     TypeResolver typeResolver,
                                                     @Autowired(required = false) List<DocketCustomizer> docketCustomizers,
                                                     ApiInfo apiInfo) {
        return new SwaggerDocketBuilder(apiSelector,
                                        typeResolver,
                                        docketCustomizers,
                                        apiInfo);
    }

    @Bean
    @ConditionalOnMissingBean
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

    @Bean(name = "alfrescoAPIDocket")
    @ConditionalOnMissingBean(name = "alfrescoAPIDocket")
    public Docket alfrescoAPIDocket(SwaggerDocketBuilder swaggerDocketBuilder) {
        return swaggerDocketBuilder.buildAlfrescoAPIDocket();
    }

}
