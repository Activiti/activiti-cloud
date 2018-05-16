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

import org.activiti.cloud.organization.core.model.Model.ModelType;
import org.activiti.cloud.organization.core.model.ModelReference;
import org.activiti.cloud.organization.core.rest.client.ModelService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import static org.activiti.cloud.organization.core.util.ReflectionUtils.getFieldValue;
import static org.activiti.cloud.organization.core.util.ReflectionUtils.setFieldValue;

/**
 * Processing rest resources logic.
 */
@Service
public class RestResourceService {

    private static Logger log = LoggerFactory.getLogger(RestResourceService.class);

    private final ModelService modelService;

    @Autowired
    RestResourceService(ModelService modelService) {
        this.modelService = modelService;
    }

    /**
     * Process an entity with a rest resource.
     * It loads the rest resource into the annotated field of the entity
     * and adds the link to the resource, if needed.
     * @param resource the resource to process
     * @param fieldName the entity field name associated with the rest resource
     * @param restResource the {@link RestResource} annotation of the resource
     * @param <T> the type of the entity
     */
    protected <T> void processResourceWithRestResource(final Resource<T> resource,
                                                       String fieldName,
                                                       RestResource restResource) {

        T entity = resource.getContent();
        log.trace("Processing entity with rest resource: " + entity);

        ModelType modelType = (ModelType) getFieldValue(entity,
                                                        restResource.resourceKeyField());
        String modelId = (String) getFieldValue(entity,
                                                restResource.resourceIdField());

        final Object resolvedResource;
        try {
            resolvedResource = modelService.getResource(modelType,
                                                        modelId);
        } catch (Exception ex) {
            // just log the error, don't break the processing entity mechanism
            log.error(String.format("Failed to fetch resource of type '%s' with id '%s'",
                                    modelType,
                                    modelId),
                      ex);
            return;
        }

        String targetFieldName = !StringUtils.isEmpty(restResource.targetField()) ?
                restResource.targetField() :
                fieldName;

        setFieldValue(
                entity,
                targetFieldName,
                resolvedResource);
    }

    /**
     * Perform write rest operation corresponding
     * to a save operation on an entity containing a rest resource.
     * @param entity the entity to be saved
     * @param fieldName the entity field name associated with the rest resource
     * @param restResource the {@link RestResource} annotation of the resource
     * @param update true is the save is an update
     */
    public void saveRestResourceFromEntityField(Object entity,
                                                String fieldName,
                                                RestResource restResource,
                                                boolean update) {

        log.trace("Handling saving entity with rest resource: " + entity);

        String targetFieldName = !StringUtils.isEmpty(restResource.targetField()) ?
                restResource.targetField() :
                fieldName;

        ModelReference model = (ModelReference) getFieldValue(entity,
                                                              targetFieldName);
        if (model == null) {
            log.debug(String.format(
                    "No data found in field '%s' of entity type '%s'",
                    targetFieldName,
                    entity.getClass()));
        }

        ModelType modelType = (ModelType) getFieldValue(entity,
                                                        restResource.resourceKeyField());

        if (update) {
            String modelId = (String) getFieldValue(entity,
                                                    restResource.resourceIdField());
            modelService.updateResource(modelType,
                                        modelId,
                                        model);
        } else {
            modelService.createResource(modelType,
                                        model);
        }
    }
}
