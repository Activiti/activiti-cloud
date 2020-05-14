/*
 * Copyright 2017-2020 Alfresco Software, Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.activiti.cloud.starter.modeling.configuration;

import java.util.function.Predicate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.RequestHandler;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;

@Configuration
public class ModelingSwaggerConfig {

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

    @Bean
    @ConditionalOnMissingBean
    public Predicate<RequestHandler> apiSelector() {
        return RequestHandlerSelectors.basePackage("org.activiti.cloud.services.modeling.rest")::apply;
    }

}

