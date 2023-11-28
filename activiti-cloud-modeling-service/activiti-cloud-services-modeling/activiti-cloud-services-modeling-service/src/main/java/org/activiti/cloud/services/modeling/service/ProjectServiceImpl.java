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

import static org.activiti.cloud.services.common.util.ContentTypeUtils.JSON;
import static org.activiti.cloud.services.common.util.ContentTypeUtils.changeToJsonFilename;
import static org.activiti.cloud.services.common.util.ContentTypeUtils.getContentTypeByPath;
import static org.activiti.cloud.services.common.util.ContentTypeUtils.removeExtension;
import static org.activiti.cloud.services.modeling.service.ModelTypeComparators.MODEL_JSON_FILE_TYPE_COMPARATOR;
import static org.activiti.cloud.services.modeling.service.ModelTypeComparators.MODEL_TYPE_COMPARATOR;

import jakarta.transaction.Transactional;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.activiti.bpmn.model.UserTask;
import org.activiti.cloud.modeling.api.Model;
import org.activiti.cloud.modeling.api.ModelType;
import org.activiti.cloud.modeling.api.ModelValidationError;
import org.activiti.cloud.modeling.api.ProcessModelType;
import org.activiti.cloud.modeling.api.Project;
import org.activiti.cloud.modeling.api.ValidationContext;
import org.activiti.cloud.modeling.api.process.ModelScope;
import org.activiti.cloud.modeling.converter.JsonConverter;
import org.activiti.cloud.modeling.core.error.ImportProjectException;
import org.activiti.cloud.modeling.core.error.SemanticModelValidationException;
import org.activiti.cloud.modeling.repository.ProjectRepository;
import org.activiti.cloud.services.common.file.FileContent;
import org.activiti.cloud.services.common.zip.ZipBuilder;
import org.activiti.cloud.services.common.zip.ZipStream;
import org.activiti.cloud.services.modeling.service.api.ModelService;
import org.activiti.cloud.services.modeling.service.api.ModelService.ProjectAccessControl;
import org.activiti.cloud.services.modeling.service.api.ProjectService;
import org.activiti.cloud.services.modeling.service.decorators.ProjectDecoratorService;
import org.activiti.cloud.services.modeling.service.filters.ProjectFilterService;
import org.activiti.cloud.services.modeling.service.utils.KeyGenerator;
import org.activiti.cloud.services.modeling.validation.ProjectValidationContext;
import org.activiti.cloud.services.modeling.validation.project.ProjectNameValidator;
import org.activiti.cloud.services.modeling.validation.project.ProjectValidator;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.Nullable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.multipart.MultipartFile;

/**
 * Business logic related to {@link Project} entities
 */
@PreAuthorize("hasRole('ACTIVITI_MODELER')")
@Transactional
public class ProjectServiceImpl implements ProjectService {

    private static final Pattern EXPRESSION_REGEX = Pattern.compile("^\\$\\{[\\p{Graph}]+\\}+$");

    private final ProjectRepository projectRepository;

    private final ModelService modelService;

    private final ModelTypeService modelTypeService;

    private final JsonConverter<ProjectDescriptor> descriptorJsonConverter;

    private final JsonConverter<Project> jsonConverter;

    private final JsonConverter<Map> jsonMetadataConverter;

    private final Set<ProjectValidator> projectValidators;

    private final ProjectFilterService projectFilterService;

    private final ProjectDecoratorService projectDecoratorService;

    private final KeyGenerator keyGenerator;

    public ProjectServiceImpl(
        ProjectRepository projectRepository,
        ModelService modelService,
        ModelTypeService modelTypeService,
        JsonConverter<ProjectDescriptor> descriptorJsonConverter,
        JsonConverter<Project> jsonConverter,
        JsonConverter<Map> jsonMetadataConverter,
        Set<ProjectValidator> projectValidators,
        ProjectFilterService projectFilterService,
        ProjectDecoratorService projectDecoratorService,
        KeyGenerator keyGenerator
    ) {
        this.projectRepository = projectRepository;
        this.modelService = modelService;
        this.modelTypeService = modelTypeService;
        this.descriptorJsonConverter = descriptorJsonConverter;
        this.jsonConverter = jsonConverter;
        this.projectValidators = projectValidators;
        this.jsonMetadataConverter = jsonMetadataConverter;
        this.projectFilterService = projectFilterService;
        this.projectDecoratorService = projectDecoratorService;
        this.keyGenerator = keyGenerator;
    }

    /**
     * Get a page of projects.
     *
     * @param pageable the pagination information
     * @return the page
     */
    @Override
    public Page<Project> getProjects(Pageable pageable, String name, List<String> filters, List<String> include) {
        String projectName = name != null ? name.toLowerCase() : null;
        List<String> filteredProjects = getFilteredProjectIds(filters);
        Page<Project> projects = projectRepository.getProjects(pageable, projectName, filteredProjects);
        return decorateAll(projects, include);
    }

    /**
     * Create an project.
     *
     * @param project the project to create
     * @return the created project
     */
    @Override
    public Project createProject(Project project) {
        project.setId(null);
        project.setKey(keyGenerator.generate(project.getName()));
        project.setName(project.getName());
        List<ModelValidationError> nameValidationErrors = validateProjectNameAndKey(project);
        if (!nameValidationErrors.isEmpty()) {
            throw new SemanticModelValidationException(
                "Validation errors found in project's models",
                nameValidationErrors
            );
        }
        return projectRepository.createProject(project);
    }

    /**
     * Update an project.
     *
     * @param projectToUpdate the project to update
     * @param newProject      the project containing the new values to be used for update
     * @return the the updated project
     */
    @Override
    public Project updateProject(Project projectToUpdate, Project newProject) {
        Optional.ofNullable(newProject.getDescription()).ifPresent(projectToUpdate::setDescription);
        Optional
            .ofNullable(newProject.getName())
            .ifPresent(name -> {
                if (!name.equals(projectToUpdate.getName())) {
                    projectToUpdate.setName(name);
                    projectToUpdate.setKey(keyGenerator.generate(name));
                }
            });
        return projectRepository.updateProject(projectToUpdate);
    }

    /**
     * Delete an project.
     *
     * @param project the project to be deleted
     */
    @Override
    public void deleteProject(Project project) {
        deleteAllModelsInProject(project);
        projectRepository.deleteProject(project);
    }

    private void deleteAllModelsInProject(Project project) {
        modelService
            .getAllModels(project)
            .stream()
            .filter(model -> ModelScope.PROJECT.equals(model.getScope()))
            .forEach(modelService::deleteModel);
    }

    /**
     * Find an project by id.
     *
     * @param projectId the id to search for
     * @return the found project, or {@literal Optional#empty()}
     */
    @Override
    public Optional<Project> findProjectById(String projectId, List<String> include) {
        Optional<Project> project = projectRepository.findProjectById(projectId);
        return decorate(project, include);
    }

    /**
     * Export an project to a zip file.
     *
     * @param project the project to export
     * @return the {@link FileContent} with zip content
     * @throws IOException in case of I/O error
     */
    @Override
    public FileContent exportProject(Project project) throws IOException {
        ProjectDescriptor projectDescriptor = buildDescriptor(project);

        ZipBuilder zipBuilder = new ZipBuilder(project.getKey())
            .appendFile(
                descriptorJsonConverter.convertToJsonBytes(projectDescriptor),
                changeToJsonFilename(project.getKey())
            );

        modelService
            .getAllModels(project)
            .forEach(model ->
                modelTypeService
                    .findModelTypeByName(model.getType())
                    .map(ModelType::getFolderName)
                    .ifPresent(folderName -> {
                        zipBuilder.appendFolder(folderName).appendFile(modelService.exportModel(model), folderName);
                        modelService
                            .getModelExtensionsFileContent(model)
                            .map(extensionFileContent -> zipBuilder.appendFile(extensionFileContent, folderName));
                    })
            );
        return zipBuilder.toZipFileContent();
    }

    @Override
    public Project copyProject(Project projectToCopy, String newProjectName) {
        String newProjectKey = keyGenerator.generate(newProjectName);
        Project projectCopy = projectRepository.copyProject(projectToCopy, newProjectName, newProjectKey);
        List<Model> models = modelService.getAllModels(projectToCopy);

        Map<String, String> identifiersToUpdate = new HashMap<>();
        models
            .stream()
            .sorted(MODEL_TYPE_COMPARATOR)
            .forEach(model -> modelService.copyModel(model, projectCopy, identifiersToUpdate));

        return projectCopy;
    }

    @Override
    public ProjectAccessControl getProjectAccessControl(Project project) {
        List<UserTask> userTasks = modelService.getTasksBy(project, new ProcessModelType(), UserTask.class);

        Set<String> users = extractFromTasks(this::selectUsers, userTasks);
        Set<String> groups = extractFromTasks(this::selectGroups, userTasks);

        return new ProjectAccessControl(users, groups);
    }

    private Set<String> extractFromTasks(Function<UserTask, Set<String>> extractor, List<UserTask> userTasks) {
        return userTasks.stream().map(extractor).flatMap(Set::stream).collect(Collectors.toSet());
    }

    private Set<String> selectGroups(UserTask userTask) {
        return selectCandidatesThatAreNotAnExpression(userTask.getCandidateGroups());
    }

    private Set<String> selectUsers(UserTask userTask) {
        Set<String> users = selectCandidatesThatAreNotAnExpression(userTask.getCandidateUsers());
        String assignee = userTask.getAssignee();
        if (assignee != null && isNotAnExpression(assignee)) {
            users.add(assignee);
        }
        return users;
    }

    private Set<String> selectCandidatesThatAreNotAnExpression(List<String> candidates) {
        Set<String> result = Collections.emptySet();
        if (candidates != null) {
            result = candidates.stream().filter(this::isNotAnExpression).collect(Collectors.toSet());
        }
        return result;
    }

    private boolean isNotAnExpression(String v) {
        return !EXPRESSION_REGEX.matcher(v).find();
    }

    private ProjectDescriptor buildDescriptor(Project project) {
        ProjectDescriptor projectDescriptor = new ProjectDescriptor(project);
        ProjectAccessControl accessControl = getProjectAccessControl(project);
        projectDescriptor.setUsers(accessControl.getUsers());
        projectDescriptor.setGroups(accessControl.getGroups());
        return projectDescriptor;
    }

    private Optional<FileContent> createFileContentFromZipEntry(ZipStream.ZipStreamEntry zipEntry) {
        return zipEntry
            .getContent()
            .map(bytes ->
                getContentTypeByPath(zipEntry.getFileName())
                    .map(contentType -> new FileContent(zipEntry.getFileName(), contentType, bytes))
            )
            .orElse(Optional.empty());
    }

    private void convertZipElementToModelObject(
        ZipStream.ZipStreamEntry zipEntry,
        @Nullable String name,
        FileContent fileContent,
        ProjectHolder projectHolder
    ) {
        Optional<String> folderName = zipEntry.getFolderName(0);

        if (folderName.isPresent()) {
            folderName
                .flatMap(modelTypeService::findModelTypeByFolderName)
                .ifPresent(modelType -> processZipEntryFile(projectHolder, fileContent, modelType));
        } else if (fileContent.isJson()) {
            zipEntry
                .getContent()
                .ifPresent(bytes ->
                    jsonConverter
                        .tryConvertToEntity(bytes)
                        .ifPresent(project -> projectHolder.setProject(project, name))
                );
        }
    }

    /**
     * Import an project form a zip multipart file.
     *
     * @param file the multipart zip file to import from
     * @param name the name of the new project that will be set if provided
     * @return the imported project
     * @throws IOException in case of multipart file input stream access error
     */
    @Override
    public Project importProject(MultipartFile file, @Nullable String name) throws IOException {
        try (InputStream inputStream = file.getInputStream()) {
            return importModelsFromProjectHolder(getProjectHolderFromZipStream(ZipStream.of(inputStream), name));
        }
    }

    /**
     * Import a project from a zip inputstream.
     *
     * @param file the InputStream zip file to import from
     * @param name the name of the new project that will be set if provided
     * @return the imported project
     * @throws IOException in case of InputStream access error
     */
    @Override
    public Project importProject(InputStream file, String name) throws IOException {
        return importModelsFromProjectHolder(getProjectHolderFromZipStream(ZipStream.of(file), name));
    }

    private void importJSONModelFiles(
        ProjectHolder projectHolder,
        Project createdProject,
        ProjectHolder.ModelJsonFile modelJsonFile
    ) {
        ImportedModel createdModel = modelService.importModel(
            createdProject,
            modelJsonFile.getModelType(),
            modelJsonFile.getFileContent()
        );
        if (createdModel.hasIdentifiersToUpdate()) {
            projectHolder.addIdentifierToUpdate(createdModel.getOriginalId(), createdModel.getUpdatedId());
        }

        modelService.updateModelContent(
            createdModel.getModel(),
            modelJsonFile.getFileContent(),
            projectHolder.getIdentifiersToUpdate()
        );

        Model model = createdModel.getModel();
        projectHolder
            .getModelExtension(model)
            .ifPresent(fileMetadata -> {
                jsonMetadataConverter
                    .tryConvertToEntity(fileMetadata.getFileContent())
                    .ifPresent(extensions -> model.setExtensions(getExtensionsValueMapFromJson(extensions)));
                modelService.updateModel(model, model);
            });
    }

    private Map<ImportedModel, FileContent> createXMLModelFiles(ProjectHolder projectHolder, Project createdProject) {
        Map<ImportedModel, FileContent> createdModels = new HashMap<>();
        projectHolder
            .getProcessFiles()
            .forEach(modelProcessFile -> {
                ImportedModel createdModel = modelService.importModel(
                    createdProject,
                    modelProcessFile.getModelType(),
                    modelProcessFile.getFileContent()
                );
                createdModels.put(createdModel, modelProcessFile.getFileContent());
            });
        return createdModels;
    }

    private void importXMLModelFiles(
        ProjectHolder projectHolder,
        Project createdProject,
        ModelType modelType,
        FileContent fileContent
    ) {
        ImportedModel createdModel = modelService.importModel(createdProject, modelType, fileContent);
        if (createdModel.hasIdentifiersToUpdate()) {
            projectHolder.addIdentifierToUpdate(createdModel.getOriginalId(), createdModel.getUpdatedId());
        }
        updateModelProcessImported(projectHolder, createdModel, fileContent);
    }

    private void updateModelProcessImported(
        ProjectHolder projectHolder,
        ImportedModel importedModel,
        FileContent fileContent
    ) {
        modelService.updateModelContent(importedModel.getModel(), fileContent, projectHolder.getIdentifiersToUpdate());

        Model model = importedModel.getModel();

        projectHolder
            .getModelExtension(model)
            .ifPresent(fileMetadata -> {
                jsonMetadataConverter
                    .tryConvertToEntity(fileMetadata.getFileContent())
                    .ifPresent(extensions -> model.setExtensions(getExtensionsValueMapFromJson(extensions)));
                modelService.updateModel(model, model);
            });
    }

    private Map<String, Object> getExtensionsValueMapFromJson(Map<String, Object> extensions) {
        return ((Map<String, Object>) extensions.get("extensions"));
    }

    private void processZipEntryFile(ProjectHolder projectHolder, FileContent fileContent, ModelType modelType) {
        String modelName = removeExtension(fileContent.getFilename(), JSON);

        if (isProjectExtension(modelName, modelType, fileContent)) {
            modelName = StringUtils.removeEnd(modelName, modelType.getExtensionsFileSuffix());
            projectHolder.addModelExtension(modelName, modelType, fileContent);
        } else if (isProcessContent(modelName, modelType, fileContent)) {
            modelService
                .contentFilenameToModelName(modelName, modelType)
                .ifPresent(fixedModelName -> projectHolder.addProcess(fixedModelName, modelType, fileContent));
        } else if (isModelContent(modelName, modelType, fileContent)) {
            modelService
                .contentFilenameToModelName(modelName, modelType)
                .ifPresent(fixedModelName -> projectHolder.addModelContent(fixedModelName, modelType, fileContent));
        } else {
            if (modelName.endsWith(modelType.getExtensionsFileSuffix())) {
                modelName = StringUtils.removeEnd(modelName, modelType.getExtensionsFileSuffix());
            }
            projectHolder.addModelJsonFile(modelName, modelType, fileContent);
        }
    }

    private boolean isProjectExtension(String modelName, ModelType modelType, FileContent fileContent) {
        return fileContent.isJson() && (modelName.endsWith(modelType.getExtensionsFileSuffix()));
    }

    private boolean isProcessContent(String modelName, ModelType modelType, FileContent fileContent) {
        return (
            !fileContent.isJson() ||
            (!modelName.endsWith(modelType.getExtensionsFileSuffix()) && modelTypeService.isProcessContent(modelType))
        );
    }

    private boolean isModelContent(String modelName, ModelType modelType, FileContent fileContent) {
        return (
            !fileContent.isJson() ||
            (!modelName.endsWith(modelType.getExtensionsFileSuffix()) && modelTypeService.isContentXML(modelType))
        );
    }

    public List<ModelValidationError> validateProjectNameAndKey(Project project) {
        return getProjectNameValidator().validateNameAndKey(project).collect(Collectors.toList());
    }

    private ProjectNameValidator getProjectNameValidator() {
        return (ProjectNameValidator) projectValidators
            .stream()
            .filter(projectValidator -> projectValidator instanceof ProjectNameValidator)
            .findFirst()
            .orElseThrow(() -> new RuntimeException("ProjectNameValidator not found."));
    }

    @Override
    public void validateProject(Project project) {
        handleErrors(getProjectValidationErrors(project));
    }

    @Override
    public void validateProjectIgnoreWarnings(Project project) {
        List<ModelValidationError> validationErrors = getProjectValidationErrors(project)
            .stream()
            .filter(validationError -> !validationError.isWarning())
            .collect(Collectors.toList());

        handleErrors(validationErrors);
    }

    private List<ModelValidationError> getProjectValidationErrors(Project project) {
        List<Model> availableModels = modelService.getAllModels(project);
        ValidationContext validationContext = new ProjectValidationContext(availableModels);

        Stream<ModelValidationError> validationErrorStream = Stream
            .concat(
                projectValidators.stream().flatMap(validator -> validator.validate(project, validationContext)),
                availableModels.stream().flatMap(model -> getModelValidationErrors(model, validationContext))
            )
            .distinct();

        return validationErrorStream.collect(Collectors.toList());
    }

    private Stream<ModelValidationError> getModelValidationErrors(Model model, ValidationContext validationContext) {
        List<ModelValidationError> validationErrors = modelService.getModelValidationErrors(model, validationContext);
        validationErrors.addAll(modelService.getModelExtensionValidationErrors(model, validationContext));
        return validationErrors.stream();
    }

    private Project importModelsFromProjectHolder(ProjectHolder projectHolder) {
        Project project = projectHolder
            .getProjectMetadata()
            .map(this::createProject)
            .orElseThrow(() -> new ImportProjectException("No valid project entry found to import"));

        importModelsInProjectHolderToProject(projectHolder, project);
        return project;
    }

    private void importModelsInProjectHolderToProject(ProjectHolder projectHolder, Project project) {
        projectHolder
            .getModelJsonFiles()
            .stream()
            .sorted(MODEL_JSON_FILE_TYPE_COMPARATOR)
            .forEach(modelJsonFile -> {
                importJSONModelFiles(projectHolder, project, modelJsonFile);
            });

        projectHolder
            .getModelContentFiles()
            .forEach(modelXmlFile ->
                importXMLModelFiles(projectHolder, project, modelXmlFile.getModelType(), modelXmlFile.getFileContent())
            );

        Map<ImportedModel, FileContent> createdProcesses = createXMLModelFiles(projectHolder, project);
        createdProcesses
            .entrySet()
            .forEach(entry -> updateModelProcessImported(projectHolder, entry.getKey(), entry.getValue()));
    }

    private ProjectHolder getProjectHolderFromZipStream(ZipStream stream, String name) throws IOException {
        ProjectHolder projectHolder = new ProjectHolder();

        stream.forEach(zipEntry ->
            createFileContentFromZipEntry(zipEntry)
                .ifPresent(fileContent -> convertZipElementToModelObject(zipEntry, name, fileContent, projectHolder))
        );

        return projectHolder;
    }

    @Override
    public Project replaceProjectContentWithProvidedModelsInFile(Project project, InputStream inputStream)
        throws IOException {
        ProjectHolder projectHolder = getProjectHolderFromZipStream(ZipStream.of(inputStream), project.getName());

        if (projectHolder.getProjectMetadata().isEmpty()) {
            throw new ImportProjectException("No valid project entry found to import");
        }

        deleteAllModelsInProject(project);

        importModelsInProjectHolderToProject(projectHolder, project);

        return project;
    }

    public List<String> getFilteredProjectIds(List<String> filters) {
        List<String> filteredProjects = null;
        if (anyNotBlank(filters)) {
            filteredProjects = projectFilterService.getFilterIds(filters);
        }
        return filteredProjects;
    }

    public Optional<Project> decorate(Optional<Project> project, List<String> include) {
        if (anyNotBlank(include)) {
            project.ifPresent(p -> projectDecoratorService.decorate(p, include));
        }
        return project;
    }

    public Page<Project> decorateAll(Page<Project> projects, List<String> include) {
        if (anyNotBlank(include)) {
            projectDecoratorService.decorateAll(projects.getContent(), include);
        }
        return projects;
    }

    private boolean anyNotBlank(List<String> filters) {
        return filters != null && filters.stream().anyMatch(StringUtils::isNotBlank);
    }

    private static void handleErrors(List<ModelValidationError> validationErrors) {
        if (!validationErrors.isEmpty()) {
            throw new SemanticModelValidationException("Validation errors found in project's models", validationErrors);
        }
    }
}
