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
import org.activiti.cloud.organization.api.ModelValidationError;
import org.activiti.cloud.organization.api.Project;
import org.activiti.cloud.organization.repository.ModelRepository;
import org.activiti.cloud.organization.repository.ProjectRepository;
import org.activiti.cloud.services.organization.config.OrganizationRestApplication;
import org.activiti.cloud.services.organization.entity.ProjectEntity;
import org.activiti.cloud.services.organization.rest.config.RepositoryRestConfig;
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
import static org.activiti.cloud.services.organization.mock.MockFactory.extensions;
import static org.activiti.cloud.services.organization.mock.MockFactory.processModelWithContent;
import static org.activiti.cloud.services.organization.mock.MockFactory.project;
import static org.activiti.cloud.services.organization.rest.config.RepositoryRestConfig.API_VERSION;
import static org.activiti.cloud.services.test.asserts.AssertResponseContent.assertThatResponseContent;
import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
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
    private ObjectMapper mapper;

    @Before
    public void setUp() {
        this.mockMvc = webAppContextSetup(webApplicationContext).build();
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
        Project project = projectRepository.createProject(project("Existing Project"));

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
        // WHEN
        mockMvc.perform(post("{version}/projects",
                             API_VERSION)
                                .contentType(MediaType.APPLICATION_JSON_UTF8)
                                .content(mapper.writeValueAsString(project("New Project"))))
                // THEN
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name",
                                    is("New Project")));
    }

    @Test
    public void testUpdateProject() throws Exception {
        // GIVEN
        Project project = projectRepository.createProject(project("Project to update"));

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
        Project project = projectRepository.createProject(project("Project to delete"));

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
        modelRepository.createModel(processModelWithContent(project,
                                                            "process-model",
                                                            extensions("var1",
                                                                       "var2"),
                                                            resourceAsByteArray("process/x-19022.bpmn20.xml")));
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
                        "processes/process-model.json")
                .hasJsonContentSatisfying("project-with-models.json",
                                          jsonContent -> jsonContent
                                                  .node("name").isEqualTo("project-with-models"))
                .hasContent("processes/process-model.bpmn20.xml",
                            resourceAsByteArray("process/x-19022.bpmn20.xml"))
                .hasJsonContentSatisfying("processes/process-model.json",
                                          jsonContent -> jsonContent
                                                  .node("name").isEqualTo("process-model")
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