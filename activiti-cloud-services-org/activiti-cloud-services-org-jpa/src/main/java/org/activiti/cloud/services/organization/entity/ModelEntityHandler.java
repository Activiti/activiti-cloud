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

package org.activiti.cloud.services.organization.entity;

import java.util.List;

import org.activiti.cloud.organization.api.ModelValidationError;
import org.activiti.cloud.organization.core.rest.client.service.ModelReferenceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

/**
 * Handler for model entities.
 */
@Component
public class ModelEntityHandler {

    private static ModelReferenceService modelReferenceService;

    @Autowired
    public void setApplicationEventPublisher(ModelReferenceService modelReferenceService) {
        this.modelReferenceService = modelReferenceService;
    }

    public static Page<ModelEntity> loadModelReference(Page<ModelEntity> models) {
        models.getContent()
                .stream()
                .forEach(ModelEntityHandler::loadModelReference);
        return models;
    }

    public static ModelEntity loadModelReference(ModelEntity model) {
        model.setData(modelReferenceService.getResource(model.getType(),
                                                        model.getId()));
        return model;
    }

    public static void createModelReference(ModelEntity model) {
        modelReferenceService.createResource(model.getType(),
                                             model.getData());
    }

    public static void updateModelReference(ModelEntity model) {
        modelReferenceService.updateResource(model.getType(),
                                             model.getId(),
                                             model.getData());
    }

    public static void deleteModelReference(ModelEntity model) {
        modelReferenceService.deleteResource(model.getType(),
                                             model.getId());
    }

    public static List<ModelValidationError> validateModelReference(ModelEntity model,
                                                                    byte[] content) {
        return modelReferenceService.validateResourceContent(model.getType(),
                                                             content);
    }
}
