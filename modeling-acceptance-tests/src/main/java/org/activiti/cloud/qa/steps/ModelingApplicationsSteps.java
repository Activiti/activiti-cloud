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

package org.activiti.cloud.qa.steps;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import feign.Response;
import net.thucydides.core.annotations.Step;
import org.activiti.cloud.organization.api.Application;
import org.activiti.cloud.organization.api.Model;
import org.activiti.cloud.organization.api.ModelType;
import org.activiti.cloud.qa.config.ModelingTestsConfigurationProperties;
import org.activiti.cloud.qa.model.modeling.EnableModelingContext;
import org.activiti.cloud.qa.model.modeling.ModelingIdentifier;
import org.activiti.cloud.qa.service.ModelingApplicationsService;
import org.activiti.cloud.services.common.util.ContentTypeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;

import static org.activiti.cloud.services.common.util.ContentTypeUtils.setExtension;
import static org.activiti.cloud.services.common.util.ContentTypeUtils.toJsonFilename;
import static org.activiti.cloud.services.test.asserts.AssertFileContent.assertThatFileContent;
import static org.apache.http.HttpStatus.SC_OK;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.hateoas.Link.REL_SELF;

/**
 * Modeling applications steps
 */
@EnableModelingContext
public class ModelingApplicationsSteps extends ModelingContextSteps<Application> {

    @Autowired
    private ModelingApplicationsService modelingApplicationsService;

    @Autowired
    private ModelingTestsConfigurationProperties config;

    @Step
    public Resource<Application> create(String applicationName) {
        String id = UUID.randomUUID().toString();
        Application application = mock(Application.class);
        doReturn(id).when(application).getId();
        doReturn(applicationName).when(application).getName();
        return create(id,
                      application);
    }

    @Step
    public void updateApplicationName(String newApplicationName) {
        Resource<Application> currentContext = checkAndGetCurrentContext(Application.class);
        Application application = currentContext.getContent();
        application.setName(newApplicationName);

        modelingApplicationsService.updateByUri(currentContext.getLink(REL_SELF).getHref()
                        .replace("http://activiti-cloud-modeling-backend", config.getModelingUrl()),
                                                application);
    }

    @Step
    public void checkCurrentApplicationName(String applicationName) {
        updateCurrentModelingObject();
        Resource<Application> currentContext = checkAndGetCurrentContext(Application.class);
        assertThat(currentContext.getContent().getName()).isEqualTo(applicationName);
    }

    @Step
    public void checkApplicationNotFound(ModelingIdentifier identifier) {
        assertThat(findAll().getContent()
                           .stream()
                           .map(Resource::getContent)
                           .filter(identifier)
                           .findAny())
                .isEmpty();
    }

    @Step
    public Resource<Model> importModelInCurrentApplication(File file) {
        Resource<Application> currentApplication = checkAndGetCurrentContext(Application.class);
        Link importModelLink = currentApplication.getLink("import");
        assertThat(importModelLink).isNotNull();

        return modelingApplicationsService.importApplicationModelByUri(importModelLink.getHref().replace("http://activiti-cloud-modeling-backend", config.getModelingUrl()),
                                                                       file);
    }

    @Step
    public void exportCurrentApplication() throws IOException {
        Resource<Application> currentApplication = checkAndGetCurrentContext(Application.class);
        Link exportLink = currentApplication.getLink("export");
        assertThat(exportLink).isNotNull();
        Response response = modelingApplicationsService.exportApplicationByUri(exportLink.getHref().replace("http://activiti-cloud-modeling-backend", config.getModelingUrl()));

        if (response.status() == SC_OK) {
            modelingContextHandler.setCurrentModelingFile(toFileContent(response));
        }
    }

    @Step
    public void checkExportedApplicationContainsModel(ModelType modelType,
                                                      String modelName) {
        Application currentApplication = checkAndGetCurrentContext(Application.class).getContent();
        assertThat(modelingContextHandler.getCurrentModelingFile()).hasValueSatisfying(
                fileContent -> assertThatFileContent(fileContent)
                        .hasName(currentApplication.getName() + ".zip")
                        .hasContentType(ContentTypeUtils.CONTENT_TYPE_ZIP)
                        .isZip()
                        .hasEntries(
                                toJsonFilename(currentApplication.getName()),
                                modelType.getFolderName() + "/",
                                modelType.getFolderName() + "/" + toJsonFilename(modelName),
                                modelType.getFolderName() + "/" + setExtension(modelName,
                                                                               modelType.getContentFileExtension())
                        )
                        .hasJsonContentSatisfying(toJsonFilename(currentApplication.getName()),
                                                  jsonContent -> jsonContent
                                                          .node("name").isEqualTo(currentApplication.getName()))
                        .hasJsonContentSatisfying(modelType.getFolderName() + "/" + toJsonFilename(modelName),
                                                  jsonContent -> jsonContent
                                                          .node("name").isEqualTo(modelName)));
    }

    @Override
    protected Optional<String> getRel() {
        return Optional.empty();
    }

    @Override
    public ModelingApplicationsService service() {
        return modelingApplicationsService;
    }
}
