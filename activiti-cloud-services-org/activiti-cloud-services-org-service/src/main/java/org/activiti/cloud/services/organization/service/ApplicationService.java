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
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.activiti.cloud.organization.api.Application;
import org.activiti.cloud.organization.api.Model;
import org.activiti.cloud.organization.api.ModelType;
import org.activiti.cloud.organization.api.ModelValidationError;
import org.activiti.cloud.organization.converter.JsonConverter;
import org.activiti.cloud.organization.core.error.ImportApplicationException;
import org.activiti.cloud.organization.core.error.SemanticModelValidationException;
import org.activiti.cloud.organization.repository.ApplicationRepository;
import org.activiti.cloud.services.common.file.FileContent;
import org.activiti.cloud.services.common.zip.ZipBuilder;
import org.activiti.cloud.services.common.zip.ZipStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import static org.activiti.cloud.services.common.util.ContentTypeUtils.JSON;
import static org.activiti.cloud.services.common.util.ContentTypeUtils.getContentTypeByPath;
import static org.activiti.cloud.services.common.util.ContentTypeUtils.isJsonContentType;
import static org.activiti.cloud.services.common.util.ContentTypeUtils.removeExtension;
import static org.activiti.cloud.services.common.util.ContentTypeUtils.toJsonFilename;

/**
 * Business logic related to {@link Application} entities
 */
@Service
@PreAuthorize("hasRole('ACTIVITI_MODELER')")
public class ApplicationService {

    private final ApplicationRepository applicationRepository;

    private final ModelService modelService;

    private final ModelTypeService modelTypeService;

    private final JsonConverter<Application> jsonConverter;

    @Autowired
    public ApplicationService(ApplicationRepository applicationRepository,
                              ModelService modelService,
                              ModelTypeService modelTypeService,
                              JsonConverter<Application> jsonConverter) {
        this.applicationRepository = applicationRepository;
        this.modelService = modelService;
        this.modelTypeService = modelTypeService;
        this.jsonConverter = jsonConverter;
    }

    /**
     * Get a page of applications.
     * @param pageable the pagination information
     * @return the page
     */
    public Page<Application> getApplications(Pageable pageable) {
        return applicationRepository.getApplications(pageable);
    }

    /**
     * Create an application.
     * @param application the application to create
     * @return the created application
     */
    public Application createApplication(Application application) {
        return applicationRepository.createApplication(application);
    }

    /**
     * Update an application.
     * @param applicationToUpdate the application to update
     * @param newApplication the application containing the new values to be used for update
     * @return the the updated application
     */
    public Application updateApplication(Application applicationToUpdate,
                                         Application newApplication) {
        applicationToUpdate.setName(newApplication.getName());
        return applicationRepository.updateApplication(applicationToUpdate);
    }

    /**
     * Delete an application.
     * @param application the application to be deleted
     */
    public void deleteApplication(Application application) {
        modelService.getAllModels(application)
                .forEach(modelService::deleteModel);
        applicationRepository.deleteApplication(application);
    }

    /**
     * Find an application by id.
     * @param applicationId the id to search for
     * @return the found application, or {@literal Optional#empty()}
     */
    public Optional<Application> findApplicationById(String applicationId) {
        return applicationRepository.findApplicationById(applicationId);
    }

    /**
     * Export an application to a zip file.
     * @param application the application to export
     * @return the {@link FileContent} with zip content
     * @throws IOException in case of I/O error
     */
    public FileContent exportApplication(Application application) throws IOException {
        validateApplication(application);

        ZipBuilder zipBuilder = new ZipBuilder(application.getName())
                .appendFile(jsonConverter.convertToJsonBytes(application),
                            toJsonFilename(application.getName()));
        modelService.getAllModels(application)
                .forEach(model -> modelTypeService.findModelTypeByName(model.getType())
                        .map(ModelType::getFolderName)
                        .ifPresent(folderName -> {
                            zipBuilder
                                    .appendFolder(folderName)
                                    .appendFile(modelService.exportModel(model),
                                                folderName);
                            if (!isJsonContentType(model.getContentType())) {
                                zipBuilder.appendFile(modelService.getModelMetadataFileContent(model),
                                                      folderName);
                            }
                        }));
        return zipBuilder.toZipFileContent();
    }

    /**
     * Import an application form a zip multipart file.
     * @param file the multipart zip file to import from
     * @return the imported application
     * @throws IOException in case of multipart file input stream access error
     */
    public Application importApplication(MultipartFile file) throws IOException {
        ApplicationHolder applicationHolder = new ApplicationHolder();

        ZipStream.of(file).forEach(zipEntry -> zipEntry.getContent()
                .ifPresent(bytes -> getContentTypeByPath(zipEntry.getFileName())
                        .map(contentType -> new FileContent(zipEntry.getFileName(),
                                                            contentType,
                                                            bytes))
                        .ifPresent(fileContent -> {
                            Optional<String> folderName = zipEntry.getFolderName(0);
                            if (folderName.isPresent()) {
                                folderName.flatMap(modelTypeService::findModelTypeByFolderName).ifPresent(modelType -> {
                                    if (fileContent.isJson()) {
                                        String modelName = removeExtension(fileContent.getFilename(),
                                                                           JSON);
                                        applicationHolder.addModelJsonFile(modelName,
                                                                           modelType,
                                                                           fileContent);
                                    } else {
                                        modelService.contentFilenameToModelName(zipEntry.getFileName(),
                                                                                modelType)
                                                .ifPresent(modelName -> applicationHolder.addModelContent(modelName,
                                                                                                          fileContent));
                                    }
                                });
                            } else if (fileContent.isJson()) {
                                jsonConverter.tryConvertToEntity(bytes)
                                        .ifPresent(applicationHolder::setApplication);
                            }
                        })));

        Application createdApplication = applicationHolder.getApplicationMetadata()
                .map(this::createApplication)
                .orElseThrow(() -> new ImportApplicationException("No valid application entry found to import: " + file.getOriginalFilename()));

        applicationHolder.getModelJsonFiles().forEach(modelJsonFile -> {
            if (modelTypeService.isJson(modelJsonFile.getModelType())) {
                modelService.importModel(createdApplication,
                                         modelJsonFile.getModelType(),
                                         modelJsonFile.getFileContent());
            } else {
                Model createdModel = modelService.importJsonModel(createdApplication,
                                                                  modelJsonFile.getModelType(),
                                                                  modelJsonFile.getFileContent());
                applicationHolder.getModelContentFile(createdModel)
                        .ifPresent(fileContent -> modelService.updateModelContent(createdModel,
                                                                                  fileContent));
            }
        });
        return createdApplication;
    }

    public void validateApplication(Application application) {
        List<ModelValidationError> validationErrors = modelService.getAllModels(application)
                .stream()
                .flatMap(this::getModelValidationErrors)
                .collect(Collectors.toList());

        if (!validationErrors.isEmpty()) {
            throw new SemanticModelValidationException("Validation errors found in application's models",
                                                       validationErrors);
        }
    }

    private Stream<ModelValidationError> getModelValidationErrors(Model model) {
        try {
            modelService.validateModelContent(model);
            return Stream.empty();
        } catch (SemanticModelValidationException validationException) {
            return validationException.getValidationErrors().stream();
        }
    }
}
