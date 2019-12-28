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

import static org.activiti.cloud.services.common.util.ContentTypeUtils.JSON;
import static org.activiti.cloud.services.common.util.ContentTypeUtils.getContentTypeByPath;
import static org.activiti.cloud.services.common.util.ContentTypeUtils.removeExtension;
import static org.activiti.cloud.services.common.util.ContentTypeUtils.toJsonFilename;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.transaction.Transactional;

import org.activiti.cloud.modeling.api.Model;
import org.activiti.cloud.modeling.api.ModelType;
import org.activiti.cloud.modeling.api.ModelValidationError;
import org.activiti.cloud.modeling.api.Project;
import org.activiti.cloud.modeling.api.ValidationContext;
import org.activiti.cloud.modeling.converter.JsonConverter;
import org.activiti.cloud.modeling.core.error.ImportProjectException;
import org.activiti.cloud.modeling.core.error.SemanticModelValidationException;
import org.activiti.cloud.modeling.repository.ProjectRepository;
import org.activiti.cloud.services.common.file.FileContent;
import org.activiti.cloud.services.common.zip.ZipBuilder;
import org.activiti.cloud.services.common.zip.ZipStream;
import org.activiti.cloud.services.modeling.service.api.ProjectService;
import org.activiti.cloud.services.modeling.validation.ProjectValidationContext;
import org.activiti.cloud.services.modeling.validation.project.ProjectValidator;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
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

    private final ProjectRepository projectRepository;

    private final ModelService modelService;

    private final ModelTypeService modelTypeService;

    private final JsonConverter<Project> jsonConverter;

    private final JsonConverter<Map> jsonMetadataConverter;

    private final Set<ProjectValidator> projectValidators;

    @Autowired
    public ProjectServiceImpl(ProjectRepository projectRepository,
                              ModelService modelService,
                              ModelTypeService modelTypeService,
                              JsonConverter<Project> jsonConverter,
                              JsonConverter<Map> jsonMetadataConverter,
                              Set<ProjectValidator> projectValidators) {
        this.projectRepository = projectRepository;
        this.modelService = modelService;
        this.modelTypeService = modelTypeService;
        this.jsonConverter = jsonConverter;
        this.projectValidators = projectValidators;
        this.jsonMetadataConverter = jsonMetadataConverter;
    }

    /**
     * Get a page of projects.
     *
     * @param pageable the pagination information
     * @return the page
     */
    @Override
    public Page<Project> getProjects(Pageable pageable,
                                     String name) {
        String projectName = name != null ? name.toLowerCase() : null;
        return projectRepository.getProjects(pageable,
                projectName);
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
    public Project updateProject(Project projectToUpdate,
                                 Project newProject) {
        Optional.ofNullable(newProject.getDescription()).ifPresent(projectToUpdate::setDescription);
        Optional.ofNullable(newProject.getName()).ifPresent(projectToUpdate::setName);
        return projectRepository.updateProject(projectToUpdate);
    }

    /**
     * Delete an project.
     *
     * @param project the project to be deleted
     */
    @Override
    public void deleteProject(Project project) {
        modelService.getAllModels(project).forEach(modelService::deleteModel);
        projectRepository.deleteProject(project);
    }

    /**
     * Find an project by id.
     *
     * @param projectId the id to search for
     * @return the found project, or {@literal Optional#empty()}
     */
    @Override
    public Optional<Project> findProjectById(String projectId) {
        return projectRepository.findProjectById(projectId);
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

        ZipBuilder zipBuilder = new ZipBuilder(project.getName())
                .appendFile(jsonConverter.convertToJsonBytes(project), toJsonFilename(project.getName()));

        modelService.getAllModels(project).forEach(model -> modelTypeService.findModelTypeByName(model.getType()).map(ModelType::getFolderName).ifPresent(folderName -> {
            zipBuilder.appendFolder(folderName)
                    .appendFile(modelService.exportModel(model), folderName);
            modelService.getModelExtensionsFileContent(model)
                    .map(extensionFileContent -> zipBuilder.appendFile(extensionFileContent, folderName));
        }));
        return zipBuilder.toZipFileContent();
    }

    private Optional<FileContent> createFileContentFromZipEntry(ZipStream.ZipStreamEntry zipEntry) {
        return zipEntry.getContent()
                .map(bytes -> getContentTypeByPath(zipEntry.getFileName())
                        .map(contentType -> new FileContent(zipEntry.getFileName(), contentType, bytes))
                )
                .orElse(Optional.empty());
    }


    private void convertZipElementToModelObject(ZipStream.ZipStreamEntry zipEntry, @Nullable String name, FileContent fileContent, ProjectHolder projectHolder) {
        Optional<String> folderName = zipEntry.getFolderName(0);

        if (folderName.isPresent()) {
            folderName.flatMap(modelTypeService::findModelTypeByFolderName)
                    .ifPresent(modelType -> processZipEntryFile(projectHolder, fileContent, modelType)
                    );
        } else if (fileContent.isJson()) {
            zipEntry.getContent().ifPresent(
                    bytes -> jsonConverter.tryConvertToEntity(bytes)
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
        ProjectHolder projectHolder = new ProjectHolder();

        ZipStream.of(file)
                .forEach(zipEntry -> this.createFileContentFromZipEntry(zipEntry)
                        .ifPresent(fileContent -> this.convertZipElementToModelObject(zipEntry, name, fileContent, projectHolder)));

        Project createdProject = projectHolder.getProjectMetadata().map(this::createProject)
                .orElseThrow(() -> new ImportProjectException("No valid project entry found to import: " + file.getOriginalFilename()));

        projectHolder.getModelJsonFiles().forEach(modelJsonFile -> {
            this.importJSONModelFiles(projectHolder, createdProject, modelJsonFile);
        });

        projectHolder.getModelContentFiles().forEach(modelXmlFile ->
                importXMLModelFiles(projectHolder, createdProject, modelXmlFile.getModelType(), modelXmlFile.getFileContent()));

        Map<Model, FileContent> createdProcesses = this.createXMLModelFiles(projectHolder, createdProject);
        createdProcesses.keySet().forEach(model -> this.updateModelProcessImported(projectHolder, model, createdProcesses.get(model)));

        modelService.cleanModelIdList();
        return createdProject;
    }

    private void importJSONModelFiles(ProjectHolder projectHolder,
                                      Project createdProject,
                                      ProjectHolder.ModelJsonFile modelJsonFile) {
        Model createdModel = modelService.importModel(createdProject,
                modelJsonFile.getModelType(),
                modelJsonFile.getFileContent());

        modelService.updateModelContent(createdModel, modelJsonFile.getFileContent());

        projectHolder.getModelExtension(createdModel)
                .ifPresent(fileMetadata -> {
                    jsonMetadataConverter.tryConvertToEntity(fileMetadata.getFileContent())
                            .ifPresent(extensions -> createdModel.setExtensions(this.getExtensionsValueMapFromJson(extensions)));
                    modelService.updateModel(createdModel, createdModel);
                });
    }

    private Map<Model, FileContent> createXMLModelFiles(ProjectHolder projectHolder, Project createdProject) {
        Map<Model, FileContent> createdModels = new HashMap<Model, FileContent>();
        projectHolder.getProcessFiles().forEach(modelProcessFile -> {
            Model createdModel = modelService.importModel(createdProject, modelProcessFile.getModelType(), modelProcessFile.getFileContent());
            createdModels.put(createdModel, modelProcessFile.getFileContent());
        });
        return createdModels;
    }

    private void importXMLModelFiles(ProjectHolder projectHolder,
                                     Project createdProject,
                                     ModelType modelType,
                                     FileContent fileContent) {
        Model createdModel = modelService.importModel(createdProject,
                modelType,
                fileContent);
        this.updateModelProcessImported(projectHolder, createdModel, fileContent);
    }

    private void updateModelProcessImported(ProjectHolder projectHolder, Model createdModel, FileContent fileContent) {
        modelService.updateModelContent(createdModel, fileContent);

        projectHolder.getModelExtension(createdModel)
                .ifPresent(fileMetadata -> {
                    jsonMetadataConverter.tryConvertToEntity(fileMetadata.getFileContent())
                            .ifPresent(extensions -> createdModel.setExtensions(this.getExtensionsValueMapFromJson(extensions)));
                    modelService.updateModel(createdModel, createdModel);
                });
    }

    private Map<String, Object> getExtensionsValueMapFromJson(Map<String, Object> extensions) {
        return ((Map<String, Object>) extensions.get("extensions"));
    }

    private void processZipEntryFile(ProjectHolder projectHolder,
                                     FileContent fileContent,
                                     ModelType modelType) {
        String modelName = removeExtension(fileContent.getFilename(), JSON);
        if (isProjectExtension(modelName, modelType, fileContent)) {
            modelName = StringUtils.removeEnd(modelName, modelType.getExtensionsFileSuffix());
            projectHolder.addModelExtension(modelName, modelType, fileContent);
        } else if (isProcessContent(modelName, modelType, fileContent)) {
            modelService.contentFilenameToModelName(modelName, modelType)
                    .ifPresent(fixedModelName -> projectHolder.addProcess(fixedModelName, modelType, fileContent));
        } else if (isModelContent(modelName, modelType, fileContent)) {
            modelService.contentFilenameToModelName(modelName, modelType)
                    .ifPresent(fixedModelName -> projectHolder.addModelContent(fixedModelName, modelType, fileContent));
        } else {
            if (modelName.endsWith(modelType.getExtensionsFileSuffix())) {
                modelName = StringUtils.removeEnd(modelName, modelType.getExtensionsFileSuffix());
            }
            projectHolder.addModelJsonFile(modelName, modelType, fileContent);
        }
    }

    private boolean isProjectExtension(String modelName,
                                       ModelType modelType,
                                       FileContent fileContent) {
        return fileContent.isJson() && (modelName.endsWith(modelType.getExtensionsFileSuffix()));
    }

    private boolean isProcessContent(String modelName,
                                     ModelType modelType,
                                     FileContent fileContent) {
        return !fileContent.isJson() || (!modelName.endsWith(modelType.getExtensionsFileSuffix())
                && modelTypeService.isProcessContnent(modelType));
    }

    private boolean isModelContent(String modelName,
                                   ModelType modelType,
                                   FileContent fileContent) {
        return !fileContent.isJson() || (!modelName.endsWith(modelType.getExtensionsFileSuffix())
                && modelTypeService.isContentXML(modelType));
    }

    @Override
    public void validateProject(Project project) {
        List<Model> availableModels = modelService.getAllModels(project);
        ValidationContext validationContext = new ProjectValidationContext(availableModels);

        List<ModelValidationError> validationErrors = Stream.concat(projectValidators.stream().flatMap(validator -> validator.validate(project,
                validationContext)),
                availableModels.stream().flatMap(model -> getModelValidationErrors(model,
                        validationContext)))
                .collect(Collectors.toList());

        if (!validationErrors.isEmpty()) {
            throw new SemanticModelValidationException("Validation errors found in project's models",
                    validationErrors);
        }
    }

    private Stream<ModelValidationError> getModelValidationErrors(Model model,
                                                                  ValidationContext validationContext) {
        List<ModelValidationError> validationErrors = new ArrayList<>();
        try {
            modelService.validateModelContent(model,
                    validationContext);
        } catch (SemanticModelValidationException validationException) {
            validationErrors.addAll(validationException.getValidationErrors());
        }

        try {
            modelService.getModelExtensionsFileContent(model).ifPresent(extensionsFileContent -> modelService.validateModelExtensions(model,
                    extensionsFileContent,
                    validationContext));
        } catch (SemanticModelValidationException validationException) {
            validationErrors.addAll(validationException.getValidationErrors());
        }

        return validationErrors.stream();
    }
}
