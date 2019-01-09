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
import org.activiti.cloud.organization.api.Project;
import org.activiti.cloud.organization.api.Model;
import org.activiti.cloud.organization.api.ModelValidationError;
import org.activiti.cloud.organization.core.rest.client.model.ModelReference;
import org.activiti.cloud.organization.core.rest.client.service.ModelReferenceService;
import org.activiti.cloud.organization.repository.ProjectRepository;
import org.activiti.cloud.organization.repository.ModelRepository;
import org.activiti.cloud.services.organization.config.OrganizationRestApplication;
import org.activiti.cloud.services.organization.entity.ProjectEntity;
import org.activiti.cloud.services.organization.entity.ModelEntity;
import org.activiti.cloud.services.organization.jpa.ProjectJpaRepository;
import org.activiti.cloud.services.organization.jpa.ModelJpaRepository;
import org.activiti.cloud.services.organization.rest.config.RepositoryRestConfig;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.context.WebApplicationContext;

import static org.activiti.cloud.organization.api.ProcessModelType.PROCESS;
import static org.activiti.cloud.services.common.util.FileUtils.resourceAsByteArray;
import static org.activiti.cloud.services.organization.mock.MockFactory.project;
import static org.activiti.cloud.services.organization.mock.MockFactory.extensions;
import static org.activiti.cloud.services.organization.mock.MockFactory.processModelWithContent;
import static org.activiti.cloud.services.organization.mock.MockFactory.processModelWithExtensions;
import static org.activiti.cloud.services.organization.mock.ModelingArgumentMatchers.modelReferenceNamed;
import static org.activiti.cloud.services.organization.rest.config.RepositoryRestConfig.API_VERSION;
import static org.activiti.cloud.services.test.asserts.AssertResponseContent.assertThatResponseContent;
import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.AllOf.allOf;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
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
public class ProjectControllerIT {

    private MockMvc mockMvc;

    @MockBean
    private ModelReferenceService modelReferenceService;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ModelRepository modelRepository;

    @Autowired
    private ObjectMapper mapper;

    @Before
    public void setUp() {
        this.mockMvc = webAppContextSetup(webApplicationContext).build();
    }

    @After
    public void tearDown() {
        ((ModelJpaRepository) modelRepository).deleteAllInBatch();
        ((ProjectJpaRepository) projectRepository).deleteAllInBatch();
    }

    @Test
    public void testGetProjects() throws Exception {

        // GIVEN
        projectRepository.createProject(project("Project1"));
        projectRepository.createProject(project("Project2"));

        // WHEN
        mockMvc.perform(get("{version}/projects",
                            RepositoryRestConfig.API_VERSION))
                // THEN
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.projects",
                                    hasSize(2)))
                .andExpect(jsonPath("$._embedded.projects[0].name",
                                    is("Project1")))
                .andExpect(jsonPath("$._embedded.projects[1].name",
                                    is("Project2")));
    }

    @Test
    public void testGetProject() throws Exception {
        // GIVEN
        Project project = project("Existing Project");
        projectRepository.createProject(project);

        // WHEN
        mockMvc.perform(get("{version}/projects/{projectId}",
                            API_VERSION,
                            project.getId()))
                // THEN
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name",
                                    is("Existing Project")));
    }

    @Test
    public void testCreateProject() throws Exception {
        // GIVEN
        Project project = project("New project name");

        // WHEN
        mockMvc.perform(post("{version}/projects",
                             API_VERSION)
                                .contentType(MediaType.APPLICATION_JSON_UTF8)
                                .content(mapper.writeValueAsString(project)))
                // THEN
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name",
                                    is("New project name")));
    }

    @Test
    public void testUpdateProject() throws Exception {
        // GIVEN
        Project project = project("Project to update");
        projectRepository.createProject(project);

        // WHEN
        mockMvc.perform(put("{version}/projects/{projectId}",
                            API_VERSION,
                            project.getId())
                                .contentType(MediaType.APPLICATION_JSON_UTF8)
                                .content(mapper.writeValueAsString(project("Updated project name"))))
                // THEN
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name",
                                    is("Updated project name")));

        assertThat((Optional<Project>) projectRepository.findProjectById(project.getId()))
                .hasValueSatisfying(updatedProject -> {
                    assertThat(updatedProject.getName()).isEqualTo("Updated project name");
                });
    }

    @Test
    public void testDeleteProject() throws Exception {
        // GIVEN
        Project project = project("Project to delete");
        projectRepository.createProject(project);

        // WHEN
        mockMvc.perform(delete("{version}/projects/{projectId}",
                               API_VERSION,
                               project.getId()))
                // THEN
                .andExpect(status().isNoContent());

        assertThat(projectRepository.findProjectById(project.getId())).isEmpty();
    }

    @Test
    public void testCreateProjectsWithModels() throws Exception {
        // GIVEN
        final String processModelId1 = "process_model_id1";
        final String processModelName1 = "Process Model 1";

        final String processModelId2 = "process_model_id2";
        final String processModelName2 = "Process Model 2";

        ModelReference expectedProcessModel1 = new ModelReference(processModelId1,
                                                                  processModelName1);
        ModelReference expectedProcessModel2 = new ModelReference(processModelId2,
                                                                  processModelName2);
        doReturn(expectedProcessModel1)
                .when(modelReferenceService)
                .getResource(eq(PROCESS),
                             eq(expectedProcessModel1.getModelId()));
        doReturn(expectedProcessModel2)
                .when(modelReferenceService)
                .getResource(eq(PROCESS),
                             eq(expectedProcessModel2.getModelId()));

        final String projectWithModelsId = "project_with_models_id";
        final String projectWithModelsName = "project with models";
        Project project = new ProjectEntity(projectWithModelsId,
                                                projectWithModelsName);

        mockMvc.perform(post("{version}/projects",
                             RepositoryRestConfig.API_VERSION)
                                .contentType(MediaType.APPLICATION_JSON_UTF8)
                                .content(mapper.writeValueAsString(project)))
                .andDo(print())
                .andExpect(status().isCreated());

        Model processModel1 = new ModelEntity(processModelId1,
                                              processModelName1,
                                              PROCESS);

        mockMvc.perform(post("{version}/projects/{projectId}/models",
                             RepositoryRestConfig.API_VERSION,
                             projectWithModelsId)
                                .contentType(MediaType.APPLICATION_JSON_UTF8)
                                .content(mapper.writeValueAsString(processModel1)))
                .andDo(print())
                .andExpect(status().isCreated());

        Model processModel2 = new ModelEntity(processModelId2,
                                              processModelName2,
                                              PROCESS);

        mockMvc.perform(post("{version}/projects/{projectId}/models",
                             RepositoryRestConfig.API_VERSION,
                             projectWithModelsId)
                                .contentType(MediaType.APPLICATION_JSON_UTF8)
                                .content(mapper.writeValueAsString(processModel2)))
                .andDo(print())
                .andExpect(status().isCreated());

        // WHEN
        mockMvc.perform(get("{version}/projects/{projectId}/models?type=PROCESS",
                            RepositoryRestConfig.API_VERSION,
                            projectWithModelsId))
                // THEN
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.models",
                                    hasSize(2)))
                .andExpect(jsonPath("$._embedded.models[0].name",
                                    is(processModelName1)))
                .andExpect(jsonPath("$._embedded.models[1].name",
                                    is(processModelName2)));

        mockMvc.perform(delete("/v1/projects/{projectId}",
                               projectWithModelsId))
                .andExpect(status().isNoContent());
    }

    @Test
    public void testExportProject() throws Exception {
        // GIVEN
        String processName1 = "process-model-1";
        String processName2 = "process-model-2";

        Project project = project("project-with-models");
        ModelEntity processModel1 = processModelWithContent(processName1,
                                                            "Process Model Content 1");
        ModelEntity processModel2 = processModelWithExtensions(processName2,
                                                               extensions("var1",
                                                                          "var2"));

        doReturn(processModel1.getData())
                .when(modelReferenceService)
                .getResource(eq(PROCESS),
                             eq(processModel1.getId()));
        doReturn(processModel2.getData())
                .when(modelReferenceService)
                .getResource(eq(PROCESS),
                             eq(processModel2.getId()));

        mockMvc.perform(post("{version}/projects",
                             RepositoryRestConfig.API_VERSION)
                                .contentType(MediaType.APPLICATION_JSON_UTF8)
                                .content(mapper.writeValueAsString(project)))
                .andDo(print())
                .andExpect(status().isCreated());

        mockMvc.perform(post("{version}/projects/{projectId}/models",
                             RepositoryRestConfig.API_VERSION,
                             project.getId())
                                .contentType(MediaType.APPLICATION_JSON_UTF8)
                                .content(mapper.writeValueAsString(processModel1)))
                .andDo(print())
                .andExpect(status().isCreated());

        mockMvc.perform(post("{version}/projects/{projectId}/models",
                             RepositoryRestConfig.API_VERSION,
                             project.getId())
                                .contentType(MediaType.APPLICATION_JSON_UTF8)
                                .content(mapper.writeValueAsString(processModel2)))
                .andDo(print())
                .andExpect(status().isCreated());

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
                        "processes/process-model-1.bpmn20.xml",
                        "processes/process-model-1.json",
                        "processes/process-model-2.bpmn20.xml",
                        "processes/process-model-2.json")
                .hasJsonContentSatisfying("project-with-models.json",
                                          jsonContent -> jsonContent
                                                  .node("name").isEqualTo("project-with-models"))
                .hasContent("processes/process-model-1.bpmn20.xml",
                            "Process Model Content 1")
                .hasJsonContentSatisfying("processes/process-model-1.json",
                                          jsonContent -> jsonContent
                                                  .node("name").isEqualTo("process-model-1")
                                                  .node("type").isEqualTo("PROCESS"))
                .hasContent("processes/process-model-2.bpmn20.xml",
                            "")
                .hasJsonContentSatisfying("processes/process-model-2.json",
                                          jsonContent -> jsonContent
                                                  .node("name").isEqualTo("process-model-2")
                                                  .node("type").isEqualTo("PROCESS")
                                                  .node("extensions.properties").matches(allOf(hasKey("var1"),
                                                                                               hasKey("var2")))
                );
    }

    @Test
    public void testExportProjectWithValidationErrors() throws Exception {
        // GIVEN
        ProjectEntity project = (ProjectEntity) projectRepository
                .createProject(project("project-with-models"));

        String processContent = "Invalid process xml";
        ModelEntity processModel = processModelWithContent(project,
                                                           "process-model",
                                                           processContent);
        doReturn(processModel.getData())
                .when(modelReferenceService)
                .getResource(eq(PROCESS),
                             eq(processModel.getId()));

        modelRepository.createModel(processModel);

        List<ModelValidationError> expectedValidationErrors =
                Arrays.asList(new ModelValidationError(),
                              new ModelValidationError());

        doReturn(expectedValidationErrors)
                .when(modelReferenceService)
                .validateResourceContent(eq(PROCESS),
                                         eq(processContent.getBytes()));

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
    public void testImportProject() throws Exception {
        //GIVEN
        MockMultipartFile zipFile = new MockMultipartFile("file",
                                                          "project-xy.zip",
                                                          "project/zip",
                                                          resourceAsByteArray("project/project-xy.zip"));

        doReturn(mock(ModelReference.class))
                .when(modelReferenceService)
                .getResource(eq(PROCESS),
                             anyString());

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

        verify(modelReferenceService,
               times(1))
                .createResource(eq(PROCESS),
                                modelReferenceNamed("process-x"));

        verify(modelReferenceService,
               times(1))
                .createResource(eq(PROCESS),
                                modelReferenceNamed("process-y"));
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

        doReturn(mock(ModelReference.class))
                .when(modelReferenceService)
                .getResource(eq(PROCESS),
                             anyString());

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