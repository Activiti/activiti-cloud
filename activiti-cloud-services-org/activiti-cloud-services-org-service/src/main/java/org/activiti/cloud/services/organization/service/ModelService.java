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
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.activiti.cloud.organization.api.Application;
import org.activiti.cloud.organization.api.Model;
import org.activiti.cloud.organization.api.ModelType;
import org.activiti.cloud.organization.api.ModelValidator;
import org.activiti.cloud.organization.core.error.ImportModelException;
import org.activiti.cloud.organization.core.error.ModelingException;
import org.activiti.cloud.organization.core.error.UnknownModelTypeException;
import org.activiti.cloud.organization.repository.ModelRepository;
import org.activiti.cloud.services.common.file.FileContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import static org.activiti.cloud.services.common.util.ContentTypeUtils.removeExtension;
import static org.activiti.cloud.services.common.util.ContentTypeUtils.setExtension;
import static org.activiti.cloud.services.common.util.ContentTypeUtils.toJsonFilename;

/**
 * Business logic related to {@link Model} entities
 * including process models, form models, connectors, data models and decision table models.
 */
@Service
@PreAuthorize("hasRole('ACTIVITI_MODELER')")
public class ModelService {

    private static final Logger logger = LoggerFactory.getLogger(ModelService.class);

    private final ModelRepository modelRepository;

    private final ModelTypeService modelTypeService;

    private final ObjectMapper jsonMapper;

    private final Map<String, ModelValidator> modelValidatorsMapByModelType;

    @Autowired
    public ModelService(ModelRepository modelRepository,
                        ModelTypeService modelTypeService,
                        ObjectMapper jsonMapper,
                        Set<ModelValidator> modelValidators) {
        this.modelRepository = modelRepository;
        this.modelTypeService = modelTypeService;
        this.jsonMapper = jsonMapper;

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

    public List<Model> getAllModels(Application application) {
        return getModels(application,
                         Optional.empty(),
                         Pageable.unpaged())
                .getContent();
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
        findModelType(model);
        model.setApplication(application);
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

    public FileContent getModelContent(Model model) {
        return new FileContent(setExtension(model.getName(),
                                            findModelType(model).getContentFileExtension()),
                               model.getContentType(),
                               modelRepository.getModelContent(model));
    }

    public FileContent getModelJson(Model model) {
        return new FileContent(toJsonFilename(model.getName()),
                               model.getContentType(),
                               toJson(model));
    }

    public Optional<FileContent> getModelDiagram(String modelId) {
        //TODO: to implement
        return Optional.empty();
    }

    public Model updateModelContent(Model modelToBeUpdate,
                                    FileContent fileContent) {
        modelToBeUpdate.setContentType(fileContent.getContentType());
        modelToBeUpdate.setContent(fileContent.toString());

        return modelRepository.updateModel(modelToBeUpdate);
    }

    public Model importModel(Application application,
                             ModelType modelType,
                             FileContent fileContent) {
        logger.debug(MessageFormat.format(
                "Importing model type {0} from file {1}: {2}",
                modelType,
                fileContent.getFilename(),
                fileContent));

        if (fileContent.isJson()) {
            return importJsonModel(application,
                                   modelType,
                                   fileContent);
        }

        Model model = importModelFromContent(application,
                                             modelType,
                                             fileContent);

        return updateModelContent(model,
                                  fileContent);
    }

    public Model importJsonModel(Application application,
                                 ModelType modelType,
                                 FileContent fileContent) {
        try {
            Model model = jsonMapper.readValue(fileContent.getFileContent(),
                                               Model.class);
            if (!modelType.getName().equals(model.getType())) {
                throw new ImportModelException(MessageFormat.format(
                        "Expected model type {0} was found actually as {1} in the file to import: {2}",
                        modelType.getName(),
                        model.getType(),
                        fileContent.getFilename()));
            }

            return createModel(application,
                               model);
        } catch (IOException e) {
            throw new ImportModelException("Cannot convert json file content to model: " + fileContent);
        }
    }

    public Model importModelFromContent(Application application,
                                        ModelType modelType,
                                        FileContent fileContent) {
        return contentFilenameToModelName(fileContent.getFilename(),
                                          modelType)
                .map(modelName -> newModelInstance(modelType.getName(),
                                                   modelName))
                .map(model -> createModel(application,
                                          model))
                .orElseThrow(() -> new ImportModelException(MessageFormat.format(
                        "Unexpected extension was found for file to import model of type {0}: {1}",
                        modelType.getName(),
                        fileContent.getFilename())));
    }

    public Optional<String> contentFilenameToModelName(String filename,
                                                       ModelType modelType) {
        return Arrays.stream(modelType.getAllowedContentFileExtension())
                .filter(filename::endsWith)
                .findFirst()
                .map(extension -> removeExtension(filename,
                                                  extension));
    }

    public FileContent exportModel(Model model) {
        return getModelContent(model);
    }

    public void validateModelContent(Model model,
                                     FileContent fileContent) {
        Optional.ofNullable(model.getType())
                .map(modelValidatorsMapByModelType::get)
                .ifPresent(modelValidator -> modelValidator.validateModelContent(fileContent.getFileContent()));
    }

    private ModelType findModelType(Model model) {
        return Optional.ofNullable(model.getType())
                .flatMap(modelTypeService::findModelTypeByName)
                .orElseThrow(() -> new UnknownModelTypeException("Unknown model type: " + model.getType()));
    }

    private byte[] toJson(Model model) {
        try {
            return jsonMapper.writeValueAsBytes(model);
        } catch (JsonProcessingException e) {
            throw new ModelingException("Cannot convert model metadata to json: " + model.getId(),
                                        e);
        }
    }
}