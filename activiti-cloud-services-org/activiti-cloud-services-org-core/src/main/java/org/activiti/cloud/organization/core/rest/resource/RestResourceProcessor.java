/*
 * Copyright 2017 Alfresco, Inc. and/or its affiliates.
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

package org.activiti.cloud.organization.core.rest.resource;

import java.lang.reflect.Field;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceProcessor;

/**
 * Rest resource processor configuration.
 */
@Configuration
public class RestResourceProcessor {

    private RestResourceService restResourceService;

    @Autowired
    public RestResourceProcessor(final RestResourceService restResourceService) {
        this.restResourceService = restResourceService;
    }

    /**
     * Create the resource processor bean for processing entities with rest resources.
     * @param <T> the entity type
     * @return the processed resource
     */
    @Bean
    public <T> ResourceProcessor<Resource<T>> resourceProcessor() {
        return new ResourceProcessor<Resource<T>>() {
            @Override
            public Resource<T> process(Resource<T> resource) {
                Class<?> entityType = resource.getContent().getClass();
                if (entityType.isAnnotationPresent(EntityWithRestResource.class)) {
                    for (Field field : entityType.getDeclaredFields()) {
                        RestResource restResource = field.getAnnotation(RestResource.class);
                        if (restResource != null) {
                            restResourceService
                                    .processResourceWithRestResource(resource,
                                                                     field.getName(),
                                                                     restResource);
                        }
                    }
                }
                return resource;
            }
        };
    }
}
