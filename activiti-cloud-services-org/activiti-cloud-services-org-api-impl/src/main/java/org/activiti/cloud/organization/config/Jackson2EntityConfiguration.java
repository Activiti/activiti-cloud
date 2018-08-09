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

package org.activiti.cloud.organization.config;

import com.fasterxml.jackson.databind.JsonDeserializer;
import org.activiti.cloud.organization.api.Application;
import org.activiti.cloud.organization.api.Model;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Entity jackson configuration
 */
@Configuration
public class Jackson2EntityConfiguration {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer addApplicationDeserializer(
            @Qualifier("applicationDeserializer") JsonDeserializer<Application> applicationDeserializer) {
        return builder -> builder.deserializerByType(Application.class,
                                                     applicationDeserializer);
    }

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer addModelDeserializer(
            @Qualifier("modelDeserializer") JsonDeserializer<Model> modelDeserializer) {
        return builder -> builder.deserializerByType(Model.class,
                                                     modelDeserializer);
    }

    @Bean("applicationDeserializer")
    @ConditionalOnMissingBean(name = "applicationDeserializer")
    public JsonDeserializer<Application> applicationDeserializer() {
        return new ApplicationDeserializer();
    }

    @Bean("modelDeserializer")
    @ConditionalOnMissingBean(name = "modelDeserializer")
    public JsonDeserializer<Model> modelDeserializer() {
        return new ModelDeserializer();
    }
}
