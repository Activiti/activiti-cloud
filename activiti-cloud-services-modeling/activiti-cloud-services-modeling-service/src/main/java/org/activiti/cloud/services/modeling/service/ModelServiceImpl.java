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

package org.activiti.cloud.services.modeling.service;

import static java.util.Objects.nonNull;
import static org.activiti.cloud.modeling.api.ProcessModelType.PROCESS;
import static org.activiti.cloud.modeling.api.ValidationContext.EMPTY_CONTEXT;
import static org.activiti.cloud.services.common.util.ContentTypeUtils.CONTENT_TYPE_JSON;
import static org.activiti.cloud.services.common.util.ContentTypeUtils.JSON;
import static org.activiti.cloud.services.common.util.ContentTypeUtils.isJsonContentType;
import static org.activiti.cloud.services.common.util.ContentTypeUtils.removeExtension;
import static org.activiti.cloud.services.common.util.ContentTypeUtils.setExtension;
import static org.activiti.cloud.services.common.util.ContentTypeUtils.toJsonFilename;
import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;
import static org.apache.commons.lang3.StringUtils.removeEnd;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;

import javax.transaction.Transactional;
import javax.xml.stream.XMLStreamException;

import org.activiti.bpmn.exceptions.XMLException;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.Process;
import org.activiti.bpmn.model.Task;
import org.activiti.cloud.modeling.api.Model;
import org.activiti.cloud.modeling.api.ModelContent;
import org.activiti.cloud.modeling.api.ModelType;
import org.activiti.cloud.modeling.api.Project;
import org.activiti.cloud.modeling.api.ValidationContext;
import org.activiti.cloud.modeling.api.process.Extensions;
import org.activiti.cloud.modeling.converter.JsonConverter;
import org.activiti.cloud.modeling.core.error.ImportModelException;
import org.activiti.cloud.modeling.core.error.UnknownModelTypeException;
import org.activiti.cloud.modeling.repository.ModelRepository;
import org.activiti.cloud.services.common.file.FileContent;
import org.activiti.cloud.services.common.util.ContentTypeUtils;
import org.activiti.cloud.services.modeling.converter.ProcessModelContentConverter;
import org.activiti.cloud.services.modeling.service.api.ModelService;
import org.activiti.cloud.services.modeling.validation.ProjectValidationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.Assert;

/**
 * Business logic related to {@link Model} entities including process models, form models, connectors, data models and decision table models.
 */
@PreAuthorize("hasRole('ACTIVITI_MODELER')")
@Transactional
public class ModelServiceImpl implements ModelService{

    private static final Logger logger = LoggerFactory.getLogger(ModelServiceImpl.class);

    private final ModelRepository modelRepository;

    private final ModelTypeService modelTypeService;

    private final ModelContentService modelContentService;

    private final ModelExtensionsService modelExtensionsService;

    private final JsonConverter<Model> jsonConverter;

    private final ProcessModelContentConverter processModelContentConverter;

    private final HashMap<String, String> modelIdentifiers = new HashMap();

    @Autowired
    public ModelServiceImpl(ModelRepository modelRepository,
                            ModelTypeService modelTypeService,
                            ModelContentService modelContentService,
                            ModelExtensionsService modelExtensionsService,
                            JsonConverter<Model> jsonConverter,
                            ProcessModelContentConverter processModelContentConverter) {
        this.modelRepository = modelRepository;
        this.modelTypeService = modelTypeService;
        this.modelContentService = modelContentService;
        this.jsonConverter = jsonConverter;
        this.modelExtensionsService = modelExtensionsService;
        this.processModelContentConverter = processModelContentConverter;
    }

    @Override
    public List<Model> getAllModels(Project project) {
        return modelTypeService.getAvailableModelTypes().stream().map(modelType -> getModels(project,
                                                                                             modelType,
                                                                                             Pageable.unpaged()))
                .map(Page::getContent).flatMap(List::stream).collect(Collectors.toList());
    }

    @Override
    public Page<Model> getModels(Project project,
                                 ModelType modelType,
                                 Pageable pageable) {
        return modelRepository.getModels(project,
                                         modelType,
                                         pageable);
    }

    @Override
    public Model buildModel(String type,
                            String name) {
        try {
            Model model = (Model) modelRepository.getModelType().getConstructor().newInstance();
            model.setType(type);
            model.setName(name);
            return model;
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Model createModel(Project project,
                             Model model) {
        model.setId(null);
        ModelType modelType = findModelType(model);
        model.setProject(project);
        if (model.getExtensions() == null) {
          model.setExtensions(new HashMap<String,Object>());
        }
        return modelRepository.createModel(model);
    }

    @Override
    public Model updateModel(Model modelToBeUpdated,
                             Model newModel) {
        return modelRepository.updateModel(modelToBeUpdated,
                                           newModel);
    }

    @Override
    public void deleteModel(Model model) {
        modelRepository.deleteModel(model);
    }

    @Override
    public Optional<Model> findModelById(String modelId) {
        return modelRepository.findModelById(modelId);
    }

    @Override
    public Optional<FileContent> getModelExtensionsFileContent(Model model) {
        if (model.getExtensions() == null && isJsonContentType(model.getContentType())) {
            return Optional.empty();
        }

        Model fullModel = findModelById(model.getId()).orElse(model);
        Model modelToFile = buildModel(fullModel.getType(),
                                       fullModel.getName());
        modelToFile.setId(fullModel.getType().toLowerCase().concat("-").concat(model.getId()));
        modelToFile.setExtensions(fullModel.getExtensions());

        FileContent extensionsFileContent = new FileContent(getExtensionsFilename(model),
                                                            CONTENT_TYPE_JSON,
                                                            jsonConverter.convertToJsonBytes(modelToFile));
        return Optional.of(extensionsFileContent);
    }

    @Override
    public void cleanModelIdList() {
        this.modelIdentifiers.clear();
    }

    @Override
    public Optional<FileContent> getModelDiagramFile(String modelId) {
        //TODO: to implement
        return Optional.empty();
    }

    @Override
    public String getExtensionsFilename(Model model) {
        return toJsonFilename(model.getName() + findModelType(model).getExtensionsFileSuffix());
    }

    @Override
    public FileContent getModelContentFile(Model model) {
        return getModelFileContent(model,
                                   modelRepository.getModelContent(model));
    }

    @Override
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

    @Override
    public Model updateModelContent(Model modelToBeUpdate,
                                    FileContent fileContent) {
        FileContent fixedFileContent = this.modelIdentifiers.isEmpty()
                ? fileContent
                : overrideModelContentId(modelToBeUpdate,
                                         fileContent);

        modelToBeUpdate.setContentType(fixedFileContent.getContentType());
        modelToBeUpdate.setContent(fixedFileContent.getFileContent());

        try{
          Optional.ofNullable(modelToBeUpdate.getType()).flatMap(modelContentService::findModelContentConverter)
            .flatMap(validator -> validator.convertToModelContent(fixedFileContent.getFileContent()))
            .ifPresent(modelContent -> modelToBeUpdate.setTemplate(modelContent.getTemplate()));
        }catch(XMLException e){
          throw new ImportModelException("Error importing model : "+e.getMessage());
        }

        emptyIfNull(modelContentService.findContentUploadListeners(modelToBeUpdate.getType())).stream().forEach(listener -> listener.execute(modelToBeUpdate,
                                                                                                                                             fixedFileContent));

        return modelRepository.updateModelContent(modelToBeUpdate,
                                                  fixedFileContent);
    }

    @Override
    public FileContent overrideModelContentId(Model model,
                                              FileContent fileContent) {
        return modelContentService.findModelContentConverter(model.getType()).map(modelContentConverter -> modelContentConverter.overrideModelId(fileContent,
                                                                                                                                                 this.modelIdentifiers))
                .orElse(fileContent);
    }

    @Override
    public Optional<ModelContent> createModelContentFromModel(Model model,
                                                              FileContent fileContent) {
        return (Optional<ModelContent>) modelContentService.findModelContentConverter(model.getType())
                .map(modelContentConverter -> modelContentConverter.convertToModelContent(fileContent.getFileContent())).orElse(Optional.empty());
    }

    @Override
    public Model importSingleModel(Project project,
                                   ModelType modelType,
                                   FileContent fileContent) {
        Model model = this.importModel(project,
                                       modelType,
                                       fileContent);
        model = this.updateModelContent(model,
                                       fileContent);
        this.cleanModelIdList();
        return model;
    }

    @Override
    public Model importModel(Project project,
                             ModelType modelType,
                             FileContent fileContent) {
        logger.debug(MessageFormat.format("Importing model type {0} from file {1}: {2}",
                                          modelType,
                                          fileContent.getFilename(),
                                          fileContent));

        Model model = importModelFromContent(project,
                                             modelType,
                                             fileContent);
        return model;
    }

    @Override
    public Model importModelFromContent(Project project,
                                        ModelType modelType,
                                        FileContent fileContent) {
        Model model = null;
        if (modelTypeService.isJson(modelType) || ContentTypeUtils.isJsonContentType(fileContent.getContentType())) {
            model = convertContentToModel(modelType,
                                          fileContent);
        } else {
            model = createModelFromContent(modelType,
                                           fileContent);
        }
        String convertedId = model.getId();

        if (model.getId() == null && (modelTypeService.isJson(modelType) == ContentTypeUtils.isJsonContentType(fileContent.getContentType()))) {
            convertedId = retrieveModelIdFromModelContent(model,
                                            fileContent);
        }
        createModel(project,
                    model);
        if (convertedId != null) {
            modelIdentifiers.put(convertedId,
                                 String.join("-",
                                             model.getType().toLowerCase(),
                                             model.getId()));
        }
        return model;
    }

    @Override
    public <T extends Task> List<T> getTasksBy(Project project, ModelType processModelType, @NonNull Class<T> clazz) {
        Assert.notNull(clazz, "Class task type it must not be null");
        return getProcessesBy(project, processModelType)
                .stream()
                .map(Process::getFlowElements)
                .flatMap(Collection::stream)
                .filter(clazz::isInstance)
                .map(clazz::cast)
                .collect(Collectors.toList());
    }

    @Override
    public List<Process> getProcessesBy(Project project, ModelType type) {
        return this.getModels(project, type, Pageable.unpaged())
                .stream()
                .filter(model -> nonNull(model.getContent()))
                .map(this::safeGetBpmnModel)
                .map(BpmnModel::getProcesses)
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }

    private BpmnModel safeGetBpmnModel(Model model) {
        try {
            return processModelContentConverter.convertToBpmnModel(model.getContent());
        } catch (IOException | XMLStreamException e) {
            throw new RuntimeException(e);
        }
    }

    private String retrieveModelIdFromModelContent(Model model,
                                                   FileContent fileContent) {
        Optional<ModelContent> modelContent = this.createModelContentFromModel(model,
                                                                               fileContent);
        return modelContent.isPresent() ? modelContent.get().getId() : null;
    }

    @Override
    public Model convertContentToModel(ModelType modelType,
                                       FileContent fileContent) {
        Model model = jsonConverter.tryConvertToEntity(fileContent.getFileContent())
                .orElseThrow(() -> new ImportModelException("Cannot convert json file content to model: " + fileContent));
        model.setName(removeEnd(removeExtension(fileContent.getFilename(),
                                                JSON),
                                modelType.getExtensionsFileSuffix()));
        model.setType(modelType.getName());

        return model;
    }

    @Override
    public Model createModelFromContent(ModelType modelType,
                                        FileContent fileContent) {
        return contentFilenameToModelName(fileContent.getFilename(),
                                          modelType).map(
                                                         modelName -> buildModel(modelType.getName(),
                                                                                 modelName))
                                                  .orElseThrow(() -> new ImportModelException(MessageFormat
                                                          .format("Unexpected extension was found for file to import model of type {0}: {1}",
                                                                  modelType.getName(),
                                                                  fileContent.getFilename())));
    }

    @Override
    public Optional<String> contentFilenameToModelName(String filename,
                                                       ModelType modelType) {
        return Arrays.stream(modelType.getAllowedContentFileExtension()).filter(filename::endsWith).findFirst().map(extension -> removeExtension(filename,
                                                                                                                                                 extension));
    }

    @Override
    public void validateModelContent(Model model,
                                     ValidationContext validationContext) {
        validateModelContent(model.getType(),
                             modelRepository.getModelContent(model),
                             validationContext);
    }

    @Override
    public void validateModelContent(Model model,
                                     FileContent fileContent) {
        ValidationContext validationContext = !modelTypeService.isJson(findModelType(model)) && fileContent.getContentType().equals(CONTENT_TYPE_JSON)
                ? EMPTY_CONTEXT
                : Optional.ofNullable(model.getProject()).map(this::createValidationContext).orElseGet(() -> createValidationContext(model));

        validateModelContent(model.getType(),
                             fileContent.getFileContent(),
                             validationContext);
    }

    private ValidationContext createValidationContext(Project project) {
        return new ProjectValidationContext(getAllModels(project));
    }

    private ValidationContext createValidationContext(Model model) {
        return new ProjectValidationContext(model);
    }

    @Override
    public void validateModelContent(Model model,
                                     FileContent fileContent,
                                     ValidationContext validationContext) {
        validateModelContent(model.getType(),
                             fileContent.getFileContent(),
                             validationContext);
    }

    private void validateModelContent(String modelType,
                                      byte[] modelContent,
                                      ValidationContext validationContext) {
        emptyIfNull(modelContentService.findModelValidators(modelType)).stream().forEach(modelValidator -> modelValidator.validateModelContent(modelContent,
                                                                                                                                               validationContext));
    }

    @Override
    public void validateModelExtensions(Model model,
                                        ValidationContext validationContext) {
        validateModelExtensions(model.getType(),
                                modelRepository.getModelContent(model),
                                validationContext);
    }

    @Override
    public void validateModelExtensions(Model model,
                                        FileContent fileContent) {
        ValidationContext validationContext = !modelTypeService.isJson(findModelType(model))
                ? EMPTY_CONTEXT
                : Optional.ofNullable(model.getProject()).map(this::createValidationContext).orElseGet(() -> createValidationContext(model));
        validateModelExtensions(model.getType(),
                                fileContent.getFileContent(),
                                validationContext);
    }

    @Override
    public void validateModelExtensions(Model model,
                                        FileContent fileContent,
                                        ValidationContext validationContext) {
        validateModelExtensions(model.getType(),
                                fileContent.getFileContent(),
                                validationContext);
    }

    private void validateModelExtensions(String modelType,
                                         byte[] modelContent,
                                         ValidationContext validationContext) {
        emptyIfNull(modelExtensionsService.findExtensionsValidators(modelType)).stream().forEach(modelValidator -> modelValidator.validateModelExtensions(modelContent,
                                                                                                                                                          validationContext));
    }

    private ModelType findModelType(Model model) {
        return Optional.ofNullable(model.getType()).flatMap(modelTypeService::findModelTypeByName)
                .orElseThrow(() -> new UnknownModelTypeException("Unknown model type: " + model.getType()));
    }

}
