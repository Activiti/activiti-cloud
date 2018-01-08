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
import org.springframework.data.rest.core.annotation.HandleBeforeCreate;
import org.springframework.data.rest.core.annotation.HandleBeforeSave;
import org.springframework.data.rest.core.annotation.RepositoryEventHandler;
import org.springframework.stereotype.Component;

/**
 * Rest resource events handler.
 * It is used for handling creation and update of entities containing rest resources.
 */
@Component
@RepositoryEventHandler
public class RestResourceEventHandler {

    private RestResourceService restResourceService;

    @Autowired
    public RestResourceEventHandler(final RestResourceService restResourceService) {
        this.restResourceService = restResourceService;
    }

    /**
     * Handler for pre-create entity event.
     * @param entity the entity to be created
     */
    @HandleBeforeCreate
    public void handleBeforeCreates(Object entity) {
        handleBeforeSave(entity,
                         false);
    }

    /**
     * Handler for pre-update entity event.
     * @param entity the entity to be updated
     */
    @HandleBeforeSave
    public void handleBeforeUpdate(Object entity) {
        handleBeforeSave(entity,
                         true);
    }

    /**
     * Handler for sace entity event
     * @param entity the entity to be saved
     * @param update true if the event is an update one
     */
    protected void handleBeforeSave(Object entity,
                                    boolean update) {
        Class<?> entityType = entity.getClass();
        if (entityType.isAnnotationPresent(EntityWithRestResource.class)) {
            for (Field field : entityType.getDeclaredFields()) {
                RestResource restResource = field.getAnnotation(RestResource.class);
                if (restResource != null) {
                    restResourceService
                            .handleSaveOnEntityWithRestResource(entity,
                                                                field.getName(),
                                                                restResource,
                                                                update);
                }
            }
        }
    }
}
