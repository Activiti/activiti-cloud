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

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.activiti.cloud.organization.api.Model;
import org.activiti.cloud.organization.api.ModelType;
import org.activiti.cloud.organization.api.ModelValidator;
import org.activiti.cloud.organization.api.Project;
import org.activiti.cloud.organization.converter.JsonConverter;
import org.activiti.cloud.organization.core.error.ImportModelException;
import org.activiti.cloud.organization.core.error.UnknownModelTypeException;
import org.activiti.cloud.organization.repository.ModelRepository;
import org.activiti.cloud.services.common.file.FileContent;
import org.activiti.cloud.services.common.util.ContentTypeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import static org.activiti.cloud.services.common.util.ContentTypeUtils.JSON;
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

    private final Map<String, ModelValidator> modelValidatorsMapByModelType;

    private final JsonConverter<Model> jsonConverter;

    @Autowired
    public ModelService(ModelRepository modelRepository,
                        ModelTypeService modelTypeService,
                        Set<ModelValidator> modelValidators,
                        JsonConverter<Model> jsonConverter) {
        this.modelRepository = modelRepository;
        this.modelTypeService = modelTypeService;
        this.jsonConverter = jsonConverter;

        this.modelValidatorsMapByModelType = modelValidators
                .stream()
                .collect(Collectors.toMap((modelValidationService) -> modelValidationService.getHandledModelType().getName(),
                                          Function.identity()));
    }

    public List<Model> getAllModels(Project project) {
        return modelTypeService.getAvailableModelTypes()
                .stream()
                .map(modelType -> getModels(project,
                                            modelType,
                                            Pageable.unpaged()))
                .map(Page::getContent)
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }

    public Page<Model> getModels(Project project,
                                 ModelType modelType,
                                 Pageable pageable) {
        return modelRepository.getModels(project,
                                         modelType,
                                         pageable);
    }

    public Model createModel(Project project,
                             Model model) {
        findModelType(model);
        model.setProject(project);
        return modelRepository.createModel(model);
    }

    public Model updateModel(Model modelToBeUpdated,
                             Model newModel) {
        return modelRepository.updateModel(modelToBeUpdated,
                                           newModel);
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
            model.setType(type);
            model.setName(name);
            return model;
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public FileContent getModelMetadataFileContent(Model model) {
        Model modelWithFullMetadata = findModelById(model.getId()).orElse(model);
        return new FileContent(toJsonFilename(model.getName()),
                               ContentTypeUtils.CONTENT_TYPE_JSON,
                               jsonConverter.convertToJsonBytes(modelWithFullMetadata));
    }

    public FileContent getModelContentFile(Model model) {
        return getModelFileContent(model,
                                   modelRepository.getModelContent(model));
    }

    public FileContent exportModel(Model model) {
        return getModelFileContent(model,
                                   modelRepository.getModelExport(model));
    }

    private FileContent getModelFileContent(Model model,
                                            byte[] modelBytes) {
        return new FileContent(setExtension(model.getName(),
                                            findModelType(model).getContentFileExtension()),
                               model.getContentType(),
                               modelBytes);
    }

    public Optional<FileContent> getModelDiagramFile(String modelId) {
        //TODO: to implement
        return Optional.empty();
    }

    public Model updateModelContent(Model modelToBeUpdate,
                                    FileContent fileContent) {
        modelToBeUpdate.setContentType(fileContent.getContentType());
        modelToBeUpdate.setContent(fileContent.toString());
        return modelRepository.updateModelContent(modelToBeUpdate,
                                                  fileContent);
    }

    public Model importModel(Project project,
                             ModelType modelType,
                             FileContent fileContent) {
        logger.debug(MessageFormat.format(
                "Importing model type {0} from file {1}: {2}",
                modelType,
                fileContent.getFilename(),
                fileContent));

        Model model = importModelFromContent(project,
                                             modelType,
                                             fileContent);

        return updateModelContent(model,
                                  fileContent);
    }

    public Model importJsonModel(Project project,
                                 ModelType modelType,
                                 FileContent fileContent) {
        Model model = jsonConverter.tryConvertToEntity((fileContent.getFileContent()))
                .orElseThrow(() -> new ImportModelException("Cannot convert json file content to model: " + fileContent));
        model.setName(removeExtension(fileContent.getFilename(),
                                      JSON));
        model.setType(modelType.getName());

        return createModel(project,
                           model);
    }

    public Model importModelFromContent(Project project,
                                        ModelType modelType,
                                        FileContent fileContent) {
        return contentFilenameToModelName(fileContent.getFilename(),
                                          modelType)
                .map(modelName -> newModelInstance(modelType.getName(),
                                                   modelName))
                .map(model -> createModel(project,
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

    public Optional<ModelValidator> findModelValidatorByModelType(String modelType) {
        return Optional.ofNullable(modelValidatorsMapByModelType.get(modelType));
    }

    public void validateModelContent(Model model) {
        validateModelContent(model.getType(),
                             modelRepository.getModelContent(model));
    }

    public void validateModelContent(Model model,
                                     FileContent fileContent) {
        validateModelContent(model.getType(),
                             fileContent.getFileContent());
    }

    private void validateModelContent(String modelType,
                                      byte[] modelContent) {
        findModelValidatorByModelType(modelType)
                .ifPresent(modelValidator -> modelValidator.validateModelContent(modelContent));
    }

    private ModelType findModelType(Model model) {
        return Optional.ofNullable(model.getType())
                .flatMap(modelTypeService::findModelTypeByName)
                .orElseThrow(() -> new UnknownModelTypeException("Unknown model type: " + model.getType()));
    }
}