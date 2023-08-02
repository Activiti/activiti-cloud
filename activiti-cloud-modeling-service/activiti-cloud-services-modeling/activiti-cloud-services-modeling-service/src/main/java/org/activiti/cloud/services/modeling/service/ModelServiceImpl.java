/*
 * Copyright 2017-2020 Alfresco Software, Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.removeEnd;

import jakarta.transaction.Transactional;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.activiti.bpmn.exceptions.XMLException;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.Process;
import org.activiti.bpmn.model.Task;
import org.activiti.cloud.modeling.api.Model;
import org.activiti.cloud.modeling.api.ModelContent;
import org.activiti.cloud.modeling.api.ModelContentValidator;
import org.activiti.cloud.modeling.api.ModelType;
import org.activiti.cloud.modeling.api.ModelUpdateListener;
import org.activiti.cloud.modeling.api.ModelValidationError;
import org.activiti.cloud.modeling.api.Project;
import org.activiti.cloud.modeling.api.ValidationContext;
import org.activiti.cloud.modeling.api.process.ModelScope;
import org.activiti.cloud.modeling.converter.JsonConverter;
import org.activiti.cloud.modeling.core.error.ImportModelException;
import org.activiti.cloud.modeling.core.error.ModelNameConflictException;
import org.activiti.cloud.modeling.core.error.ModelNameInvalidException;
import org.activiti.cloud.modeling.core.error.ModelScopeIntegrityException;
import org.activiti.cloud.modeling.core.error.SemanticModelValidationException;
import org.activiti.cloud.modeling.core.error.UnknownModelTypeException;
import org.activiti.cloud.modeling.repository.ModelRepository;
import org.activiti.cloud.services.common.file.FileContent;
import org.activiti.cloud.services.modeling.converter.ProcessModelContentConverter;
import org.activiti.cloud.services.modeling.service.api.ModelService;
import org.activiti.cloud.services.modeling.service.utils.FileContentSanitizer;
import org.activiti.cloud.services.modeling.validation.ProjectValidationContext;
import org.activiti.cloud.services.modeling.validation.magicnumber.FileMagicNumberValidator;
import org.activiti.cloud.services.modeling.validation.model.ModelNameValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.Assert;

/**
 * Business logic related to {@link Model} entities including process models, form models, connectors, data models and
 * decision table models.
 */
@PreAuthorize("hasRole('ACTIVITI_MODELER')")
@Transactional
public class ModelServiceImpl implements ModelService {

    private static final Logger logger = LoggerFactory.getLogger(ModelServiceImpl.class);

    private final ModelRepository modelRepository;

    private final ModelTypeService modelTypeService;

    private final ModelContentService modelContentService;

    private final ModelExtensionsService modelExtensionsService;

    private final JsonConverter<Model> jsonConverter;

    private final ProcessModelContentConverter processModelContentConverter;

    private final FileMagicNumberValidator fileContentValidator;

    private final ModelNameValidator modelNameValidator;

    private final FileContentSanitizer fileContentSanitizer;

    private final Map<String, List<ModelUpdateListener>> modelUpdateListenersMapByModelType;

    private static final String MODEL_IDENTIFIER_SEPARATOR = "-";

    private static final String ERROR_MESSAGE =
        "Semantic model validation errors encountered: %d schema violations " + "found";

    private static final String WARNING_MESSAGE = "Semantic model validation warnings encountered: %d warnings found";

    public ModelServiceImpl(
        ModelRepository modelRepository,
        ModelTypeService modelTypeService,
        ModelContentService modelContentService,
        ModelExtensionsService modelExtensionsService,
        JsonConverter<Model> jsonConverter,
        ProcessModelContentConverter processModelContentConverter,
        Set<ModelUpdateListener> modelUpdateListeners,
        FileMagicNumberValidator fileContentValidator,
        ModelNameValidator modelNameValidator,
        FileContentSanitizer fileContentSanitizer
    ) {
        this.modelRepository = modelRepository;
        this.modelTypeService = modelTypeService;
        this.modelContentService = modelContentService;
        this.jsonConverter = jsonConverter;
        this.modelExtensionsService = modelExtensionsService;
        this.processModelContentConverter = processModelContentConverter;
        this.fileContentValidator = fileContentValidator;
        this.modelNameValidator = modelNameValidator;
        this.fileContentSanitizer = fileContentSanitizer;
        modelUpdateListenersMapByModelType =
            modelUpdateListeners
                .stream()
                .collect(
                    Collectors.groupingBy(modelUpdateListener -> modelUpdateListener.getHandledModelType().getName())
                );
    }

    @Override
    public List<Model> getAllModels(Project project) {
        return modelTypeService
            .getAvailableModelTypes()
            .stream()
            .map(modelType -> getModels(project, modelType, Pageable.unpaged()))
            .map(Page::getContent)
            .flatMap(List::stream)
            .collect(Collectors.toList());
    }

    @Override
    public Page<Model> getModels(Project project, ModelType modelType, Pageable pageable) {
        return modelRepository.getModels(project, modelType, pageable);
    }

    @Override
    public Page<Model> getModelsByName(Project project, String name, Pageable pageable) {
        if (isEmpty(name)) {
            return Page.empty(pageable);
        }
        return modelRepository.getModelsByName(project, name, pageable);
    }

    @Override
    public Model buildModel(String type, String name) {
        try {
            Model model = (Model) modelRepository.getModelType().getConstructor().newInstance();
            model.setType(type);
            model.setName(name);
            return model;
        } catch (
            InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e
        ) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Model createModel(Project project, Model model) {
        if (model.getType() != null && !model.getType().equals(PROCESS)) {
            validateModelNameCharacters(model.getName());
        }
        checkIfModelNameExistsInProject(project, model);
        checkModelScopeIntegrity(model);
        model.setId(null);
        ModelType modelType = findModelType(model);

        if (project != null) {
            model.addProject(project);
        }

        if (model.getExtensions() == null) {
            if (PROCESS.equals(modelType.getName()) || isJsonContentType(model.getContentType())) {
                model.setExtensions(new HashMap<>());
            }
        }

        if (PROCESS.equals(modelType.getName()) && model.getContent() != null) {
            // We leverage targetNamespace of bpmn models as a category field
            model.setCategory(processModelContentConverter.convertToBpmnModel(model.getContent()).getTargetNamespace());
        }

        return modelRepository.createModel(model);
    }

    private void validateModelName(String modelName) {
        Stream<ModelValidationError> validationErrors = this.modelNameValidator.validateDNSName(modelName, "model");
        validationErrors.findFirst().ifPresent(this::throwModelNameInvalidException);
    }

    private void validateModelNameCharacters(String modelName) {
        Stream<ModelValidationError> validationErrors =
            this.modelNameValidator.validateDNSNameCharacters(modelName, "model");
        validationErrors.findFirst().ifPresent(this::throwModelNameInvalidException);
    }

    private void throwModelNameInvalidException(ModelValidationError error) {
        String errorDescription = error.getDescription();
        logger.info(errorDescription);
        throw new ModelNameInvalidException(errorDescription);
    }

    private void checkIfModelNameExistsInProject(Project project, Model model) {
        Optional<Model> existingModel = modelRepository.findModelByNameInProject(
            project,
            model.getName(),
            model.getType()
        );
        if (!existingModel.isEmpty() && !existingModel.get().getId().equals(model.getId())) {
            throw new ModelNameConflictException(
                "A model with the same type already exists within the project with id: " +
                (project != null ? project.getId() : "null")
            );
        }
    }

    private void checkModelScopeIntegrity(Model model) {
        if (model.getScope() == null) {
            model.setScope(ModelScope.PROJECT);
        }

        if (ModelScope.PROJECT.equals(model.getScope()) && model.hasMultipleProjects()) {
            throw new ModelScopeIntegrityException("A model at PROJECT scope can only be associated to one project");
        }
    }

    @Override
    public Model updateModel(Model modelToBeUpdated, Model newModel) {
        if (newModel.hasProjects()) {
            newModel
                .getProjects()
                .stream()
                .forEach(project -> checkIfModelNameExistsInProject((Project) project, newModel));
        }

        if (newModel.getName() != null && !modelToBeUpdated.getType().equals(PROCESS)) {
            validateModelName(newModel.getName());
        }
        checkModelScopeIntegrity(newModel);

        findModelUpdateListeners(modelToBeUpdated.getType())
            .stream()
            .forEach(listener -> listener.execute(modelToBeUpdated, newModel));

        return modelRepository.updateModel(modelToBeUpdated, newModel);
    }

    @Override
    public Model copyModel(Model modelToBeCopied, Project project, Map<String, String> identifiersToUpdate) {
        Model copy = modelRepository.copyModel(modelToBeCopied, project);
        identifiersToUpdate.put(buildModelTypeAwareIdentifier(modelToBeCopied), buildModelTypeAwareIdentifier(copy));
        updateModelContent(copy, getModelContentFile(copy), identifiersToUpdate);
        return copy;
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
        Model modelToFile = buildModel(fullModel.getType(), fullModel.getName());
        modelToFile.setId(fullModel.getType().toLowerCase().concat(MODEL_IDENTIFIER_SEPARATOR).concat(model.getId()));
        modelToFile.setExtensions(fullModel.getExtensions());
        modelToFile.setScope(null);

        FileContent extensionsFileContent = new FileContent(
            getExtensionsFilename(model),
            CONTENT_TYPE_JSON,
            jsonConverter.convertToJsonBytes(modelToFile)
        );
        return Optional.of(extensionsFileContent);
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
        return getModelFileContent(model, modelRepository.getModelContent(model));
    }

    @Override
    public FileContent exportModel(Model model) {
        return getModelFileContent(model, modelRepository.getModelExport(model));
    }

    private FileContent getModelFileContent(Model model, byte[] modelBytes) {
        return new FileContent(
            setExtension(model.getName(), findModelType(model).getContentFileExtension()),
            model.getContentType(),
            modelBytes
        );
    }

    @Override
    public Model updateModelContent(Model model, FileContent fileContent, Map<String, String> identifiersToUpdate) {
        throwExceptionIfFileIsExecutable(model.getType(), fileContent);
        FileContent fixedFileContent = hasIdentifiersToUpdate(identifiersToUpdate)
            ? overrideModelContentId(model, fileContent, identifiersToUpdate)
            : fileContent;
        final FileContent sanitizedFileContent = fileContentSanitizer.sanitizeContent(fixedFileContent);

        model.setContentType(sanitizedFileContent.getContentType());
        model.setContent(sanitizedFileContent.getFileContent());

        if (
            model.getType().equals(PROCESS) &&
            sanitizedFileContent.getFileContent() != null &&
            isBpmnModelContent(sanitizedFileContent.getFileContent())
        ) {
            model.setCategory(
                processModelContentConverter
                    .convertToBpmnModel(sanitizedFileContent.getFileContent())
                    .getTargetNamespace()
            );
        }

        try {
            Optional
                .ofNullable(model.getType())
                .flatMap(modelContentService::findModelContentConverter)
                .flatMap(validator -> validator.convertToModelContent(sanitizedFileContent.getFileContent()))
                .ifPresent(modelContent -> model.setTemplate(modelContent.getTemplate()));
        } catch (XMLException e) {
            throw new ImportModelException("Error importing model : " + e.getMessage());
        }

        emptyIfNull(modelContentService.findContentUploadListeners(model.getType()))
            .stream()
            .forEach(listener -> listener.execute(model, sanitizedFileContent));

        return modelRepository.updateModelContent(model, sanitizedFileContent);
    }

    private boolean hasIdentifiersToUpdate(Map<String, String> identifiersToUpdate) {
        return identifiersToUpdate != null && !identifiersToUpdate.isEmpty();
    }

    @Override
    public FileContent overrideModelContentId(
        Model model,
        FileContent fileContent,
        Map<String, String> identifiersToUpdate
    ) {
        return modelContentService
            .findModelContentConverter(model.getType())
            .map(modelContentConverter -> modelContentConverter.overrideModelId(fileContent, identifiersToUpdate))
            .orElse(fileContent);
    }

    @Override
    public Optional<ModelContent> createModelContentFromModel(Model model, FileContent fileContent) {
        final FileContent sanitizedFileContent = fileContentSanitizer.sanitizeContent(fileContent);
        return (Optional<ModelContent>) modelContentService
            .findModelContentConverter(model.getType())
            .map(modelContentConverter ->
                modelContentConverter.convertToModelContent(sanitizedFileContent.getFileContent())
            )
            .orElse(Optional.empty());
    }

    @Override
    public Model importSingleModel(Project project, ModelType modelType, FileContent fileContent) {
        ImportedModel importedModel = importModel(project, modelType, fileContent);
        Map<String, String> identifiersToUpdate = importedModel.hasIdentifiersToUpdate()
            ? Map.of(importedModel.getOriginalId(), importedModel.getUpdatedId())
            : null;
        return updateModelContent(importedModel.getModel(), fileContent, identifiersToUpdate);
    }

    @Override
    public ImportedModel importModel(Project project, ModelType modelType, FileContent fileContent) {
        logger.debug(
            MessageFormat.format(
                "Importing model type {0} from file {1}: {2}",
                modelType,
                fileContent.getFilename(),
                fileContent
            )
        );

        return importModelFromContent(project, modelType, fileContent);
    }

    @Override
    public ImportedModel importModelFromContent(Project project, ModelType modelType, FileContent fileContent) {
        throwExceptionIfFileIsExecutable(modelType.getName(), fileContent);
        Model model = loadModel(modelType, fileContent);
        String originalId = resolveOriginalId(modelType, fileContent, model);

        model.setScope(ModelScope.PROJECT);
        createModel(project, model);
        if (originalId != null) {
            return new ImportedModel(model, originalId, buildModelTypeAwareIdentifier(model));
        }
        return new ImportedModel(model);
    }

    private String buildModelTypeAwareIdentifier(Model model) {
        return String.join(MODEL_IDENTIFIER_SEPARATOR, model.getType().toLowerCase(), model.getId());
    }

    private String resolveOriginalId(ModelType modelType, FileContent fileContent, Model model) {
        String convertedId = model.getId();

        if (
            model.getId() == null &&
            (modelTypeService.isJson(modelType) == isJsonContentType(fileContent.getContentType()))
        ) {
            convertedId = retrieveModelIdFromModelContent(model, fileContent);
        }
        return convertedId;
    }

    private Model loadModel(ModelType modelType, FileContent fileContent) {
        Model model;
        if (modelTypeService.isJson(modelType) || isJsonContentType(fileContent.getContentType())) {
            model = convertContentToModel(modelType, fileContent);
        } else {
            model = createModelFromContent(modelType, fileContent);
        }
        return model;
    }

    private void throwExceptionIfFileIsExecutable(String modelTypeName, FileContent fileContent) {
        if (fileContentValidator.checkFileIsExecutable(fileContent.getFileContent())) {
            throw new ImportModelException(
                MessageFormat.format(
                    "Import the executable file {0} for type {1} is forbidden.",
                    fileContent.getFilename(),
                    modelTypeName
                )
            );
        }
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
        return getModels(project, type, Pageable.unpaged())
            .stream()
            .map(Model::getContent)
            .filter(content -> nonNull(content))
            .map(processModelContentConverter::convertToBpmnModel)
            .map(BpmnModel::getProcesses)
            .flatMap(List::stream)
            .collect(Collectors.toList());
    }

    private boolean isBpmnModelContent(byte[] modelContent) {
        boolean isBpmn = true;
        try {
            processModelContentConverter.convertToBpmnModel(modelContent);
        } catch (RuntimeException e) {
            isBpmn = false;
        }
        return isBpmn;
    }

    private String retrieveModelIdFromModelContent(Model model, FileContent fileContent) {
        Optional<ModelContent> modelContent = createModelContentFromModel(model, fileContent);
        return modelContent.isPresent() ? modelContent.get().getId() : null;
    }

    @Override
    public Model convertContentToModel(ModelType modelType, FileContent fileContent) {
        Model model = jsonConverter
            .tryConvertToEntity(fileContent.getFileContent())
            .orElseThrow(() -> new ImportModelException("Cannot convert json file content to model: " + fileContent));
        model.setName(removeEnd(removeExtension(fileContent.getFilename(), JSON), modelType.getExtensionsFileSuffix()));
        model.setType(modelType.getName());

        return model;
    }

    @Override
    public Model createModelFromContent(ModelType modelType, FileContent fileContent) {
        return contentFilenameToModelName(fileContent.getFilename(), modelType)
            .map(modelName -> buildModel(modelType.getName(), modelName))
            .orElseThrow(() ->
                new ImportModelException(
                    MessageFormat.format(
                        "Unexpected extension was found for file to import " + "model " + "of type {0}: {1}",
                        modelType.getName(),
                        fileContent.getFilename()
                    )
                )
            );
    }

    @Override
    public Optional<String> contentFilenameToModelName(String filename, ModelType modelType) {
        return Arrays
            .stream(modelType.getAllowedContentFileExtension())
            .filter(filename::endsWith)
            .findFirst()
            .map(extension -> removeExtension(filename, extension));
    }

    @Override
    public void validateModelContent(Model model, ValidationContext validationContext) {
        validateModelContent(model, modelRepository.getModelContent(model), validationContext);
    }

    @Override
    public void validateModelContent(Model model, Project project) {
        validateModelContent(model, modelRepository.getModelContent(model), createValidationContext(project));
    }

    @Override
    public void validateModelContent(Model model, FileContent fileContent) {
        ValidationContext validationContext = getValidationContext(model, fileContent, null);
        validateModelContent(model, fileContent.getFileContent(), validationContext);
    }

    private ValidationContext getValidationContext(Model model, FileContent fileContent, @Nullable Project project) {
        if (!modelTypeService.isJson(findModelType(model)) && fileContent.getContentType().equals(CONTENT_TYPE_JSON)) {
            return EMPTY_CONTEXT;
        }

        if (project != null) {
            return Optional
                .ofNullable(project)
                .map(this::createValidationContext)
                .orElseGet(() -> createValidationContext(model));
        }

        return createValidationContext(model);
    }

    private ValidationContext createValidationContext(Project project) {
        return new ProjectValidationContext(getAllModels(project));
    }

    private ValidationContext createValidationContext(Model model) {
        return new ProjectValidationContext(model);
    }

    @Override
    public void validateModelContent(Model model, FileContent fileContent, ValidationContext validationContext) {
        validateModelContent(model, fileContent.getFileContent(), validationContext);
    }

    @Override
    public void validateModelContent(Model model, FileContent fileContent, Project project) {
        ValidationContext validationContext = getValidationContext(model, fileContent, project);

        validateModelContent(model, fileContent.getFileContent(), validationContext);
    }

    @Override
    public void validateModelContent(Model model, FileContent fileContent, Project project, boolean validateUsage) {
        if (validateUsage) {
            validateModelContentAndUsage(
                model,
                fileContent.getFileContent(),
                getValidationContext(model, fileContent, project)
            );
        } else {
            validateModelContent(model, fileContent, project);
        }
    }

    @Override
    public void validateModelContent(Model model, FileContent fileContent, boolean validateUsage) {
        if (validateUsage) {
            validateModelContentAndUsage(
                model,
                fileContent.getFileContent(),
                getValidationContext(model, fileContent, null)
            );
        } else {
            validateModelContent(model, fileContent);
        }
    }

    private void validateModelContentAndUsage(Model model, byte[] modelContent, ValidationContext validationContext) {
        Function<ModelContentValidator, Collection<ModelValidationError>> validationFunction = validator ->
            validator.validateModelContent(model, modelContent, validationContext, true);

        validate(model.getType(), validationFunction);
    }

    private void validate(
        String model,
        Function<ModelContentValidator, Collection<ModelValidationError>> validationFunction
    ) {
        List<ModelValidationError> validationErrors = modelContentService
            .findModelValidators(model)
            .stream()
            .map(validationFunction)
            .flatMap(Collection::stream)
            .collect(Collectors.toList());

        throwExceptionIfNeeded(validationErrors);
    }

    private void validateModelContent(Model model, byte[] modelContent, ValidationContext validationContext) {
        Function<ModelContentValidator, Collection<ModelValidationError>> validationFunction = modelValidator ->
            modelValidator.validateModelContent(model, modelContent, validationContext, false);

        validate(model.getType(), validationFunction);
    }

    private List<ModelValidationError> getModelContentValidationErrors(
        Model model,
        byte[] modelContent,
        ValidationContext validationContext
    ) {
        return modelContentService
            .findModelValidators(model.getType())
            .stream()
            .map(modelValidator -> modelValidator.validate(model, modelContent, validationContext, false))
            .flatMap(Collection::stream)
            .distinct()
            .collect(Collectors.toList());
    }

    @Override
    public void validateModelExtensions(Model model, ValidationContext validationContext) {
        validateModelExtensions(model.getType(), modelRepository.getModelContent(model), validationContext);
    }

    @Override
    public void validateModelExtensions(Model model, Project project) {
        validateModelExtensions(
            model.getType(),
            modelRepository.getModelContent(model),
            createValidationContext(project)
        );
    }

    @Override
    public void validateModelExtensions(Model model, FileContent fileContent) {
        ValidationContext validationContext = !modelTypeService.isJson(findModelType(model))
            ? EMPTY_CONTEXT
            : createValidationContext(model);
        validateModelExtensions(model.getType(), fileContent.getFileContent(), validationContext);
    }

    @Override
    public void validateModelExtensions(Model model, FileContent fileContent, ValidationContext validationContext) {
        validateModelExtensions(model.getType(), fileContent.getFileContent(), validationContext);
    }

    @Override
    public void validateModelExtensions(Model model, FileContent fileContent, Project project) {
        validateModelExtensions(model.getType(), fileContent.getFileContent(), createValidationContext(project));
    }

    @Override
    public Page<Model> getGlobalModels(ModelType modelType, boolean includeOrphans, Pageable pageable) {
        return modelRepository.getGlobalModels(modelType, includeOrphans, pageable);
    }

    private void validateModelExtensions(String modelType, byte[] modelContent, ValidationContext validationContext) {
        List<ModelValidationError> validationErrors = modelExtensionsService
            .findExtensionsValidators(modelType)
            .stream()
            .map(modelValidator -> modelValidator.validateModelExtensions(modelContent, validationContext))
            .flatMap(Collection::stream)
            .collect(Collectors.toList());

        throwExceptionIfNeeded(validationErrors);
    }

    private List<ModelValidationError> getModelExtensionsValidationErrors(
        String modelType,
        byte[] modelContent,
        ValidationContext validationContext
    ) {
        return modelExtensionsService
            .findExtensionsValidators(modelType)
            .stream()
            .map(modelValidator -> modelValidator.validate(modelContent, validationContext))
            .flatMap(Collection::stream)
            .collect(Collectors.toList());
    }

    private ModelType findModelType(Model model) {
        return Optional
            .ofNullable(model.getType())
            .flatMap(modelTypeService::findModelTypeByName)
            .orElseThrow(() -> new UnknownModelTypeException("Unknown model type: " + model.getType()));
    }

    @Override
    public List<ModelUpdateListener> findModelUpdateListeners(String modelType) {
        return (List<ModelUpdateListener>) emptyIfNull(modelUpdateListenersMapByModelType.get(modelType));
    }

    @Override
    public List<ModelValidationError> getModelValidationErrors(Model model, ValidationContext validationContext) {
        return getModelContentValidationErrors(model, modelRepository.getModelContent(model), validationContext);
    }

    @Override
    public List<ModelValidationError> getModelExtensionValidationErrors(
        Model model,
        ValidationContext validationContext
    ) {
        return getModelExtensionsFileContent(model)
            .filter(Objects::nonNull)
            .map(extensionsFileContent ->
                getModelExtensionsValidationErrors(
                    model.getType(),
                    extensionsFileContent.getFileContent(),
                    validationContext
                )
            )
            .orElse(Collections.emptyList());
    }

    private void throwExceptionIfNeeded(@NonNull List<ModelValidationError> allModelValidationErrors) {
        if (!allModelValidationErrors.isEmpty()) {
            List<ModelValidationError> modelValidationErrors = allModelValidationErrors
                .stream()
                .distinct()
                .collect(Collectors.toList());

            if (modelValidationErrors.stream().anyMatch(modelValidationError -> !modelValidationError.isWarning())) {
                throw new SemanticModelValidationException(
                    String.format(
                        ERROR_MESSAGE,
                        modelValidationErrors
                            .stream()
                            .filter(modelValidationError -> !modelValidationError.isWarning())
                            .count()
                    ),
                    modelValidationErrors
                );
            }

            throw new SemanticModelValidationException(
                String.format(WARNING_MESSAGE, modelValidationErrors.size()),
                modelValidationErrors
            );
        }
    }
}
