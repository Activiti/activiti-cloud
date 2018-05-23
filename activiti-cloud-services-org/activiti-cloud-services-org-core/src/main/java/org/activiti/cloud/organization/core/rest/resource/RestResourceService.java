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

package org.activiti.cloud.organization.core.rest.resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.activiti.cloud.organization.core.util.ReflectionUtils.getFieldValue;
import static org.activiti.cloud.organization.core.util.ReflectionUtils.setFieldValue;

/**
 * Processing rest resources logic.
 */
public abstract class RestResourceService<T, K, I> {

    private static Logger log = LoggerFactory.getLogger(RestResourceService.class);

    /**
     * Process an entity with a rest resource.
     * It loads the rest resource into the annotated field of the entity.
     * @param entity the object to process
     * @param fieldName the entity field name associated with the rest resource
     * @param resourceKeyField the field name corresponding to the resource key
     * @param resourceIdField the field name corresponding to the resource id
     */
    protected void loadRestResourceIntoEntityField(Object entity,
                                                   String fieldName,
                                                   String resourceKeyField,
                                                   String resourceIdField) {

        log.trace("Processing entity with rest resource: " + entity);

        K resourceKey = getFieldValue(entity,
                                      resourceKeyField);
        I resourceId = getFieldValue(entity,
                                     resourceIdField);

        final T resolvedResource;
        try {
            resolvedResource = getResource(resourceKey,
                                           resourceId);
        } catch (Exception ex) {
            // just log the error, don't break the processing entity mechanism
            log.error(String.format("Failed to fetch resource of type '%s' with id '%s'",
                                    resourceKey,
                                    resourceId),
                      ex);
            return;
        }

        setFieldValue(
                entity,
                fieldName,
                resolvedResource);
    }

    /**
     * Perform write rest operation corresponding
     * to a save operation on an entity containing a rest resource.
     * @param entity the entity to be saved
     * @param fieldName the entity field name associated with the rest resource
     * @param resourceKeyField the field name corresponding to the resource key
     * @param resourceIdField the field name corresponding to the resource id
     * @param update true is the save is an update
     */
    public void saveRestResourceFromEntityField(Object entity,
                                                String fieldName,
                                                String resourceKeyField,
                                                String resourceIdField,
                                                boolean update) {

        log.trace("Handling saving entity with rest resource: " + entity);

        T resource = getFieldValue(entity,
                                   fieldName);
        if (resource == null) {
            log.debug(String.format(
                    "No data found in field '%s' of entity type '%s'",
                    fieldName,
                    entity.getClass()));
            return;
        }

        K resourceKey = getFieldValue(entity,
                                      resourceKeyField);

        if (update) {
            I resourceId = getFieldValue(entity,
                                         resourceIdField);
            updateResource(resourceKey,
                           resourceId,
                           resource);
        } else {
            createResource(resourceKey,
                           resource);
        }
    }

    protected abstract T getResource(K resourceKey,
                                     I resourceId);

    protected abstract void createResource(K resourceKey,
                                           T resource);

    protected abstract void updateResource(K resourceKey,
                                           I resourceId,
                                           T resource);
}
