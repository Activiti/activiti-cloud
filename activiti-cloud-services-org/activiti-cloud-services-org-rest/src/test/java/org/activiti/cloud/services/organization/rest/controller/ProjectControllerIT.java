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

package org.activiti.cloud.services.organization.rest.controller;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.activiti.cloud.organization.api.Model;
import org.activiti.cloud.organization.api.ModelValidationError;
import org.activiti.cloud.organization.api.ProcessModelType;
import org.activiti.cloud.organization.api.Project;
import org.activiti.cloud.organization.core.error.SemanticModelValidationException;
import org.activiti.cloud.organization.repository.ModelRepository;
import org.activiti.cloud.organization.repository.ProjectRepository;
import org.activiti.cloud.services.organization.config.OrganizationRestApplication;
import org.activiti.cloud.services.organization.entity.ProjectEntity;
import org.activiti.cloud.services.organization.rest.config.RepositoryRestConfig;
import org.activiti.cloud.services.organization.service.ModelService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.context.WebApplicationContext;

import static org.activiti.cloud.services.common.util.FileUtils.resourceAsByteArray;
import static org.activiti.cloud.services.organization.mock.MockFactory.connectorModel;
import static org.activiti.cloud.services.organization.mock.MockFactory.extensions;
import static org.activiti.cloud.services.organization.mock.MockFactory.processFileContent;
import static org.activiti.cloud.services.organization.mock.MockFactory.processModelWithContent;
import static org.activiti.cloud.services.organization.mock.MockFactory.processModelWithExtensions;
import static org.activiti.cloud.services.organization.mock.MockFactory.project;
import static org.activiti.cloud.services.organization.mock.MockFactory.projectWithDescription;
import static org.activiti.cloud.services.organization.rest.config.RepositoryRestConfig.API_VERSION;
import static org.activiti.cloud.services.test.asserts.AssertResponseContent.assertThatResponseContent;
import static org.assertj.core.api.Assertions.*;
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
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = OrganizationRestApplication.class)
@WebAppConfiguration
@DirtiesContext(classMode = AFTER_EACH_TEST_METHOD)
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

    @Before
    public void setUp() {
        this.mockMvc = webAppContextSetup(webApplicationContext).build();
    }

    @Test
    public void testGetProjects() throws Exception {

        // GIVEN
        projectRepository.createProject(project("project1"));
        projectRepository.createProject(project("project2"));

        // WHEN
        mockMvc.perform(get("{version}/projects",
                            RepositoryRestConfig.API_VERSION))
                // THEN
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.projects",
                                    hasSize(2)))
                .andExpect(jsonPath("$._embedded.projects[0].name",
                                    is("project1")))
                .andExpect(jsonPath("$._embedded.projects[1].name",
                                    is("project2")));
    }

    @Test
    public void testGetProject() throws Exception {
        // GIVEN
        Project project = projectRepository.createProject(project("existing-project"));

        // WHEN
        mockMvc.perform(get("{version}/projects/{projectId}",
                            API_VERSION,
                            project.getId()))
                // THEN
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name",
                                    is("existing-project")));
    }

    @Test
    public void testCreateProject() throws Exception {
        // WHEN
        mockMvc.perform(post("{version}/projects",
                             API_VERSION)
                                .contentType(MediaType.APPLICATION_JSON_UTF8)
                                .content(mapper.writeValueAsString(projectWithDescription("new-project",
                                                                                          "Project description"))))
                // THEN
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name",
                                    is("new-project")))
                .andExpect(jsonPath("$.description",
                                    is("Project description")));
    }

    @Test
    public void testCreateProjectExistingName() throws Exception {
        // GIVEN
        projectRepository.createProject(project("existing-project"));

        // WHEN
        mockMvc.perform(post("{version}/projects",
                             API_VERSION)
                                .contentType(MediaType.APPLICATION_JSON_UTF8)
                                .content(mapper.writeValueAsString(project("existing-project"))))
                // THEN
                .andExpect(status().isConflict());
    }

    @Test
    public void testUpdateProject() throws Exception {
        // GIVEN
        Project project = projectRepository.createProject(project("project-to-update"));

        // WHEN
        mockMvc.perform(put("{version}/projects/{projectId}",
                            API_VERSION,
                            project.getId())
                                .contentType(MediaType.APPLICATION_JSON_UTF8)
                                .content(mapper.writeValueAsString(project("updated-project-name"))))
                // THEN
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name",
                                    is("updated-project-name")));

        assertThat((Optional<Project>) projectRepository.findProjectById(project.getId()))
                .hasValueSatisfying(updatedProject -> {
                    assertThat(updatedProject.getName()).isEqualTo("updated-project-name");
                });
    }

    @Test
    public void testUpdateProjectExistingName() throws Exception {
        // GIVEN
        projectRepository.createProject(project("existing-project"));
        Project project = projectRepository.createProject(project("project-to-update"));

        // WHEN
        mockMvc.perform(put("{version}/projects/{projectId}",
                            API_VERSION,
                            project.getId())
                                .contentType(MediaType.APPLICATION_JSON_UTF8)
                                .content(mapper.writeValueAsString(project("existing-project"))))
                // THEN
                .andExpect(status().isConflict());
    }

    @Test
    public void testUpdateProjectNoName() throws Exception {
        // GIVEN
        Project project = projectRepository.createProject(project("project-to-update"));

        // WHEN
        mockMvc.perform(put("{version}/projects/{projectId}",
                            API_VERSION,
                            project.getId())
                                .contentType(MediaType.APPLICATION_JSON_UTF8)
                                .content(mapper.writeValueAsString(project(null))))
                // THEN
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testDeleteProject() throws Exception {
        // GIVEN
        Project project = projectRepository.createProject(project("project-to-delete"));

        // WHEN
        mockMvc.perform(delete("{version}/projects/{projectId}",
                               API_VERSION,
                               project.getId()))
                // THEN
                .andExpect(status().isNoContent());

        assertThat(projectRepository.findProjectById(project.getId())).isEmpty();
    }

    @Test
    public void testExportProject() throws Exception {
        // GIVEN
        ProjectEntity project = (ProjectEntity) projectRepository.createProject(project("project-with-models"));

        modelRepository.createModel(connectorModel(project,
                                                   "movies",
                                                   resourceAsByteArray("connector/movies.json")));

        Model processModel = modelService.importModel(project,
                                                      processModelType,
                                                      processFileContent("process-model",
                                                                         resourceAsByteArray("process/RankMovie.bpmn20.xml")));

        modelRepository.updateModel(processModel,
                                    processModelWithExtensions("process-model",
                                                               extensions("Task_1spvopd",
                                                                          "movieToRank",
                                                                          "movieDesc")));

        // WHEN
        MvcResult response = mockMvc.perform(
                get("{version}/projects/{projectId}/export",
                    API_VERSION,
                    project.getId()))
                .andExpect(status().isOk())
                .andReturn();

        // THEN
        assertThatResponseContent(response)
                .isFile()
                .isZip()
                .hasName("project-with-models.zip")
                .hasEntries(
                        "project-with-models.json",
                        "processes/",
                        "processes/process-model.bpmn20.xml",
                        "processes/process-model-extensions.json",
                        "connectors/",
                        "connectors/movies.json")
                .hasJsonContentSatisfying("project-with-models.json",
                                          jsonContent -> jsonContent
                                                  .node("name").isEqualTo("project-with-models"))
                .hasJsonContentSatisfying("processes/process-model-extensions.json",
                                          jsonContent -> jsonContent
                                                  .node("name").isEqualTo("process-model")
                                                  .node("type").isEqualTo("PROCESS")
                                                  .node("extensions.properties").matches(allOf(hasKey("movieToRank"),
                                                                                               hasKey("movieDesc")))
                                                  .node("extensions.mappings").matches(
                                                          hasEntry(equalTo("Task_1spvopd"),
                                                                   allOf(hasEntry(equalTo("inputs"),
                                                                                  allOf(hasKey("movieToRank"),
                                                                                        hasKey("movieDesc"))),
                                                                         hasEntry(equalTo("outputs"),
                                                                                  allOf(hasKey("movieToRank"),
                                                                                        hasKey("movieDesc"))))))

                );
    }

    @Test
    public void testExportProjectWithValidationErrors() throws Exception {
        // GIVEN
        ProjectEntity project = (ProjectEntity) projectRepository
                .createProject(project("project-with-models"));

        modelRepository.createModel(processModelWithContent(project,
                                                            "process-model",
                                                            "Invalid process xml"));

        List<ModelValidationError> expectedValidationErrors =
                Arrays.asList(new ModelValidationError(),
                              new ModelValidationError());

        // WHEN
        MvcResult response = mockMvc.perform(
                get("{version}/projects/{projectId}/export",
                    API_VERSION,
                    project.getId()))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andReturn();
    }

    @Test
    public void exportProjectWithNoAssigneeShouldReturnErrors() throws Exception {
        // GIVEN
        ProjectEntity project = (ProjectEntity) projectRepository.createProject(project("project-with-models"));
        modelService.importModel(project,
                                 processModelType,
                                 processFileContent("process-model",
                                                    resourceAsByteArray("process/no-assignee.bpmn20.xml")));

        // WHEN
        MvcResult response = mockMvc.perform(
                get("{version}/projects/{projectId}/export",
                    API_VERSION,
                    project.getId()))
                .andExpect(status().isBadRequest())
                .andReturn();

        // THEN
        assertThat(((SemanticModelValidationException) response.getResolvedException()).getValidationErrors())
                .hasSize(1)
                .extracting(ModelValidationError::getProblem,
                            ModelValidationError::getDescription,
                            ModelValidationError::getValidatorSetName)
                .containsOnly(tuple("No assignee for user task",
                                    "One of the attributes 'assignee','candidateUsers' or 'candidateGroups' are mandatory on user task",
                                    "BPMN user task validator"));
    }

    @Test
    public void exportProjectWithInvalidServiceTaskReturnErrors() throws Exception {
        // GIVEN
        ProjectEntity project = (ProjectEntity) projectRepository.createProject(project("project-with-connectors"));
        modelRepository.createModel(processModelWithContent(project,
                                                            "invalid-service",
                                                            resourceAsByteArray("process/invalid-service-task.bpmn20.xml")));

        modelRepository.createModel(connectorModel(project,
                                                   "invalid-connector-action",
                                                   resourceAsByteArray("connector/invalid-connector-action.json")));
        // WHEN
        MvcResult response = mockMvc.perform(
                get("{version}/projects/{projectId}/export",
                    API_VERSION,
                    project.getId()))
                .andExpect(status().isBadRequest())
                .andReturn();

        // THEN
        assertThat(((SemanticModelValidationException) response.getResolvedException()).getValidationErrors())
                .extracting(ModelValidationError::getProblem,
                            ModelValidationError::getDescription,
                            ModelValidationError::getValidatorSetName)
                .contains(tuple("Invalid service implementation",
                                    "Invalid service implementation on service 'ServiceTask_1qr4ad0'",
                                    "BPMN service task validator"));
    }

    @Test
    public void exportProjectWithServiceTaskEmptyImplementationReturnErrors() throws Exception {
        // GIVEN
        ProjectEntity project = (ProjectEntity) projectRepository.createProject(project("project-with-invalid-stask"));
        modelRepository.createModel(processModelWithContent(project,
                                                            "invalid-connector-action",
                                                            resourceAsByteArray("process/no-implementation-service-task.bpmn20.xml")));

        modelRepository.createModel(connectorModel(project,
                                                   "invalid-connector-action",
                                                   resourceAsByteArray("connector/invalid-connector-action.json")));
        // WHEN
        MvcResult response = mockMvc.perform(
                get("{version}/projects/{projectId}/export",
                    API_VERSION,
                    project.getId()))
                .andExpect(status().isBadRequest())
                .andReturn();

        // THEN
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
    public void testImportProject() throws Exception {
        //GIVEN
        MockMultipartFile zipFile = new MockMultipartFile("file",
                                                          "project-xy.zip",
                                                          "project/zip",
                                                          resourceAsByteArray("project/project-xy.zip"));

        // WHEN
        mockMvc.perform(multipart("{version}/projects/import",
                                  API_VERSION)
                                .file(zipFile)
                                .accept(APPLICATION_JSON_VALUE))
                // THEN
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name",
                                    is("application-xy")));
    }

    @Test
    public void testImportProjectInvalidJsonFile() throws Exception {
        //GIVEN
        MockMultipartFile zipFile = new MockMultipartFile("file",
                                                          "project-xy.zip",
                                                          "project/zip",
                                                          resourceAsByteArray("project/project-xy-invalid.zip"));

        // WHEN
        mockMvc.perform(multipart("{version}/projects/import",
                                  API_VERSION)
                                .file(zipFile)
                                .accept(APPLICATION_JSON_VALUE))
                // THEN
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(status().reason(is("No valid project entry found to import: project-xy.zip")));
    }

    @Test
    public void testImportProjectInvalidProcessJsonFile() throws Exception {
        //GIVEN
        MockMultipartFile zipFile = new MockMultipartFile("file",
                                                          "project-xy.zip",
                                                          "project/zip",
                                                          resourceAsByteArray("project/project-xy-invalid-process-json.zip"));

        // WHEN
        mockMvc.perform(multipart("{version}/projects/import",
                                  API_VERSION)
                                .file(zipFile)
                                .accept(APPLICATION_JSON_VALUE))
                // THEN
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(status().reason(containsString("Cannot convert json file content to model")));
    }
}