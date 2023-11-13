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
package org.activiti.cloud.services.modeling.rest.controller;

import static org.activiti.cloud.services.common.util.FileUtils.resourceAsByteArray;
import static org.activiti.cloud.services.modeling.mock.MockFactory.project;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.activiti.cloud.modeling.api.Project;
import org.activiti.cloud.modeling.repository.ProjectRepository;
import org.activiti.cloud.services.common.security.test.support.WithActivitiMockUser;
import org.activiti.cloud.services.modeling.config.ModelingRestApplication;
import org.activiti.cloud.services.modeling.entity.ProjectEntity;
import org.activiti.cloud.services.modeling.security.WithMockModelerUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(classes = ModelingRestApplication.class)
@WebAppConfiguration
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
@Transactional
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ProjectControllerExistingNameIT {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ObjectMapper mapper;

    @BeforeEach
    public void setUp() {
        mockMvc = webAppContextSetup(webApplicationContext).build();
    }

    /*
     The following test is to be run before other tests in this class
     in order to create the projects with a different user.
    */
    @Test
    @Order(1)
    @Transactional(propagation = Propagation.NEVER)
    @WithActivitiMockUser(username = "otherUser", roles = { "ACTIVITI_MODELER" })
    public void should_createProjectWithOtherUser() throws Exception {
        projectRepository.createProject(project("application xy"));
        projectRepository.createProject(project("creating-project"));
        projectRepository.createProject(project("updating-project"));
        projectRepository.createProject(project("copying-project"));
        assertThat((Page<ProjectEntity>) projectRepository.getProjects(Pageable.ofSize(50), null, null))
            .hasSize(4)
            .extracting(ProjectEntity::getCreatedBy)
            .containsOnly("otherUser");
    }

    @Test
    @Order(2)
    @Transactional(propagation = Propagation.NEVER)
    @WithMockModelerUser
    public void should_returnStatusCreated_when_importingProjectWithExistingNameCreatedByOtherUser() throws Exception {
        MockMultipartFile zipFile = new MockMultipartFile(
            "file",
            "project-xy.zip",
            "project/zip",
            resourceAsByteArray("project/project-xy.zip")
        );

        mockMvc
            .perform(multipart("/v1/projects/import").file(zipFile).accept(APPLICATION_JSON_VALUE))
            .andExpect(status().isCreated());

        assertThat((Page<ProjectEntity>) projectRepository.getProjects(Pageable.ofSize(50), null, null))
            .extracting(ProjectEntity::getCreatedBy, ProjectEntity::getName)
            .contains(tuple("otherUser", "application xy"), tuple("testuser", "application xy"));
    }

    @Test
    @Order(3)
    @Transactional(propagation = Propagation.NEVER)
    @WithMockModelerUser
    public void should_returnStatusCreated_when_creatingProjectWithExistingNameCreatedByOtherUser() throws Exception {
        mockMvc
            .perform(
                post("/v1/projects")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(project("creating-project")))
            )
            .andExpect(status().isCreated());

        assertThat((Page<ProjectEntity>) projectRepository.getProjects(Pageable.ofSize(50), null, null))
            .extracting(ProjectEntity::getCreatedBy, ProjectEntity::getName)
            .contains(tuple("otherUser", "creating-project"), tuple("testuser", "creating-project"));
    }

    @Test
    @Order(4)
    @Transactional(propagation = Propagation.NEVER)
    @WithMockModelerUser
    public void should_returnStatusOk_when_updatingProjectWithExistingNameCreatedByOtherUser() throws Exception {
        Project project = projectRepository.createProject(project("project-to-update"));

        mockMvc
            .perform(
                put("/v1/projects/{projectId}", project.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(project("updating-project")))
            )
            .andExpect(status().isOk());

        assertThat((Page<ProjectEntity>) projectRepository.getProjects(Pageable.ofSize(50), null, null))
            .extracting(ProjectEntity::getCreatedBy, ProjectEntity::getName)
            .contains(tuple("otherUser", "updating-project"), tuple("testuser", "updating-project"));
    }

    @Test
    @Order(5)
    @Transactional(propagation = Propagation.NEVER)
    @WithMockModelerUser
    public void should_returnStatusOk_when_copyingProjectWithExistingNameCreatedByOtherUser() throws Exception {
        Project project = projectRepository.createProject(project("project-to-copy"));

        mockMvc
            .perform(post("/v1/projects/{projectId}/copy?name=copying-project", project.getId()))
            .andExpect(status().isOk());

        assertThat((Page<ProjectEntity>) projectRepository.getProjects(Pageable.ofSize(50), null, null))
            .extracting(ProjectEntity::getCreatedBy, ProjectEntity::getName)
            .contains(tuple("otherUser", "copying-project"), tuple("testuser", "copying-project"));
    }
}
