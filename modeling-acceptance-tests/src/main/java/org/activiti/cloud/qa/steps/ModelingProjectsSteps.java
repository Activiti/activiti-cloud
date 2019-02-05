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
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

import feign.Response;
import net.thucydides.core.annotations.Step;
import org.activiti.cloud.organization.api.Project;
import org.activiti.cloud.organization.api.Model;
import org.activiti.cloud.organization.api.ModelType;
import org.activiti.cloud.qa.config.ModelingTestsConfigurationProperties;
import org.activiti.cloud.qa.model.modeling.EnableModelingContext;
import org.activiti.cloud.qa.model.modeling.ModelingIdentifier;
import org.activiti.cloud.qa.service.ModelingProjectsService;
import org.activiti.cloud.services.common.util.ContentTypeUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;

import static org.activiti.cloud.services.common.util.ContentTypeUtils.setExtension;
import static org.activiti.cloud.services.common.util.ContentTypeUtils.toJsonFilename;
import static org.activiti.cloud.services.test.asserts.AssertFileContent.assertThatFileContent;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_OK;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.hateoas.Link.REL_SELF;

/**
 * Modeling projects steps
 */
@EnableModelingContext
public class ModelingProjectsSteps extends ModelingContextSteps<Project> {

    @Autowired
    private ModelingProjectsService modelingProjectService;

    @Autowired
    private ModelingTestsConfigurationProperties config;

    @Step
    public Resource<Project> create(String projectName) {
        String id = UUID.randomUUID().toString();
        Project project = mock(Project.class);
        doReturn(id).when(project).getId();
        doReturn(projectName).when(project).getName();
        return create(id,
                      project);
    }

    @Step
    public void updateProjectName(String newProjectName) {
        Resource<Project> currentContext = checkAndGetCurrentContext(Project.class);
        Project project = currentContext.getContent();
        project.setName(newProjectName);

        modelingProjectService.updateByUri(currentContext.getLink(REL_SELF).getHref()
                        .replace("http://activiti-cloud-modeling-backend", config.getModelingUrl()),
                                           project);
    }

    @Step
    public void checkCurrentProjectName(String projectName) {
        updateCurrentModelingObject();
        Resource<Project> currentContext = checkAndGetCurrentContext(Project.class);
        assertThat(currentContext.getContent().getName()).isEqualTo(projectName);
    }

    @Step
    public void checkProjectNotFound(ModelingIdentifier identifier) {
        assertThat(findAll().getContent()
                           .stream()
                           .map(Resource::getContent)
                           .filter(identifier)
                           .findAny())
                .isEmpty();
    }

    @Step
    public Resource<Model> importModelInCurrentProject(File file) {
        Resource<Project> currentProject = checkAndGetCurrentContext(Project.class);
        Link importModelLink = currentProject.getLink("import");
        assertThat(importModelLink).isNotNull();

        return modelingProjectService.importProjectModelByUri(importModelLink.getHref().replace("http://activiti-cloud-modeling-backend", config.getModelingUrl()),
                                                              file);
    }

    @Step
    public void checkCurrentProjectExport() throws IOException {
        Response response = exportCurrentProject();
        assertThat(response.status()).isEqualTo(SC_OK);
        modelingContextHandler.setCurrentModelingFile(toFileContent(response));
    }

    @Step
    public void checkCurrentProjectExportFails(String errorMessage) throws IOException {
        Response response = exportCurrentProject();
        assertThat(response.status()).isEqualTo(SC_BAD_REQUEST);
        assertThat(IOUtils.toString(response.body().asInputStream(),
                                    StandardCharsets.UTF_8)).contains(errorMessage);
    }

    private Response exportCurrentProject() {
        Resource<Project> currentProject = checkAndGetCurrentContext(Project.class);
        Link exportLink = currentProject.getLink("export");
        assertThat(exportLink).isNotNull();
        return modelingProjectService.exportProjectByUri(exportLink.getHref().replace("http://activiti-cloud-modeling-backend",
                                                                                      config.getModelingUrl()));
    }

    @Step
    public void checkExportedProjectContainsModel(ModelType modelType,
                                                  String modelName) {
        Project currentProject = checkAndGetCurrentContext(Project.class).getContent();
        assertThat(modelingContextHandler.getCurrentModelingFile()).hasValueSatisfying(
                fileContent -> assertThatFileContent(fileContent)
                        .hasName(currentProject.getName() + ".zip")
                        .hasContentType(ContentTypeUtils.CONTENT_TYPE_ZIP)
                        .isZip()
                        .hasEntries(
                                toJsonFilename(currentProject.getName()),
                                modelType.getFolderName() + "/",
                                modelType.getFolderName() + "/" + toJsonFilename(modelName),
                                modelType.getFolderName() + "/" + setExtension(modelName,
                                                                               modelType.getContentFileExtension())
                        )
                        .hasJsonContentSatisfying(toJsonFilename(currentProject.getName()),
                                                  jsonContent -> jsonContent
                                                          .node("name").isEqualTo(currentProject.getName()))
                        .hasJsonContentSatisfying(modelType.getFolderName() + "/" + toJsonFilename(modelName),
                                                  jsonContent -> jsonContent
                                                          .node("name").isEqualTo(modelName)));
    }

    @Override
    protected Optional<String> getRel() {
        return Optional.empty();
    }

    @Override
    public ModelingProjectsService service() {
        return modelingProjectService;
    }
}
