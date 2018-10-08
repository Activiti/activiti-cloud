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

package org.activiti.cloud.services.organization.service;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.activiti.cloud.organization.api.Application;
import org.activiti.cloud.organization.api.Model;
import org.activiti.cloud.organization.api.ModelType;
import org.activiti.cloud.organization.api.ModelValidator;
import org.activiti.cloud.organization.core.error.UnknownModelTypeException;
import org.activiti.cloud.organization.repository.ModelRepository;
import org.activiti.cloud.services.common.file.FileContent;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

/**
 * Business logic related to {@link Model} entities
 * including process models, form models, connectors, data models and decision table models.
 */
@Service
@PreAuthorize("hasRole('ACTIVITI_MODELER')")
public class ModelService {

    private final ModelRepository modelRepository;

    private final ModelTypeService modelTypeService;

    private Map<String, ModelValidator> modelValidatorsMapByModelType;

    @Autowired
    public ModelService(ModelRepository modelRepository,
                        ModelTypeService modelTypeService,
                        Set<ModelValidator> modelValidators) {
        this.modelRepository = modelRepository;
        this.modelTypeService = modelTypeService;

        this.modelValidatorsMapByModelType = modelValidators
                .stream()
                .collect(Collectors.toMap((modelValidationService) -> modelValidationService.getHandledModelType().getName(),
                                          Function.identity()));
    }

    public Set<String> buildModelTypesFilter(Optional<ModelType> modelType) {
        return modelType
                .map(Stream::of)
                .orElseGet(() -> modelTypeService.getAvailableModelTypes().stream())
                .map(ModelType::getName)
                .collect(Collectors.toSet());
    }

    public Page<Model> getTopLevelModels(Optional<ModelType> modelType,
                                         Pageable pageable) {
        return modelRepository.getTopLevelModels(buildModelTypesFilter(modelType),
                                                 pageable);
    }

    public Page<Model> getModels(Application application,
                                 Optional<ModelType> modelType,
                                 Pageable pageable) {
        return modelRepository.getModels(application,
                                         buildModelTypesFilter(modelType),
                                         pageable);
    }

    public Model createModel(Application application,
                             Model model) {
        model.setApplication(application);
        Optional.ofNullable(model.getType())
                .flatMap(modelTypeService::findModelTypeByName)
                .orElseThrow(() -> new UnknownModelTypeException("Unknown model type: " + model.getType()));
        return modelRepository.createModel(model);
    }

    public Model updateModel(Model modelToBeUpdated,
                             Model newModel) {
        modelToBeUpdated.setName(newModel.getName());
        return modelRepository.updateModel(modelToBeUpdated);
    }

    public void deleteModel(Model model) {
        modelRepository.deleteModel(model);
    }

    public Optional<Model> findModelById(String modelId) {
        return modelRepository.findModelById(modelId);
    }

    public Model newModelInstance(String type,
                                  String name) {
        try {
            Model model = (Model) modelRepository.getModelType().newInstance();
            model.setId(UUID.randomUUID().toString());
            model.setType(type);
            model.setName(name);
            return model;
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public Optional<FileContent> getModelContent(String modelId) {
        Optional<Model> optionalModel = modelRepository.findModelById(modelId);
        return optionalModel
                .filter(model -> StringUtils.isNotEmpty(model.getContent()))
                .map(model -> new FileContent(model.getName(),
                                              model.getContentType(),
                                              model.getContent().getBytes()));
    }

    public void updateModelContent(Model modelToBeUpdate,
                                   FileContent fileContent) throws IOException {
        modelToBeUpdate.setContentType(fileContent.getContentType());
        modelToBeUpdate.setContent(new String(fileContent.getFileContent()));

        modelRepository.updateModel(modelToBeUpdate);
    }

    public Model importModel(Application application,
                             ModelType modelType,
                             FileContent fileContent) {

        Model model = newModelInstance(modelType.getName(),
                                       fileContent.getFilename());
        model.setContentType(fileContent.getContentType());
        model.setContent(new String(fileContent.getFileContent()));

        return createModel(application,
                           model);
    }

    public Optional<FileContent> exportModel(String modelId) {
        //TODO: to export all model information+content in a file
        return getModelContent(modelId);
    }

    public Optional<FileContent> getModelDiagram(String modelId) {
        //TODO: to implement
        return Optional.empty();
    }

    public void validateModelContent(Model model,
                                     FileContent fileContent) {
        Optional.ofNullable(model.getType())
                .map(modelValidatorsMapByModelType::get)
                .ifPresent(modelValidator -> modelValidator.validateModelContent(fileContent.getFileContent()));
    }
}