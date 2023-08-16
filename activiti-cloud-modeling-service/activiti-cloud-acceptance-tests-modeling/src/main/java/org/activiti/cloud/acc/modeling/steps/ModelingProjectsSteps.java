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
package org.activiti.cloud.acc.modeling.steps;

import static org.activiti.cloud.acc.modeling.modeling.ProcessExtensions.EXTENSIONS_TASK_NAME;
import static org.activiti.cloud.services.common.util.ContentTypeUtils.changeExtension;
import static org.activiti.cloud.services.common.util.ContentTypeUtils.changeToJsonFilename;
import static org.activiti.cloud.services.test.asserts.AssertFileContent.assertThatFileContent;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_OK;
import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.hamcrest.core.AllOf.allOf;
import static org.hamcrest.core.StringStartsWith.startsWith;
import static org.mockito.Mockito.*;
import static org.springframework.hateoas.IanaLinkRelations.SELF;

import feign.Response;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.thucydides.core.annotations.Step;
import org.activiti.cloud.acc.modeling.modeling.EnableModelingContext;
import org.activiti.cloud.acc.modeling.modeling.ModelingContextHandler;
import org.activiti.cloud.acc.modeling.modeling.ModelingIdentifier;
import org.activiti.cloud.acc.modeling.service.ModelingProjectsService;
import org.activiti.cloud.modeling.api.Model;
import org.activiti.cloud.modeling.api.ModelType;
import org.activiti.cloud.modeling.api.Project;
import org.activiti.cloud.services.common.util.ContentTypeUtils;
import org.hamcrest.Matcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.util.StreamUtils;

/**
 * Modeling projects steps
 */
@EnableModelingContext
public class ModelingProjectsSteps extends ModelingContextSteps<Project> {

    @Autowired
    private ModelingProjectsService modelingProjectService;

    @Autowired
    private ModelingContextHandler modelingContextHandler;

    @Step
    public void findByName(String name) {
        modelingContextHandler.setCurrentProjects(service().findAllByName(name).getContent());
    }

    @Step
    public void checkCurrentProjects(List<String> expectedNames) {
        assertThat(
            modelingContextHandler
                .getCurrentProjects()
                .map(resources -> resources.stream().map(EntityModel::getContent).map(Project::getTechnicalName))
                .orElseGet(Stream::empty)
                .collect(Collectors.toList())
        )
            .containsExactlyInAnyOrder(expectedNames.toArray(new String[0]));
    }

    @Step
    public EntityModel<Project> create(String projectName) {
        Project project = mock(Project.class);
        doReturn(projectName).when(project).getTechnicalName();
        return create(project);
    }

    @Step
    public void updateProjectName(String newProjectName) {
        EntityModel<Project> currentContext = checkAndGetCurrentContext(Project.class);
        Project project = currentContext.getContent();
        project.setTechnicalName(newProjectName);

        modelingProjectService.updateByUri(modelingUri(currentContext.getLink(SELF).get().getHref()), project);
    }

    @Step
    public void checkCurrentProjectName(String projectName) {
        updateCurrentModelingObject();
        EntityModel<Project> currentContext = checkAndGetCurrentContext(Project.class);
        assertThat(currentContext.getContent().getTechnicalName()).isEqualTo(projectName);
    }

    @Step
    public void checkProjectNotFound(ModelingIdentifier identifier) {
        assertThat(findAll().getContent().stream().map(EntityModel::getContent).filter(identifier).findAny()).isEmpty();
    }

    @Step
    public EntityModel<Model> importModelInCurrentProject(File file) {
        EntityModel<Project> currentProject = checkAndGetCurrentContext(Project.class);
        Link importModelLink = currentProject.getLink("import").get();
        assertThat(importModelLink).isNotNull();

        return modelingProjectService.importProjectModelByUri(modelingUri(importModelLink.getHref()), file);
    }

    @Step
    public void checkCurrentProjectExport() throws IOException {
        Response response = exportCurrentProject();
        assertThat(response.status()).isEqualTo(SC_OK);
        modelingContextHandler.setCurrentModelingFile(toFileContent(response));
    }

    @Step
    public void checkCurrentProjectValidate() throws IOException {
        Response response = validateCurrentProject();
        assertThat(response.status()).isEqualTo(SC_OK);
        modelingContextHandler.setCurrentModelingFile(toFileContent(response));
    }

    @Step
    public void checkCurrentProjectExportNotFail(String errorMessage) throws IOException {
        Response response = exportCurrentProject();
        assertThat(response.status()).isEqualTo(SC_OK);
        modelingContextHandler.setCurrentModelingFile(toFileContent(response));
    }

    @Step
    public void checkCurrentProjectValidationFails(String errorMessage) throws IOException {
        Response response = validateCurrentProject();
        assertThat(response.status()).isEqualTo(SC_BAD_REQUEST);
        assertThat(StreamUtils.copyToString(response.body().asInputStream(), StandardCharsets.UTF_8))
            .contains(errorMessage);
    }

    private Response exportCurrentProject() {
        EntityModel<Project> currentProject = checkAndGetCurrentContext(Project.class);
        Link exportLink = currentProject.getLink("export").get();
        assertThat(exportLink).isNotNull();
        return modelingProjectService.exportProjectByUri(modelingUri(exportLink.getHref()));
    }

    private Response validateCurrentProject() {
        EntityModel<Project> currentProject = checkAndGetCurrentContext(Project.class);
        Link exportLink = currentProject.getLink("export").get();
        String validateLink = exportLink.getHref().replace("/export", "/validate");
        assertThat(validateLink).isNotNull();
        return modelingProjectService.validateProjectByUri(modelingUri(validateLink));
    }

    @Step
    public void checkExportedProjectContainsModel(
        ModelType modelType,
        String modelName,
        List<String> processVariables
    ) {
        Project currentProject = checkAndGetCurrentContext(Project.class).getContent();
        assertThat(modelingContextHandler.getCurrentModelingFile())
            .hasValueSatisfying(fileContent ->
                assertThatFileContent(fileContent)
                    .hasName(currentProject.getTechnicalName() + ".zip")
                    .hasContentType(ContentTypeUtils.CONTENT_TYPE_ZIP)
                    .isZip()
                    .hasEntries(
                        changeToJsonFilename(currentProject.getTechnicalName()),
                        modelType.getFolderName() + "/",
                        modelType.getFolderName() +
                        "/" +
                        changeToJsonFilename(modelName + modelType.getExtensionsFileSuffix()),
                        modelType.getFolderName() +
                        "/" +
                        changeExtension(modelName, modelType.getContentFileExtension())
                    )
                    .hasJsonContentSatisfying(
                        changeToJsonFilename(currentProject.getTechnicalName()),
                        jsonContent -> jsonContent.node("name").isEqualTo(currentProject.getTechnicalName())
                    )
                    .hasJsonContentSatisfying(
                        modelType.getFolderName() +
                        "/" +
                        changeToJsonFilename(modelName + modelType.getExtensionsFileSuffix()),
                        jsonContent -> {
                            jsonContent.node("id").matches(startsWith("process-"));
                            jsonContent.node("name").isEqualTo(modelName);
                            processVariables.forEach(processVariable -> {
                                jsonContent
                                    .node("extensions." + modelName + ".properties")
                                    .matches(
                                        hasEntry(
                                            equalTo(processVariable),
                                            allOf(
                                                hasEntry(equalTo("id"), equalTo(processVariable)),
                                                hasEntry(equalTo("name"), equalTo(processVariable)),
                                                hasEntry(equalTo("type"), equalTo("boolean")),
                                                hasEntry(equalTo("value"), is(true))
                                            )
                                        )
                                    );
                                jsonContent
                                    .node("extensions." + modelName + ".mappings")
                                    .matches(
                                        hasEntry(
                                            equalTo(EXTENSIONS_TASK_NAME),
                                            allOf(
                                                hasEntry(
                                                    equalTo("inputs"),
                                                    hasEntry(
                                                        equalTo(processVariable),
                                                        allOf(
                                                            hasEntry(equalTo("type"), equalTo("value")),
                                                            hasEntry(equalTo("value"), equalTo(processVariable))
                                                        )
                                                    )
                                                ),
                                                hasEntry(
                                                    equalTo("outputs"),
                                                    hasEntry(
                                                        equalTo(processVariable),
                                                        allOf(
                                                            hasEntry(equalTo("type"), equalTo("variable")),
                                                            hasEntry(equalTo("value"), equalTo("${host}"))
                                                        )
                                                    )
                                                )
                                            )
                                        )
                                    );
                            });
                        }
                    )
            );
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
