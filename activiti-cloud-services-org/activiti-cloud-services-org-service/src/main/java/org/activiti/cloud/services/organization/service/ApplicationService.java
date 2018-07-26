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
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.activiti.cloud.organization.api.Application;
import org.activiti.cloud.organization.api.Model;
import org.activiti.cloud.organization.api.ModelType;
import org.activiti.cloud.organization.core.error.ModelingException;
import org.activiti.cloud.organization.repository.ApplicationRepository;
import org.activiti.cloud.services.common.file.FileContent;
import org.activiti.cloud.services.common.zip.ZipBuilder;
import org.activiti.cloud.services.common.zip.ZipStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import static org.activiti.cloud.organization.api.ModelType.FORM;
import static org.activiti.cloud.organization.api.ModelType.PROCESS;
import static org.activiti.cloud.services.common.util.ContentTypeUtils.CONTENT_TYPE_JSON;
import static org.activiti.cloud.services.common.util.ContentTypeUtils.CONTENT_TYPE_XML;

/**
 * Business logic related to {@link Application} entities
 */
@Service
public class ApplicationService {

    private final ApplicationRepository applicationRepository;

    private final ModelService modelService;

    @Autowired
    public ApplicationService(ApplicationRepository applicationRepository,
                              ModelService modelService) {
        this.applicationRepository = applicationRepository;
        this.modelService = modelService;
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
     * Create a new instance of {@link Application} with a given name
     * @param name the name of the application to create instance for
     * @return the instance of application
     */
    public Application newApplicationInstance(String name) {
        try {
            Application app = (Application) applicationRepository.getApplicationType().newInstance();
            app.setName(name);
            return app;
        } catch (InstantiationException | IllegalAccessException e) {
            throw new ModelingException("Cannot create application instance", e);
        }
    }

    /**
     * Export an application to a zip file.
     * @param application the application to export
     * @return the {@link FileContent} with zip content
     * @throws IOException in case of I/O error
     */
    public FileContent exportApplication(Application application) throws IOException {
        ZipBuilder zipBuilder = new ZipBuilder(application.getName())
                .appendFolder(application.getName());

        modelService.getModels(application,
                               Pageable.unpaged())
                .getContent()
                .stream()
                .map(Model::getId)
                .map(modelService::exportModel)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .forEach(fileContent -> zipBuilder.appendFile(fileContent.getFileContent(),
                                                              application.getName(),
                                                              fileContent.getFilename()));
        return zipBuilder.toZipFileContent();
    }

    /**
     * Import an application form a zip multipart file.
     * @param file the multipart zip file to import from
     * @return the imported application
     * @throws IOException in case of multipart file input stream access error
     */
    public Application importApplication(MultipartFile file) throws IOException {
        ApplicationBuilder applicationBuilder = new ApplicationBuilder();

        ZipStream.of(file).forEach(zipEntry -> {
            zipEntry.getFolderName(0).ifPresent(applicationName -> {
                applicationBuilder.withApplicationName(applicationName);
                zipEntry.getContent()
                        .ifPresent(bytes -> applicationBuilder.addContent(zipEntry.getFileName(),
                                                                          bytes));
            });
        });

        return applicationBuilder.getApplicationName()
                .map(this::newApplicationInstance)
                .map(app -> {
                    Application createdApplication = applicationRepository.createApplication(app);
                    applicationBuilder.getApplicationMap()
                            .entrySet()
                            .stream()
                            .map(this::toModel)
                            .filter(Optional::isPresent)
                            .map(Optional::get)
                            .forEach(modelToCreate -> modelService.createModel(createdApplication,
                                                                               modelToCreate));
                    return createdApplication;
                })
                .orElseThrow(() -> new ModelingException("No valid application entry found to import: " + file.getOriginalFilename()));
    }

    private Optional<Model> toModel(Map.Entry<String, FileContent> modelMapEntry) {
        String modelName = modelMapEntry.getKey();
        FileContent fileContent = modelMapEntry.getValue();
        //TODO: to detect the model type from file content. For now, just use the content type.
        return getModelType(fileContent.getContentType())
                .map(modelType -> {
                    Model model = modelService.newModelInstance(modelType,
                                                                modelName);
                    model.setName(modelName);
                    model.setType(modelType);
                    model.setContentType(fileContent.getContentType());
                    model.setContent(new String(fileContent.getFileContent()));
                    return model;
                });
    }

    private Optional<ModelType> getModelType(String contentType) {
        return CONTENT_TYPE_JSON.equals(contentType) ? Optional.of(FORM) :
                CONTENT_TYPE_XML.equals(contentType) ? Optional.of(PROCESS) :
                        Optional.empty();
    }
}
