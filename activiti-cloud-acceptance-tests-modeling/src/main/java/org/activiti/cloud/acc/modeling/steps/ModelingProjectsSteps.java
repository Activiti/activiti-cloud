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

package org.activiti.cloud.acc.modeling.steps;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import feign.Response;
import net.thucydides.core.annotations.Step;
import org.activiti.cloud.acc.modeling.config.ModelingTestsConfigurationProperties;
import org.activiti.cloud.acc.modeling.modeling.EnableModelingContext;
import org.activiti.cloud.acc.modeling.modeling.ModelingIdentifier;
import org.activiti.cloud.acc.modeling.service.ModelingProjectsService;
import org.activiti.cloud.organization.api.Model;
import org.activiti.cloud.organization.api.ModelType;
import org.activiti.cloud.organization.api.Project;
import org.activiti.cloud.services.common.util.ContentTypeUtils;
import org.apache.commons.io.IOUtils;
import org.hamcrest.Matcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;

import static org.activiti.cloud.acc.modeling.modeling.ProcessExtensions.EXTENSIONS_TASK_NAME;
import static org.activiti.cloud.services.common.util.ContentTypeUtils.setExtension;
import static org.activiti.cloud.services.common.util.ContentTypeUtils.toJsonFilename;
import static org.activiti.cloud.services.test.asserts.AssertFileContent.assertThatFileContent;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_OK;
import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.hamcrest.core.AllOf.allOf;
import static org.hamcrest.core.StringStartsWith.startsWith;
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
        Project project = mock(Project.class);
        doReturn(projectName).when(project).getName();
        return create(project);
    }

    @Step
    public void updateProjectName(String newProjectName) {
        Resource<Project> currentContext = checkAndGetCurrentContext(Project.class);
        Project project = currentContext.getContent();
        project.setName(newProjectName);

        modelingProjectService.updateByUri(modelingUri(currentContext.getLink(REL_SELF).getHref()),
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

        return modelingProjectService.importProjectModelByUri(modelingUri(importModelLink.getHref()),
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
        return modelingProjectService.exportProjectByUri(modelingUri(exportLink.getHref()));
    }

    @Step
    public void checkExportedProjectContainsModel(ModelType modelType,
                                                  String modelName,
                                                  List<String> processVariables) {
        Project currentProject = checkAndGetCurrentContext(Project.class).getContent();
        assertThat(modelingContextHandler.getCurrentModelingFile()).hasValueSatisfying(
                fileContent -> assertThatFileContent(fileContent)
                        .hasName(currentProject.getName() + ".zip")
                        .hasContentType(ContentTypeUtils.CONTENT_TYPE_ZIP)
                        .isZip()
                        .hasEntries(
                                toJsonFilename(currentProject.getName()),
                                modelType.getFolderName() + "/",
                                modelType.getFolderName() + "/" + toJsonFilename(modelName + modelType.getMetadataFileSuffix()),
                                modelType.getFolderName() + "/" + setExtension(modelName,
                                                                               modelType.getContentFileExtension())
                        )
                        .hasJsonContentSatisfying(toJsonFilename(currentProject.getName()),
                                                  jsonContent -> jsonContent
                                                          .node("name").isEqualTo(currentProject.getName()))
                        .hasJsonContentSatisfying(
                                modelType.getFolderName() + "/" + toJsonFilename(modelName + modelType.getMetadataFileSuffix()),
                                jsonContent -> {
                                    jsonContent.node("id").matches(startsWith("process-"));
                                    jsonContent.node("name").isEqualTo(modelName);
                                    processVariables.forEach(processVariable -> {
                                        jsonContent.node("extensions.properties")
                                                .matches(hasEntry(equalTo(processVariable),
                                                                  allOf(hasEntry(equalTo("id"),
                                                                                 equalTo(processVariable)),
                                                                        hasEntry(equalTo("name"),
                                                                                 equalTo(processVariable)),
                                                                        hasEntry(equalTo("type"),
                                                                                 equalTo("boolean")),
                                                                        hasEntry(equalTo("value"),
                                                                                 is(true))
                                                                  )));
                                        jsonContent.node("extensions.mappings").matches(
                                                hasEntry(equalTo(EXTENSIONS_TASK_NAME),
                                                         allOf(hasEntry(equalTo("inputs"),
                                                                        hasEntry(equalTo(processVariable),
                                                                                 allOf(hasEntry(equalTo("type"),
                                                                                                equalTo("value")),
                                                                                       hasEntry(equalTo("value"),
                                                                                                equalTo(processVariable))
                                                                                 ))),
                                                               hasEntry(equalTo("outputs"),
                                                                        hasEntry(equalTo(processVariable),
                                                                                 allOf(hasEntry(equalTo("type"),
                                                                                                equalTo("variable")),
                                                                                       hasEntry(equalTo("value"),
                                                                                                equalTo("${host}"))
                                                                                 ))
                                                               ))
                                                )
                                        );
                                    });
                                }
                        ));
    }

    @Override
    protected Optional<String> getRel() {
        return Optional.empty();
    }

    @Override
    public ModelingProjectsService service() {
        return modelingProjectService;
    }

    private Matcher<Object> is(boolean operand) {
        return equalTo(operand);
    }
}
