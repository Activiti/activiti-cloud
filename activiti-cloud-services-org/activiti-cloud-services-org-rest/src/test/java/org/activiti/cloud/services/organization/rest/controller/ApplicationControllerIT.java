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

import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.activiti.cloud.organization.api.Application;
import org.activiti.cloud.organization.api.Model;
import org.activiti.cloud.organization.core.rest.client.service.ModelReferenceService;
import org.activiti.cloud.organization.core.rest.client.model.ModelReference;
import org.activiti.cloud.organization.repository.ApplicationRepository;
import org.activiti.cloud.organization.repository.ModelRepository;
import org.activiti.cloud.services.organization.config.OrganizationRestApplication;
import org.activiti.cloud.services.organization.entity.ApplicationEntity;
import org.activiti.cloud.services.organization.entity.ModelEntity;
import org.activiti.cloud.services.organization.jpa.ApplicationJpaRepository;
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
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import static org.activiti.cloud.organization.api.ProcessModelType.PROCESS;
import static org.activiti.cloud.services.organization.mock.MockFactory.application;
import static org.activiti.cloud.services.organization.rest.config.RepositoryRestConfig.API_VERSION;
import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = OrganizationRestApplication.class)
@WebAppConfiguration
public class ApplicationControllerIT {

    private MockMvc mockMvc;

    @MockBean
    private ModelReferenceService modelReferenceService;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ApplicationRepository applicationRepository;

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
        ((ApplicationJpaRepository) applicationRepository).deleteAllInBatch();
    }

    @Test
    public void testGetApplications() throws Exception {

        // GIVEN
        applicationRepository.createApplication(application("Application1"));
        applicationRepository.createApplication(application("Application2"));

        // WHEN
        mockMvc.perform(get("{version}/applications",
                            RepositoryRestConfig.API_VERSION))
                // THEN
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.applications",
                                    hasSize(2)))
                .andExpect(jsonPath("$._embedded.applications[0].name",
                                    is("Application1")))
                .andExpect(jsonPath("$._embedded.applications[1].name",
                                    is("Application2")));
    }

    @Test
    public void testGetApplication() throws Exception {
        // GIVEN
        Application application = application("Existing Application");
        applicationRepository.createApplication(application);

        // WHEN
        mockMvc.perform(get("{version}/applications/{applicationId}",
                            API_VERSION,
                            application.getId()))
                // THEN
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name",
                                    is("Existing Application")));
    }

    @Test
    public void testCreateApplication() throws Exception {
        // GIVEN
        Application application = application("New application name");

        // WHEN
        mockMvc.perform(post("{version}/applications",
                             API_VERSION)
                                .contentType(MediaType.APPLICATION_JSON_UTF8)
                                .content(mapper.writeValueAsString(application)))
                // THEN
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name",
                                    is("New application name")));
    }

    @Test
    public void testUpdateApplication() throws Exception {
        // GIVEN
        Application application = application("Application to update");
        applicationRepository.createApplication(application);

        // WHEN
        mockMvc.perform(put("{version}/applications/{applicationId}",
                            API_VERSION,
                            application.getId())
                                .contentType(MediaType.APPLICATION_JSON_UTF8)
                                .content(mapper.writeValueAsString(application("Updated application name"))))
                // THEN
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name",
                                    is("Updated application name")));

        assertThat((Optional<Application>) applicationRepository.findApplicationById(application.getId()))
                .hasValueSatisfying(updatedApplication -> {
                    assertThat(updatedApplication.getName()).isEqualTo("Updated application name");
                });
    }

    @Test
    public void testDeleteApplication() throws Exception {
        // GIVEN
        Application application = application("Application to delete");
        applicationRepository.createApplication(application);

        // WHEN
        mockMvc.perform(delete("{version}/applications/{applicationId}",
                               API_VERSION,
                               application.getId()))
                // THEN
                .andExpect(status().isNoContent());

        assertThat(applicationRepository.findApplicationById(application.getId())).isEmpty();
    }

    @Test
    public void testCreateApplicationsWithModels() throws Exception {
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

        final String applicationWithModelsId = "application_with_models_id";
        final String applicationWithModelsName = "application with models";
        Application application = new ApplicationEntity(applicationWithModelsId,
                                                        applicationWithModelsName);

        mockMvc.perform(post("{version}/applications",
                             RepositoryRestConfig.API_VERSION)
                                .contentType(MediaType.APPLICATION_JSON_UTF8)
                                .content(mapper.writeValueAsString(application)))
                .andDo(print())
                .andExpect(status().isCreated());

        Model processModel1 = new ModelEntity(processModelId1,
                                              processModelName1,
                                              PROCESS);

        mockMvc.perform(post("{version}/applications/{applicationId}/models",
                             RepositoryRestConfig.API_VERSION,
                             applicationWithModelsId)
                                .contentType(MediaType.APPLICATION_JSON_UTF8)
                                .content(mapper.writeValueAsString(processModel1)))
                .andDo(print())
                .andExpect(status().isCreated());

        Model processModel2 = new ModelEntity(processModelId2,
                                              processModelName2,
                                              PROCESS);

        mockMvc.perform(post("{version}/applications/{applicationId}/models",
                             RepositoryRestConfig.API_VERSION,
                             applicationWithModelsId)
                                .contentType(MediaType.APPLICATION_JSON_UTF8)
                                .content(mapper.writeValueAsString(processModel2)))
                .andDo(print())
                .andExpect(status().isCreated());

        // WHEN
        mockMvc.perform(get("{version}/applications/{applicationId}/models",
                            RepositoryRestConfig.API_VERSION,
                            applicationWithModelsId))
                // THEN
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.models",
                                    hasSize(2)))
                .andExpect(jsonPath("$._embedded.models[0].name",
                                    is(processModelName1)))
                .andExpect(jsonPath("$._embedded.models[1].name",
                                    is(processModelName2)));
    }
}