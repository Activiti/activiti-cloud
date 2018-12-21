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

import org.activiti.cloud.organization.api.Extensions;
import org.activiti.cloud.organization.converter.JsonConverter;
import org.activiti.cloud.organization.core.rest.client.model.ModelReference;
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

    private static JsonConverter<Extensions> extensionsConverter;

    @Autowired
    public void setApplicationEventPublisher(ModelReferenceService modelReferenceService,
                                             JsonConverter<Extensions> extensionsConverter) {
        this.modelReferenceService = modelReferenceService;
        this.extensionsConverter = extensionsConverter;
    }

    public static Page<ModelEntity> loadModelReference(Page<ModelEntity> models) {
        models.getContent().forEach(ModelEntityHandler::loadModelReference);
        return models;
    }

    public static ModelEntity loadModelReference(ModelEntity model) {
        model.setData(modelReferenceService.getResource(model.getType(),
                                                        model.getId()));
        return model;
    }

    public static ModelEntity loadFullModelReference(ModelEntity model) {
        return setModelData(model,
                            modelReferenceService.getResource(model.getType(),
                                                              model.getId()));
    }

    public static void createModelReference(ModelEntity model) {
        modelReferenceService.createResource(model.getType(),
                                             getModelData(model));
    }

    public static void updateModelReference(ModelEntity model) {
        modelReferenceService.updateResource(model.getType(),
                                             model.getId(),
                                             getModelData(model));
    }

    public static void deleteModelReference(ModelEntity model) {
        modelReferenceService.deleteResource(model.getType(),
                                             model.getId());
    }

    private static ModelEntity setModelData(ModelEntity model,
                                            ModelReference data) {
        model.setData(data);
        if (data != null) {
            model.setExtensions(extensionsConverter.convertToEntity(data.getExtensions()));
        }
        return model;
    }

    private static ModelReference getModelData(ModelEntity model) {
        ModelReference data = model.getData();
        if (data != null) {
            data.setExtensions(extensionsConverter.convertToJson(model.getExtensions()));
        }
        return data;
    }
}
