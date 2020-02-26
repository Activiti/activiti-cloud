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

package org.activiti.cloud.services.modeling.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.activiti.cloud.modeling.api.Project;
import org.activiti.cloud.modeling.api.process.Extensions;

import java.util.Map;

import org.activiti.cloud.modeling.api.Model;
import org.activiti.cloud.modeling.converter.JsonConverter;
import org.activiti.cloud.services.modeling.converter.ConnectorModelContent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Json converter configuration
 */
@Configuration
public class JsonConverterConfiguration {

    @Bean
    public JsonConverter<Project> projectJsonConverter(ObjectMapper objectMapper) {
        return new JsonConverter<>(Project.class,
                                   objectMapper);
    }

    @Bean
    public JsonConverter<ProjectDescriptor> projectDescriptorJsonConverter(ObjectMapper objectMapper) {
        return new JsonConverter<>(ProjectDescriptor.class,
                objectMapper);
    }

    @Bean
    public JsonConverter<Model> modelJsonConverter(ObjectMapper objectMapper) {
        return new JsonConverter<>(Model.class,
                                   objectMapper);
    }

    @Bean
    public JsonConverter<ConnectorModelContent> connectorModelContentJsonConverter(ObjectMapper objectMapper) {
        return new JsonConverter<>(ConnectorModelContent.class,
                                   objectMapper);
    }

    @Bean
    public JsonConverter<Map> jsonMetadataConverter(ObjectMapper objectMapper) {
        return new JsonConverter<>(Map.class,
                                   objectMapper);
    }

    @Bean
    public JsonConverter<Extensions> jsonExtensionsConverter(ObjectMapper objectMapper) {
        return new JsonConverter<>(Extensions.class,
                                   objectMapper);
    }
}
