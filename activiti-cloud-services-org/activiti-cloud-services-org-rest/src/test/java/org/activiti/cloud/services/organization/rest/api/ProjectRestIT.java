/*
 * Copyright 2017 Alfresco, Inc. and/or its affiliates.
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

package org.activiti.cloud.services.organization.rest.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.activiti.cloud.organization.core.model.Model;
import org.activiti.cloud.organization.core.model.ModelReference;
import org.activiti.cloud.organization.core.model.Project;
import org.activiti.cloud.services.organization.config.Application;
import org.activiti.cloud.services.organization.config.RepositoryRestConfig;
import org.activiti.cloud.services.organization.jpa.ModelRepository;
import org.activiti.cloud.services.organization.jpa.ProjectRepository;
import org.activiti.cloud.services.organization.mock.MockModelRestServiceServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class)
@WebAppConfiguration
public class ProjectRestIT {

    private MockMvc mockMvc;

    @Autowired
    private RestTemplate restTemplate;

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
        projectRepository.deleteAllInBatch();
        modelRepository.deleteAllInBatch();
    }

    @Test
    public void getProjects() throws Exception {

        //given
        final String projectId = "project_id";
        final String projectName = "Project";
        Project project = new Project(projectId,
                                      projectName);

        //when
        project = projectRepository.save(project);
        assertThat(project).isNotNull();

        //then
        mockMvc.perform(get("{version}/projects",
                            RepositoryRestConfig.API_VERSION))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.projects",
                                    hasSize(1)))
                .andExpect(jsonPath("$._embedded.projects[0].name",
                                    is(projectName)));
    }

    @Test
    public void createProjectsWithModels() throws Exception {
        MockModelRestServiceServer.createServer(restTemplate)
                .expectFormModelCreation()
                .expectProcessModelCreation()
                .expectFormModelRequest(new ModelReference("ref_model_form_id",
                                                           "Form Model"))
                .expectProcessModelRequest(new ModelReference("ref_process_model_id",
                                                              "Process Model"));

        //given
        final String projectWithModelsId = "project_with_models_id";
        final String projectWithModelsName = "Project with models";
        Project project = new Project(projectWithModelsId,
                                      projectWithModelsName);

        // create a project
        mockMvc.perform(post("{version}/projects",
                             RepositoryRestConfig.API_VERSION)
                                .contentType(MediaType.APPLICATION_JSON_UTF8)
                                .content(mapper.writeValueAsString(project)))
                .andDo(print())
                .andExpect(status().isCreated());

        // create a form model
        final String formModelId = "form_model_id";
        final String formModelName = "Form Model";
        Model modelForm = new Model(formModelId,
                                    formModelName,
                                    Model.ModelType.FORM,
                                    "ref_model_form_id");

        mockMvc.perform(post("{version}/models",
                             RepositoryRestConfig.API_VERSION)
                                .contentType(MediaType.APPLICATION_JSON_UTF8)
                                .content(mapper.writeValueAsString(modelForm)))
                .andDo(print())
                .andExpect(status().isCreated());

        // create a process-model
        final String processModelId = "process_model_id";
        final String processModelName = "Process Model";
        Model processModel = new Model(processModelId,
                                       processModelName,
                                       Model.ModelType.PROCESS_MODEL,
                                       "ref_process_model_id");

        mockMvc.perform(post("{version}/models",
                             RepositoryRestConfig.API_VERSION)
                                .contentType(MediaType.APPLICATION_JSON_UTF8)
                                .content(mapper.writeValueAsString(processModel)))
                .andDo(print())
                .andExpect(status().isCreated());

        //when
        String uriList = "http://localhost" + RepositoryRestConfig.API_VERSION + "/models/" + formModelId + "\n"
                + "http://localhost" + RepositoryRestConfig.API_VERSION + "/models/" + processModelId;

        mockMvc.perform(put("{version}/projects/{projectId}/models",
                            RepositoryRestConfig.API_VERSION,
                            projectWithModelsId)
                                .contentType("text/uri-list")
                                .content(uriList))
                .andDo(print())
                .andExpect(status().isNoContent());

        //then
        mockMvc.perform(get("{version}/projects/{projectId}/models",
                            RepositoryRestConfig.API_VERSION,
                            projectWithModelsId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.models",
                                    hasSize(2)))
                .andExpect(jsonPath("$._embedded.models[0].name",
                                    is(formModelName)))
                .andExpect(jsonPath("$._embedded.models[1].name",
                                    is(processModelName)));
    }
}