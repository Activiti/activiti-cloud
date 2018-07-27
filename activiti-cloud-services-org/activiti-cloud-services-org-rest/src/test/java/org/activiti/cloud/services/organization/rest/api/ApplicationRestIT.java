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

package org.activiti.cloud.services.organization.rest.api;

import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.activiti.cloud.organization.api.Application;
import org.activiti.cloud.organization.api.Model;
import org.activiti.cloud.organization.core.model.ModelReference;
import org.activiti.cloud.organization.core.rest.client.ModelService;
import org.activiti.cloud.organization.repository.ApplicationRepository;
import org.activiti.cloud.organization.repository.ModelRepository;
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

import static org.activiti.cloud.organization.api.ModelType.FORM;
import static org.activiti.cloud.organization.api.ModelType.PROCESS;
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
@SpringBootTest(classes = org.activiti.cloud.services.organization.config.OrganizationRestApplication.class)
@WebAppConfiguration
public class ApplicationRestIT {

    private MockMvc mockMvc;

    @MockBean
    private ModelService modelService;

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

        //given
        final String applicationId = "application_id";
        final String applicationName = "Application";
        Application application = new ApplicationEntity(applicationId,
                                                        applicationName);

        //when
        application = applicationRepository.createApplication(application);
        assertThat(application).isNotNull();

        //then
        mockMvc.perform(get("{version}/applications",
                            RepositoryRestConfig.API_VERSION))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.applications",
                                    hasSize(1)))
                .andExpect(jsonPath("$._embedded.applications[0].name",
                                    is(applicationName)));
    }

    @Test
    public void testCreateApplicationsWithModels() throws Exception {
        final String formModelId = "form_model_id";
        final String formModelName = "Form Model";

        final String processModelId = "process_model_id";
        final String processModelName = "Process Model";

        ModelReference expectedFormModel = new ModelReference(formModelId,
                                                              formModelName);
        ModelReference expectedProcessModel = new ModelReference(processModelId,
                                                                 processModelName);
        doReturn(expectedFormModel)
                .when(modelService)
                .getResource(eq(FORM),
                             eq(expectedFormModel.getModelId()));
        doReturn(expectedProcessModel)
                .when(modelService)
                .getResource(eq(PROCESS),
                             eq(expectedProcessModel.getModelId()));

        //given
        final String applicationWithModelsId = "application_with_models_id";
        final String applicationWithModelsName = "application with models";
        Application application = new ApplicationEntity(applicationWithModelsId,
                                                        applicationWithModelsName);

        // create an application
        mockMvc.perform(post("{version}/applications",
                             RepositoryRestConfig.API_VERSION)
                                .contentType(MediaType.APPLICATION_JSON_UTF8)
                                .content(mapper.writeValueAsString(application)))
                .andDo(print())
                .andExpect(status().isCreated());

        // create a form model
        Model modelForm = new ModelEntity(formModelId,
                                          formModelName,
                                          FORM);

        mockMvc.perform(post("{version}/models",
                             RepositoryRestConfig.API_VERSION)
                                .contentType(MediaType.APPLICATION_JSON_UTF8)
                                .content(mapper.writeValueAsString(modelForm)))
                .andDo(print())
                .andExpect(status().isCreated());

        // create a process-model
        Model processModel = new ModelEntity(processModelId,
                                             processModelName,
                                             PROCESS
        );

        mockMvc.perform(post("{version}/models",
                             RepositoryRestConfig.API_VERSION)
                                .contentType(MediaType.APPLICATION_JSON_UTF8)
                                .content(mapper.writeValueAsString(processModel)))
                .andDo(print())
                .andExpect(status().isCreated());

        //when
        String uriList = "http://localhost" + RepositoryRestConfig.API_VERSION + "/models/" + formModelId + "\n"
                + "http://localhost" + RepositoryRestConfig.API_VERSION + "/models/" + processModelId;

        mockMvc.perform(put("{version}/applications/{applicationId}/models",
                            RepositoryRestConfig.API_VERSION,
                            applicationWithModelsId)
                                .contentType("text/uri-list")
                                .content(uriList))
                .andDo(print())
                .andExpect(status().isNoContent());

        //then
        mockMvc.perform(get("{version}/applications/{applicationId}/models",
                            RepositoryRestConfig.API_VERSION,
                            applicationWithModelsId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.models",
                                    hasSize(2)))
                .andExpect(jsonPath("$._embedded.models[0].name",
                                    is(formModelName)))
                .andExpect(jsonPath("$._embedded.models[1].name",
                                    is(processModelName)));
    }

    @Test
    public void testGetApplication() throws Exception {
        //given
        final String applicationId = "application_id";
        final String applicationName = "Application";
        Application application = new ApplicationEntity(applicationId,
                                                        applicationName);

        //when
        application = applicationRepository.createApplication(application);
        assertThat(application).isNotNull();

        //then
        mockMvc.perform(get("{version}/applications/{applicationId}",
                            API_VERSION,
                            applicationId))
                .andExpect(status().isOk());
    }

    @Test
    public void testUpdateApplication() throws Exception {
        //given
        final String applicationId = "application_id";
        final Application savedApplication = applicationRepository.createApplication(new ApplicationEntity(applicationId,
                                                                                                           "Application name"));
        assertThat(savedApplication).isNotNull();

        assertThat((Optional<Application>) applicationRepository.findApplicationById(applicationId))
                .hasValueSatisfying(application -> {
                    assertThat(application.getName()).isEqualTo("Application name");
                });

        Application newApplication = new ApplicationEntity(applicationId,
                                                           "New application name");

        //when
        mockMvc.perform(put("{version}/applications/{applicationId}",
                            API_VERSION,
                            applicationId)
                                .contentType(MediaType.APPLICATION_JSON_UTF8)
                                .content(mapper.writeValueAsString(newApplication)))
                .andExpect(status().isNoContent());

        //then
        assertThat((Optional<Application>) applicationRepository.findApplicationById(applicationId))
                .hasValueSatisfying(application -> {
                    assertThat(application.getName()).isEqualTo("New application name");
                });
    }

    @Test
    public void testDeleteApplication() throws Exception {
        //given
        final String applicationId = "application_id";
        final Application savedApplication = applicationRepository.createApplication(new ApplicationEntity(applicationId,
                                                                                                           "Application"));
        assertThat(savedApplication).isNotNull();

        //when
        mockMvc.perform(delete("{version}/applications/{applicationId}",
                               API_VERSION,
                               applicationId))
                .andExpect(status().isNoContent());

        //then
        assertThat(applicationRepository.findApplicationById(applicationId)).isEmpty();
    }
}