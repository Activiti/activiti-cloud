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

package org.activiti.cloud.services.rest.conf;

import java.util.List;

import org.activiti.cloud.services.rest.controllers.ResourcesAssembler;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.hateoas.mvc.TypeConstrainedMappingJackson2HttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@AutoConfigureAfter(WebMvcAutoConfiguration.class)
public class ServicesRestAutoConfiguration implements WebMvcConfigurer {

    private final Jackson2ObjectMapperBuilder objectMapperBuilder;

    public ServicesRestAutoConfiguration(Jackson2ObjectMapperBuilder objectMapperBuilder) {
        this.objectMapperBuilder = objectMapperBuilder;
    }

    @Bean
    public ResourcesAssembler resourcesAssembler() {
        return new ResourcesAssembler();
    }

    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        // for some, not yet identified, reason the ObjectMapper used by MappingJackson2HttpMessageConverter
        // does not contains the object mapper customizations provided by custom Module beans.
        // need to call configure here to ensure that the customisations are registered
        for (HttpMessageConverter<?> converter : converters) {
            //should exclude TypeConstrainedMappingJackson2HttpMessageConverter from configuration
            if(converter instanceof MappingJackson2HttpMessageConverter && !(converter instanceof TypeConstrainedMappingJackson2HttpMessageConverter)) {
                objectMapperBuilder.configure(((MappingJackson2HttpMessageConverter) converter).getObjectMapper());
            }
        }

    }
}
