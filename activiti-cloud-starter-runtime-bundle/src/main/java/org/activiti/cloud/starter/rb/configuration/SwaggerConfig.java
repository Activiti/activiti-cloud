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

package org.activiti.cloud.starter.rb.configuration;

import java.util.function.Predicate;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.RequestHandler;
import springfox.documentation.builders.RequestHandlerSelectors;

@Configuration
public class SwaggerConfig {

    @Bean
    @ConditionalOnMissingBean
    public Predicate<RequestHandler> apiSelector() {

        return RequestHandlerSelectors.basePackage("org.activiti.cloud.services")::apply;
    }

    @Bean
    @ConditionalOnMissingBean
    public PayloadsDocketCustomizer payloadsDocketCustomizer(){
        return new PayloadsDocketCustomizer();
    }


}