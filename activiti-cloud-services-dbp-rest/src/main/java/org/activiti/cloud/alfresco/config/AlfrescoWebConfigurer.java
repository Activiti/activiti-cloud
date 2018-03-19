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

package org.activiti.cloud.alfresco.config;

import java.util.ArrayList;
import java.util.List;

import org.activiti.cloud.alfresco.argument.resolver.AlfrescoPageArgumentMethodResolver;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.hateoas.mvc.TypeConstrainedMappingJackson2HttpMessageConverter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@PropertySource("classpath:config/alfresco-rest-config.properties")
public class AlfrescoWebConfigurer implements WebMvcConfigurer {

    private final AlfrescoPageArgumentMethodResolver alfrescoPageArgumentMethodResolver;

    public AlfrescoWebConfigurer(AlfrescoPageArgumentMethodResolver alfrescoPageArgumentMethodResolver) {
        this.alfrescoPageArgumentMethodResolver = alfrescoPageArgumentMethodResolver;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(0, alfrescoPageArgumentMethodResolver);
    }

    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        //the property spring.hateoas.use-hal-as-default-json-media-type is not working
        //we need to manually remove application/json from supported mediaTypes
        for (HttpMessageConverter<?> converter : converters) {
            if (converter instanceof TypeConstrainedMappingJackson2HttpMessageConverter ) {
                ArrayList<MediaType> mediaTypes = new ArrayList<>(converter.getSupportedMediaTypes());
                mediaTypes.remove(MediaType.APPLICATION_JSON);
                ((TypeConstrainedMappingJackson2HttpMessageConverter) converter).setSupportedMediaTypes(mediaTypes);
            }
        }

    }
}
