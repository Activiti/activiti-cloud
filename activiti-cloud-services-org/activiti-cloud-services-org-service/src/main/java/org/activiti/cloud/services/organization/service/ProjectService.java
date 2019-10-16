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

import static org.activiti.cloud.services.common.util.ContentTypeUtils.JSON;
import static org.activiti.cloud.services.common.util.ContentTypeUtils.getContentTypeByPath;
import static org.activiti.cloud.services.common.util.ContentTypeUtils.removeExtension;
import static org.activiti.cloud.services.common.util.ContentTypeUtils.toJsonFilename;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.transaction.Transactional;

import org.activiti.cloud.organization.api.Model;
import org.activiti.cloud.organization.api.ModelType;
import org.activiti.cloud.organization.api.ModelValidationError;
import org.activiti.cloud.organization.api.Project;
import org.activiti.cloud.organization.api.ValidationContext;
import org.activiti.cloud.organization.converter.JsonConverter;
import org.activiti.cloud.organization.core.error.ImportProjectException;
import org.activiti.cloud.organization.core.error.SemanticModelValidationException;
import org.activiti.cloud.organization.repository.ProjectRepository;
import org.activiti.cloud.services.common.file.FileContent;
import org.activiti.cloud.services.common.zip.ZipBuilder;
import org.activiti.cloud.services.common.zip.ZipStream;
import org.activiti.cloud.services.organization.validation.ProjectValidationContext;
import org.activiti.cloud.services.organization.validation.project.ProjectValidator;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.multipart.MultipartFile;

/**
 * Business logic related to {@link Project} entities
 */
@PreAuthorize("hasRole('ACTIVITI_MODELER')")
@Transactional
public class ProjectService {

    private final ProjectRepository projectRepository;

    private final ModelService modelService;

    private final ModelTypeService modelTypeService;

    private final JsonConverter<Project> jsonConverter;

    private final JsonConverter<Map> jsonMetadataConverter;

    private final Set<ProjectValidator> projectValidators;

    @Autowired
    public ProjectService(ProjectRepository projectRepository,
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
    public Page<Project> getProjects(Pageable pageable,
                                     String name) {
        return projectRepository.getProjects(pageable,
                                             name);
    }

    /**
     * Create an project.
     * 
     * @param project the project to create
     * @return the created project
     */
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
    public FileContent exportProject(Project project) throws IOException {
        validateProject(project);

        ZipBuilder zipBuilder = new ZipBuilder(project.getName()).appendFile(jsonConverter.convertToJsonBytes(project),
                                                                             toJsonFilename(project.getName()));
        modelService.getAllModels(project).forEach(model -> modelTypeService.findModelTypeByName(model.getType()).map(ModelType::getFolderName).ifPresent(folderName -> {
            zipBuilder.appendFolder(folderName).appendFile(modelService.exportModel(model),
                                                           folderName);
            modelService.getModelExtensionsFileContent(model).ifPresent(extensionFileContent -> zipBuilder.appendFile(extensionFileContent,
                                                                                                                      folderName));
        }));
        return zipBuilder.toZipFileContent();
    }

    /**
     * Import an project form a zip multipart file.
     * 
     * @param file the multipart zip file to import from
     * @return the imported project
     * @throws IOException in case of multipart file input stream access error
     */
    public Project importProject(MultipartFile file) throws IOException {
        ProjectHolder projectHolder = new ProjectHolder();

        ZipStream.of(file)
                .forEach(zipEntry -> zipEntry.getContent()
                        .ifPresent(bytes -> getContentTypeByPath(zipEntry.getFileName()).map(contentType -> new FileContent(zipEntry.getFileName(),
                                                                                                                            contentType,
                                                                                                                            bytes))
                                .ifPresent(fileContent -> {
                                    Optional<String> folderName = zipEntry.getFolderName(0);
                                    if (folderName.isPresent()) {
                                        folderName.flatMap(modelTypeService::findModelTypeByFolderName).ifPresent(modelType -> processZipEntryFile(projectHolder,
                                                                                                                                                   fileContent,
                                                                                                                                                   modelType));
                                    } else if (fileContent.isJson()) {
                                        jsonConverter.tryConvertToEntity(bytes).ifPresent(projectHolder::setProject);
                                    }
                                })));

        Project createdProject = projectHolder.getProjectMetadata().map(this::createProject)
                .orElseThrow(() -> new ImportProjectException("No valid project entry found to import: " + file.getOriginalFilename()));

        projectHolder.getModelJsonFiles().forEach(modelJsonFile -> {
            Model createdModel = modelService.importModel(createdProject,
                                                          modelJsonFile.getModelType(),
                                                          modelJsonFile.getFileContent());
            if (modelTypeService.isJson(modelJsonFile.getModelType())) {
                modelService.updateModelContent(createdModel,
                                                modelJsonFile.getFileContent());
            } else {
                projectHolder.getModelContentFile(createdModel).ifPresent(fileContent -> modelService.updateModelContent(createdModel,
                                                                                                                         fileContent));
            }
            //Update model with metadata
            projectHolder.getModelExtensions(createdModel).ifPresent(fileMetadata -> {
                jsonMetadataConverter.tryConvertToEntity(fileMetadata.getFileContent()).ifPresent(createdModel::setExtensions);
                modelService.updateModel(createdModel,
                                         createdModel);
            });
        });
        modelService.cleanModelIdList();
        return createdProject;
    }

    private void processZipEntryFile(ProjectHolder projectHolder,
                                     FileContent fileContent,
                                     ModelType modelType) {
        String modelName = removeExtension(fileContent.getFilename(),
                                           JSON);
        if (isProjectExtension(modelName,
                               modelType,
                               fileContent)) {
            modelName = StringUtils.removeEnd(modelName,
                                              modelType.getExtensionsFileSuffix());
            projectHolder.addModelExtension(modelName,
                                            modelType,
                                            fileContent);
        } else if (isProjectContent(modelName,
                                    modelType,
                                    fileContent)) {
            modelService.contentFilenameToModelName(modelName,
                                                    modelType)
                    .ifPresent(fixedModelName -> projectHolder.addModelContent(fixedModelName,
                                                                               modelType,
                                                                               fileContent));
        } else {
            if (modelName.endsWith(modelType.getExtensionsFileSuffix())) {
                modelName = StringUtils.removeEnd(modelName,
                                                  modelType.getExtensionsFileSuffix());
            }
            projectHolder.addModelJsonFile(modelName,
                                           modelType,
                                           fileContent);
        }
    }

    private boolean isProjectExtension(String modelName,
                                       ModelType modelType,
                                       FileContent fileContent) {
        return fileContent.isJson() && (modelName.endsWith(modelType.getExtensionsFileSuffix()) && modelTypeService.isJson(modelType));
    }

    private boolean isProjectContent(String modelName,
                                     ModelType modelType,
                                     FileContent fileContent) {
        return !fileContent.isJson() || (!modelName.endsWith(modelType.getExtensionsFileSuffix()) && !modelTypeService.isJson(modelType));
    }

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
