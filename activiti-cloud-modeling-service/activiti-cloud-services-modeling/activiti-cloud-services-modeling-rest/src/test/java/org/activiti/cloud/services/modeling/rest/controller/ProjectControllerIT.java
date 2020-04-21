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

package org.activiti.cloud.services.modeling.rest.controller;

import static org.activiti.cloud.services.common.util.FileUtils.resourceAsByteArray;
import static org.activiti.cloud.services.modeling.asserts.AssertResponse.assertThatResponse;
import static org.activiti.cloud.services.modeling.mock.MockFactory.connectorModel;
import static org.activiti.cloud.services.modeling.mock.MockFactory.extensions;
import static org.activiti.cloud.services.modeling.mock.MockFactory.inputsMappings;
import static org.activiti.cloud.services.modeling.mock.MockFactory.outputsMappings;
import static org.activiti.cloud.services.modeling.mock.MockFactory.processFileContent;
import static org.activiti.cloud.services.modeling.mock.MockFactory.processFileContentWithCallActivity;
import static org.activiti.cloud.services.modeling.mock.MockFactory.processModelWithContent;
import static org.activiti.cloud.services.modeling.mock.MockFactory.processModelWithExtensions;
import static org.activiti.cloud.services.modeling.mock.MockFactory.processVariables;
import static org.activiti.cloud.services.modeling.mock.MockFactory.project;
import static org.activiti.cloud.services.modeling.mock.MockFactory.projectWithDescription;
import static org.activiti.cloud.services.modeling.rest.config.RepositoryRestConfig.API_VERSION;
import static org.activiti.cloud.services.test.asserts.AssertResponseContent.assertThatResponseContent;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.hamcrest.core.AllOf.allOf;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

import java.util.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.activiti.cloud.modeling.api.Model;
import org.activiti.cloud.modeling.api.ModelValidationError;
import org.activiti.cloud.modeling.api.ProcessModelType;
import org.activiti.cloud.modeling.api.Project;
import org.activiti.cloud.modeling.api.process.Extensions;
import org.activiti.cloud.modeling.core.error.SemanticModelValidationException;
import org.activiti.cloud.modeling.repository.ModelRepository;
import org.activiti.cloud.modeling.repository.ProjectRepository;
import org.activiti.cloud.services.modeling.config.ModelingRestApplication;
import org.activiti.cloud.services.modeling.entity.ProjectEntity;
import org.activiti.cloud.services.modeling.rest.config.RepositoryRestConfig;
import org.activiti.cloud.services.modeling.security.WithMockModelerUser;
import org.activiti.cloud.services.modeling.service.api.ModelService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(classes = ModelingRestApplication.class)
@WebAppConfiguration
@DirtiesContext(classMode = AFTER_EACH_TEST_METHOD)
@WithMockModelerUser
public class ProjectControllerIT {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ModelRepository modelRepository;

    @Autowired
    private ModelService modelService;

    @Autowired
    private ProcessModelType processModelType;

    @Autowired
    private ObjectMapper mapper;

    @BeforeEach
    public void setUp() {
        this.mockMvc = webAppContextSetup(webApplicationContext).build();
    }

    @Test
    public void should_returnExistingProjects_when_gettingProjects() throws Exception {

        projectRepository.createProject(project("project1"));
        projectRepository.createProject(project("project2"));

        mockMvc.perform(get("{version}/projects",
                            RepositoryRestConfig.API_VERSION))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.projects",
                                    hasSize(2)))
                .andExpect(jsonPath("$._embedded.projects[0].name",
                                    is("project1")))
                .andExpect(jsonPath("$._embedded.projects[1].name",
                                    is("project2")));
    }

    @Test
    public void should_returnProjectsMatchingTheExactName_when_gettingProjectsFilteredByName() throws Exception {

        projectRepository.createProject(project("project1"));
        projectRepository.createProject(project("project2"));

        mockMvc.perform(get("{version}/projects?name=project1",
                            RepositoryRestConfig.API_VERSION))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.projects",
                                    hasSize(1)))
                .andExpect(jsonPath("$._embedded.projects[0].name",
                                    is("project1")));
    }

    @Test
    public void should_returnProjectsContainingTheName_when_gettingProjectsFilteredByContainingName() throws Exception {

        projectRepository.createProject(project("project-main-1"));
        projectRepository.createProject(project("project-main-2"));
        projectRepository.createProject(project("project-secondary-2"));

        mockMvc.perform(get("{version}/projects?name=main",
                            RepositoryRestConfig.API_VERSION))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.projects",
                                    hasSize(2)))
                .andExpect(jsonPath("$._embedded.projects[0].name",
                                    is("project-main-1")))
                .andExpect(jsonPath("$._embedded.projects[1].name",
                                    is("project-main-2")));
    }

    @Test
    public void should_returnProjectsContainingTheName_when_gettingProjectsFilteredByInsensitiveContainingName() throws Exception {

        projectRepository.createProject(project("project-main-1"));
        projectRepository.createProject(project("project-main-2"));
        projectRepository.createProject(project("project-secondary-2"));

        mockMvc.perform(get("{version}/projects?name=MAIN",
                RepositoryRestConfig.API_VERSION))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.projects",
                        hasSize(2)))
                .andExpect(jsonPath("$._embedded.projects[0].name",
                        is("project-main-1")))
                .andExpect(jsonPath("$._embedded.projects[1].name",
                        is("project-main-2")));
    }

    @Test
    public void should_returnProject_when_gettingExistingProject() throws Exception {
        Project project = projectRepository.createProject(project("existing-project"));

        mockMvc.perform(get("{version}/projects/{projectId}",
                            API_VERSION,
                            project.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name",
                                    is("existing-project")));
    }

    @Test
    public void should_returnStatusCreatedAndProjectDetails_when_creatingProject() throws Exception {
        mockMvc.perform(post("{version}/projects",
                             API_VERSION)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(projectWithDescription("new-project",
                                                                                          "Project description"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name",
                                    is("new-project")))
                .andExpect(jsonPath("$.description",
                                    is("Project description")));
    }

    @Test
    public void should_throwConflictException_when_creatingProjectExistingName() throws Exception {
        projectRepository.createProject(project("existing-project"));

        mockMvc.perform(post("{version}/projects",
                             API_VERSION)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(project("existing-project"))))
                .andExpect(status().isConflict());
    }

    @Test
    public void should_returnStatusOk_when_updatingExistingProject() throws Exception {
        Project project = projectRepository.createProject(project("project-to-update"));

        mockMvc.perform(put("{version}/projects/{projectId}",
                            API_VERSION,
                            project.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(project("updated-project-name"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name",
                                    is("updated-project-name")));

        assertThat((Optional<Project>) projectRepository.findProjectById(project.getId()))
                .hasValueSatisfying(updatedProject -> {
                    assertThat(updatedProject.getName()).isEqualTo("updated-project-name");
                });
    }

    @Test
    public void should_throwConflictException_when_updatingProjectExistingName() throws Exception {
        projectRepository.createProject(project("existing-project"));
        Project project = projectRepository.createProject(project("project-to-update"));

        mockMvc.perform(put("{version}/projects/{projectId}",
                            API_VERSION,
                            project.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(project("existing-project"))))
                .andExpect(status().isConflict());
    }

    @Test
    public void should_returnStatusOk_when_updatingProjectNoName() throws Exception {
        Project project = projectRepository.createProject(project("project-to-update"));

        mockMvc.perform(put("{version}/projects/{projectId}",
                            API_VERSION,
                            project.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(projectWithDescription(null,
                                                                                          "New Description"))))
                .andExpect(status().isOk());
    }

    @Test
    public void should_throwBadRequestException_when_updatingProjectEmptyName() throws Exception {
        Project project = projectRepository.createProject(project("project-to-update"));

        mockMvc.perform(put("{version}/projects/{projectId}",
                            API_VERSION,
                            project.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(project(""))))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void should_throwBadRequestException_when_updatingProjectInvalidName() throws Exception {
        Project project = projectRepository.createProject(project("project-to-update"));

        mockMvc.perform(put("{version}/projects/{projectId}",
                            API_VERSION,
                            project.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(project("_1-invalid-name"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void should_throwBadRequestException_when_updatingProjectLongName() throws Exception {
        Project project = projectRepository.createProject(project("project-to-update"));

        mockMvc.perform(put("{version}/projects/{projectId}",
                            API_VERSION,
                            project.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(project("too-long-name-1234567890-1234567890"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void should_returnStatusNoContent_when_deletingProject() throws Exception {
        Project project = projectRepository.createProject(project("project-to-delete"));

        mockMvc.perform(delete("{version}/projects/{projectId}",
                               API_VERSION,
                               project.getId()))
                .andExpect(status().isNoContent());

        assertThat(projectRepository.findProjectById(project.getId())).isEmpty();
    }

    @Test
    public void should_returnZipFileWithProjectModels_when_exportingProject() throws Exception {
        ProjectEntity project = (ProjectEntity) projectRepository.createProject(project("project-with-models"));

        modelRepository.createModel(connectorModel(project,
                                                   "movies",
                                                   resourceAsByteArray("connector/movies.json")));

        Model processModel = modelService.importSingleModel(project,
                                                      processModelType,
                                                      processFileContent("Process_RankMovieId",
                                                                         resourceAsByteArray("process/RankMovie.bpmn20.xml")));


        Map<String, Extensions> extensionsMap = Collections.singletonMap("Process_RankMovieId",
            extensions("Task_1spvopd",
                                    processVariables("movieName",
                                        "movieDescription"),
                                    inputsMappings("movieName"),
                                    outputsMappings("movieDescription")));

        modelRepository.updateModel(processModel,
                                    processModelWithExtensions("Process_RankMovieId", extensionsMap));

       MvcResult response = mockMvc.perform(
                get("{version}/projects/{projectId}/export",
                    API_VERSION,
                    project.getId()))
                .andExpect(status().isOk())
                .andReturn();

        assertThatResponseContent(response)
                .isFile()
                .isZip()
                .hasName("project-with-models.zip")
                .hasEntries(
                        "project-with-models.json",
                        "processes/",
                        "processes/Process_RankMovieId.bpmn20.xml",
                        "processes/Process_RankMovieId-extensions.json",
                        "connectors/",
                        "connectors/movies.json")
                .hasJsonContentSatisfying("project-with-models.json",
                                          jsonContent -> jsonContent
                                                  .node("name").isEqualTo("project-with-models"))
                .hasJsonContentSatisfying("project-with-models.json",
                        jsonContent -> jsonContent
                                .node("users")
                                .isArray()
                                .ofLength(2)
                                .thatContains("userOne")
                                .thatContains("userTwo"))
                .hasJsonContentSatisfying("project-with-models.json",
                        jsonContent -> jsonContent
                                .node("groups")
                                .isArray()
                                .ofLength(2)
                                .thatContains("hr")
                                .thatContains("testgroup"))
                .hasJsonContentSatisfying("processes/Process_RankMovieId-extensions.json",
                                          jsonContent -> jsonContent
                                                  .node("name").isEqualTo("Process_RankMovieId")
                                                  .node("type").isEqualTo("PROCESS")
                                                  .node("extensions.Process_RankMovieId.properties").matches(allOf(hasKey("movieName"),
                                                                                               hasKey("movieDescription")))
                                                  .node("extensions.Process_RankMovieId.mappings").matches(
                                                          hasEntry(equalTo("Task_1spvopd"),
                                                                   allOf(hasEntry(equalTo("inputs"),
                                                                                  hasKey("movieName")),
                                                                         hasEntry(equalTo("outputs"),
                                                                                  hasKey("movieDescription")))))

                );
    }

    @Test
    public void should_throwBadRequestException_when_validatingProjectWithValidationErrors() throws Exception {
        ProjectEntity project = (ProjectEntity) projectRepository
                .createProject(project("project-with-models"));

        modelRepository.createModel(processModelWithContent(project,
                                                            "process-model",
                                                            "Invalid process xml"));

        List<ModelValidationError> expectedValidationErrors =
                Arrays.asList(new ModelValidationError(),
                              new ModelValidationError());

        MvcResult response = mockMvc.perform(
                get("{version}/projects/{projectId}/validate",
                    API_VERSION,
                    project.getId()))
                .andExpect(status().isBadRequest())
                .andReturn();
    }

    @Test
    public void should_throwSemanticModelValidationException_when_validatingEmptyProjectWithValidationErrors() throws Exception {
        ProjectEntity project = (ProjectEntity) projectRepository
                .createProject(project("project-without-process"));

        MvcResult response = mockMvc.perform(
                get("{version}/projects/{projectId}/validate",
                    API_VERSION,
                    project.getId()))
                .andExpect(status().isBadRequest())
                .andReturn();

        assertThat(((SemanticModelValidationException) response.getResolvedException()).getValidationErrors())
                .hasSize(1)
                .extracting(ModelValidationError::getProblem,
                            ModelValidationError::getDescription)
                .containsOnly(tuple("Invalid project",
                                    "Project must contain at least one process"));
    }

    @Test
    public void should_returnStatusNoContent_when_ProjectIsValid() throws Exception {
        ProjectEntity project = (ProjectEntity) projectRepository.createProject(project("project-with-models"));

        modelRepository.createModel(connectorModel(project,
                "movies",
                resourceAsByteArray("connector/movies.json")));

        Model processModel = modelService.importSingleModel(project,
                processModelType,
                processFileContent("Process_RankMovieId",
                        resourceAsByteArray("process/RankMovie.bpmn20.xml")));

        Map<String, Extensions> extensionsMap = Collections.singletonMap("Process_RankMovieId",
            extensions("Task_1spvopd",
                processVariables("movieName",
                    "movieDescription"),
                inputsMappings("movieName"),
                outputsMappings("movieDescription")));
        modelRepository.updateModel(processModel,
                processModelWithExtensions("process-model", extensionsMap));

        MvcResult response = mockMvc.perform(
                get("{version}/projects/{projectId}/validate",
                        API_VERSION,
                        project.getId()))
                .andExpect(status().isOk())
                .andReturn();
    }

    @Test
    public void should_throwSemanticModelValidationException_when_validatingProjectWithNoAssigneeShouldReturnErrors() throws Exception {
        ProjectEntity project = (ProjectEntity) projectRepository.createProject(project("project-with-models"));
        modelService.importSingleModel(project,
                                 processModelType,
                                 processFileContent("process-model",
                                                    resourceAsByteArray("process/no-assignee.bpmn20.xml")));

        MvcResult response = mockMvc.perform(
                get("{version}/projects/{projectId}/validate",
                    API_VERSION,
                    project.getId()))
                .andExpect(status().isBadRequest())
                .andReturn();

        assertThat(((SemanticModelValidationException) response.getResolvedException()).getValidationErrors())
                .hasSize(1)
                .extracting(ModelValidationError::getProblem,
                            ModelValidationError::getDescription,
                            ModelValidationError::getValidatorSetName)
                .containsOnly(tuple("No assignee for user task",
                                    "One of the attributes 'assignee','candidateUsers' or 'candidateGroups' are mandatory on user task",
                                    "BPMN user task assignee validator"));
    }

    @Test
    public void should_throwSemanticModelValidationException_when_exportingProjectWithInvalidServiceTaskReturnErrors() throws Exception {
        ProjectEntity project = (ProjectEntity) projectRepository.createProject(project("project-with-connectors"));
        modelRepository.createModel(processModelWithContent(project,
                                                            "Process_InvalidTask",
                                                            resourceAsByteArray("process/invalid-service-task.bpmn20.xml")));

        modelRepository.createModel(connectorModel(project,
                                                   "invalid-connector-action",
                                                   resourceAsByteArray("connector/invalid-connector-action.json")));
        MvcResult response = mockMvc.perform(
                get("{version}/projects/{projectId}/validate",
                    API_VERSION,
                    project.getId()))
                .andExpect(status().isBadRequest())
                .andReturn();

        assertThat(((SemanticModelValidationException) response.getResolvedException()).getValidationErrors())
                .extracting(ModelValidationError::getProblem,
                            ModelValidationError::getDescription,
                            ModelValidationError::getValidatorSetName)
                .contains(tuple("Invalid service implementation",
                                "Invalid service implementation on service 'ServiceTask_1qr4ad0'",
                                "BPMN service task validator"));
    }

    @Test
    public void should_throwSemanticModelValidationException_when_validatingProjectWithServiceTaskEmptyImplementationReturnErrors() throws Exception {
        ProjectEntity project = (ProjectEntity) projectRepository.createProject(project("project-with-invalid-task"));
        modelRepository.createModel(processModelWithContent(project,
                                                            "invalid-connector-action",
                                                            resourceAsByteArray("process/no-implementation-service-task.bpmn20.xml")));

        modelRepository.createModel(connectorModel(project,
                                                   "invalid-connector-action",
                                                   resourceAsByteArray("connector/invalid-connector-action.json")));
        MvcResult response = mockMvc.perform(
                get("{version}/projects/{projectId}/validate",
                    API_VERSION,
                    project.getId()))
                .andExpect(status().isBadRequest())
                .andReturn();

        assertThat(((SemanticModelValidationException) response.getResolvedException()).getValidationErrors())
                .extracting(ModelValidationError::getProblem,
                            ModelValidationError::getDescription,
                            ModelValidationError::getValidatorSetName)
                .contains(tuple("activiti-servicetask-missing-implementation",
                                "One of the attributes 'implementation', 'class', 'delegateExpression', 'type', 'operation', or 'expression' is mandatory on serviceTask.",
                                "activiti-executable-process"),
                          tuple("Invalid service implementation",
                                "Invalid service implementation on service 'ServiceTask_1qr4ad0'",
                                "BPMN service task validator"));
    }

    @Test
    public void should_throwSemanticModelValidationException_when_validatingProjectWithProcessExtensionsForUnknownTask() throws Exception {
        ProjectEntity project = (ProjectEntity) projectRepository.createProject(project("invalid-project"));
        Model processModel = modelService.importSingleModel(project,
                                                      processModelType,
                                                      processFileContent("RankMovie",
                                                                         resourceAsByteArray("process/RankMovie.bpmn20.xml")));
        modelRepository.updateModel(processModel,
                                    processModelWithExtensions("Process_RankMovieId",
                                                               extensions(resourceAsByteArray("process-extensions/RankMovie-extensions-unknown-task.json"))));
        modelRepository.createModel(connectorModel(project,
                                                   "movies",
                                                   resourceAsByteArray("connector/movies.json")));

        assertThatResponse(
                mockMvc.perform(
                        get("{version}/projects/{projectId}/validate",
                            API_VERSION,
                            project.getId()))
                        .andExpect(status().isBadRequest())
                        .andReturn())
                .isSemanticValidationException()
                .hasValidationErrorMessages(
                        "The extensions for process 'Process_RankMovieId' " +
                            "contains mappings for an unknown task 'unknown-task'");
    }

    @Test
    public void should_throwSemanticModelValidationException_when_validatingProjectWithProcessExtensionsForUnknownOutputProcessVariableMapping() throws Exception {
        ProjectEntity project = (ProjectEntity) projectRepository.createProject(project("invalid-project"));
        Model processModel = modelService.importSingleModel(project,
                                                      processModelType,
                                                      processFileContent("RankMovie",
                                                                         resourceAsByteArray("process/RankMovie.bpmn20.xml")));
        modelRepository.updateModel(processModel,
                                    processModelWithExtensions("Process_RankMovieId",
                                                               extensions(resourceAsByteArray("process-extensions/RankMovie-extensions-unknown-output-process-variable.json"))));
        modelRepository.createModel(connectorModel(project,
                                                   "movies",
                                                   resourceAsByteArray("connector/movies.json")));

        assertThatResponse(
                mockMvc.perform(
                        get("{version}/projects/{projectId}/validate",
                            API_VERSION,
                            project.getId()))
                        .andExpect(status().isBadRequest())
                        .andReturn())
                .isSemanticValidationException()
                .hasValidationErrorMessages(
                        "The extensions for process 'Process_RankMovieId' " +
                            "contains mappings for an unknown process variable 'unknown-output-variable'");
    }

    @Test
    public void should_throwSemanticModelValidationException_when_validatingProjectWithProcessExtensionsForConnectorWithoutInputsOutputs() throws Exception {
        ProjectEntity project = (ProjectEntity) projectRepository.createProject(project("invalid-project"));
        Model processModel = modelService.importSingleModel(project,
                                                      processModelType,
                                                      processFileContent("RankMovie",
                                                                         resourceAsByteArray("process/RankMovie.bpmn20.xml")));
        modelRepository.updateModel(processModel,
                                    processModelWithExtensions("Process_RankMovieId",
                                                               extensions(resourceAsByteArray("process-extensions/RankMovie-extensions.json"))));
        modelRepository.createModel(connectorModel(project,
                                                   "movies",
                                                   resourceAsByteArray("connector/movies-without-inputs-outputs.json")));

        assertThatResponse(
                mockMvc.perform(
                        get("{version}/projects/{projectId}/validate",
                            API_VERSION,
                            project.getId()))
                        .andExpect(status().isBadRequest())
                        .andReturn())
                .isSemanticValidationException()
                .hasValidationErrorMessages(
                        "The extensions for process 'Process_RankMovieId' " +
                            "contains mappings to task 'Task_1spvopd' for an unknown inputs connector parameter name 'movieName'",
                        "The extensions for process 'Process_RankMovieId' " +
                            "contains mappings to task 'Task_1spvopd' for an unknown outputs connector parameter name 'movieDescription'");
    }

    @Test
    public void should_throwSemanticModelValidationException_when_validatingProcessWithInValidMessagePayload() throws Exception {
        ProjectEntity project = (ProjectEntity) projectRepository.createProject(project("message-project"));
        Model processModel = modelService.importSingleModel(project,
                                                      processModelType,
                                                      processFileContent("Process_hg2itgWRj",
                                                                         resourceAsByteArray("process/message-payload.bpmn20.xml")));
        modelRepository.updateModel(processModel,
                                    processModelWithExtensions("Process_hg2itgWRj",
                                                               extensions(resourceAsByteArray("process-extensions/message-payload-extension.json"))));

        assertThatResponse(
            mockMvc.perform(
                get("{version}/projects/{projectId}/validate",
                    API_VERSION,
                    project.getId()))
                .andExpect(status().isBadRequest())
                .andReturn())
            .isSemanticValidationException()
            .hasValidationErrorMessages(
                "The extensions for process 'Process_hg2itgWRj' contains mappings to element 'IntermediateThrowEvent_1kozj3g' for an invalid payload name 'my-message-payload'",
                "The extensions for process 'Process_hg2itgWRj' contains mappings to element 'IntermediateThrowEvent_1kozj3g' for an invalid payload name 'wrong-payload'",
                "The extensions for process 'Process_hg2itgWRj' contains mappings to element 'IntermediateThrowEvent_1kozj3g' for an invalid payload name '123abc'");
    }

    @Test
    public void should_throwSemanticModelValidationException_when_validatingProjectWithProcessExtensionsForUnknownConnectorParameterMapping() throws Exception {
        ProjectEntity project = (ProjectEntity) projectRepository.createProject(project("invalid-project"));
        Model processModel = modelService.importSingleModel(project,
                                                      processModelType,
                                                      processFileContent("RankMovie",
                                                                         resourceAsByteArray("process/RankMovie.bpmn20.xml")));
        modelRepository.updateModel(processModel,
                                    processModelWithExtensions("Process_RankMovieId",
                                                               extensions(resourceAsByteArray("process-extensions/RankMovie-extensions-unknown-connector-parameter.json"))));
        modelRepository.createModel(connectorModel(project,
                                                   "movies",
                                                   resourceAsByteArray("connector/movies.json")));

        assertThatResponse(
                mockMvc.perform(
                        get("{version}/projects/{projectId}/validate",
                            API_VERSION,
                            project.getId()))
                        .andExpect(status().isBadRequest())
                        .andReturn())
                .isSemanticValidationException()
                .hasValidationErrorMessages(
                        "The extensions for process 'Process_RankMovieId' " +
                            "contains mappings to task 'Task_1spvopd' for an unknown inputs connector parameter name 'unknown-input-parameter'",
                        "The extensions for process 'Process_RankMovieId' " +
                            "contains mappings to task 'Task_1spvopd' for an unknown outputs connector parameter name 'unknown-output-parameter'");
    }

    @Test
    public void should_throwSemanticModelValidationException_when_validatingProjectWithProcessExtensionsForUnknownInputProcessVariableMapping() throws Exception {
        ProjectEntity project = (ProjectEntity) projectRepository.createProject(project("invalid-project"));
        Model processModel = modelService.importSingleModel(project,
                                                      processModelType,
                                                      processFileContent("RankMovie",
                                                                         resourceAsByteArray("process/RankMovie.bpmn20.xml")));
        modelRepository.updateModel(processModel,
                                    processModelWithExtensions("Process_RankMovieId",
                                                               extensions(resourceAsByteArray("process-extensions/RankMovie-extensions-unknown-input-process-variable.json"))));
        modelRepository.createModel(connectorModel(project,
                                                   "movies",
                                                   resourceAsByteArray("connector/movies.json")));

        assertThatResponse(
                mockMvc.perform(
                        get("{version}/projects/{projectId}/validate",
                            API_VERSION,
                            project.getId()))
                        .andExpect(status().isBadRequest())
                        .andReturn())
                .isSemanticValidationException()
                .hasValidationErrorMessages(
                        "The extensions for process 'Process_RankMovieId' " +
                            "contains mappings for an unknown process variable 'unknown-input-variable'");
    }

    @Test
    public void should_throwSemanticModelValidationException_when_validatingProjectWithInvalidCallActivityReference() throws Exception {
        ProjectEntity project = (ProjectEntity) projectRepository.createProject(project("project-call-activiti"));
        Model processModel = modelService.importSingleModel(project,
                                                      processModelType,
                                                      processFileContent("Process_RankMovieId",
                                                                         resourceAsByteArray("process/RankMovie.bpmn20.xml")));
        modelRepository.updateModel(processModel,
                                    processModelWithExtensions("Process_RankMovieId",
                                                               extensions(resourceAsByteArray("process-extensions/RankMovie-extensions.json"))));
        modelRepository.createModel(connectorModel(project,
                                                   "movies",
                                                   resourceAsByteArray("connector/movies.json")));

        Model mainProcessModel = modelService.importSingleModel(project,
                                                          processModelType,
                                                          processFileContentWithCallActivity("Process_TwoCall",
                                                                                             processModel,
                                                                                             resourceAsByteArray("process/two-call-activities.bpmn20.xml")));

        assertThatResponse(
                mockMvc.perform(
                        get("{version}/projects/{projectId}/validate",
                            API_VERSION,
                            project.getId()))
                        .andExpect(status().isBadRequest())
                        .andReturn())
                .isSemanticValidationException()
                .hasValidationErrorMessages(
                        "Call activity 'Task_1mbp1v0' with call element 'not-present' found in process 'Process_TwoCall' " +
                            "references a process id that does not exist in the current project.");
    }

    @Test
    public void should_returnStatusOk_when_exportingProjectWithValidCallActivity() throws Exception {
        ProjectEntity project = (ProjectEntity) projectRepository.createProject(project("project-with-call-activity"));
        Model processModel = modelService.importSingleModel(project,
                                                      processModelType,
                                                      processFileContent("RankMovie",
                                                                         resourceAsByteArray("process/RankMovie.bpmn20.xml")));
        modelRepository.updateModel(processModel,
                                    processModelWithExtensions("process-model",
                                                               extensions(resourceAsByteArray("process-extensions/RankMovie-extensions.json"))));
        modelRepository.createModel(connectorModel(project,
                                                   "movies",
                                                   resourceAsByteArray("connector/movies.json")));

        modelService.importSingleModel(project,
                                 processModelType,
                                 processFileContentWithCallActivity("main-process",
                                                                    processModel,
                                                                    resourceAsByteArray("process/call-activity.bpmn20.xml")));

        mockMvc.perform(
                get("{version}/projects/{projectId}/export",
                    API_VERSION,
                    project.getId()))
                .andExpect(status().isOk());
    }

    @Test
    public void should_returnStatusCreated_when_importingProject() throws Exception {
        MockMultipartFile zipFile = new MockMultipartFile("file",
                                                          "project-xy.zip",
                                                          "project/zip",
                                                          resourceAsByteArray("project/project-xy.zip"));

        mockMvc.perform(multipart("{version}/projects/import",
                                  API_VERSION)
                                .file(zipFile)
                                .accept(APPLICATION_JSON_VALUE))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.entry.name",
                                    is("application-xy")));
    }

    @Test
    public void should_throwBadRequestException_when_importingProjectInvalidJsonFile() throws Exception {
        MockMultipartFile zipFile = new MockMultipartFile("file",
                                                          "project-xy-invalid.zip",
                                                          "project/zip",
                                                          resourceAsByteArray("project/project-xy-invalid.zip"));

        mockMvc.perform(multipart("{version}/projects/import",
                                  API_VERSION)
                                .file(zipFile)
                                .accept(APPLICATION_JSON_VALUE))
                .andExpect(status().isBadRequest())
                .andExpect(status().reason(is("No valid project entry found to import: project-xy-invalid.zip")));
    }

    @Test
    public void should_throwBadRequestException_when_importingProjectInvalidProcessJsonFile() throws Exception {
        MockMultipartFile zipFile = new MockMultipartFile("file",
                                                          "project-xy.zip",
                                                          "project/zip",
                                                          resourceAsByteArray("project/project-xy-invalid-process-json.zip"));

        mockMvc.perform(multipart("{version}/projects/import",
                                  API_VERSION)
                                .file(zipFile)
                                .accept(APPLICATION_JSON_VALUE))
                .andExpect(status().isBadRequest())
                .andExpect(status().reason(containsString("Error importing model : Error reading XML")));
    }

    @Test
    public void should_returnStatusCreatedAndGivenName_when_importingProjectWithNameProvided() throws Exception {
        MockMultipartFile zipFile = new MockMultipartFile("file",
                                                          "project-xy.zip",
                                                          "project/zip",
                                                          resourceAsByteArray("project/project-xy.zip"));

        String overridingName = "overridingName";

        mockMvc.perform(multipart("{version}/projects/import?name=" + overridingName,
                                  API_VERSION).file(zipFile).accept(APPLICATION_JSON_VALUE))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.entry.name",
                                                                                   is(overridingName)));
    }

    @Test
    public void should_returnStatusCreatedAndZipName_when_importingProjectWithNullNameProvided() throws Exception {
        MockMultipartFile zipFile = new MockMultipartFile("file",
                                                          "project-xy.zip",
                                                          "project/zip",
                                                          resourceAsByteArray("project/project-xy.zip"));

        mockMvc.perform(multipart("{version}/projects/import?name=",
                                  API_VERSION).file(zipFile).accept(APPLICATION_JSON_VALUE))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.entry.name",
                                                                                   is("application-xy")));
    }

    @Test
    public void should_returnStatusCreatedAndZipName_when_importingProjectWithBlankNameProvided() throws Exception {
        MockMultipartFile zipFile = new MockMultipartFile("file",
                                                          "project-xy.zip",
                                                          "project/zip",
                                                          resourceAsByteArray("project/project-xy.zip"));

        String overridingName = "      ";

        mockMvc.perform(multipart("{version}/projects/import?name=" + overridingName,
                                  API_VERSION).file(zipFile).accept(APPLICATION_JSON_VALUE))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.entry.name",
                                                                                   is("application-xy")));
    }

    @Test
    public void should_returnStatusOK_when_exportingProjectWithProcessExtensionsWithValueOutputProcessVariableMapping() throws Exception {
        ProjectEntity project = (ProjectEntity) projectRepository.createProject(project("invalid-project"));
        Model processModel = modelService.importSingleModel(project,
                                                            processModelType,
                                                            processFileContent("RankMovie",
                                                                               resourceAsByteArray("process/RankMovie.bpmn20.xml")));
        modelRepository.updateModel(processModel,
                                    processModelWithExtensions("process-model",
                                                               extensions(resourceAsByteArray("process-extensions/RankMovie-extensions-value-output-process-variable.json"))));
        modelRepository.createModel(connectorModel(project,
                                                   "movies",
                                                   resourceAsByteArray("connector/movies.json")));

        mockMvc.perform(get("{version}/projects/{projectId}/export",
                            API_VERSION,
                            project.getId()))
                .andExpect(status().isOk());
    }

}
