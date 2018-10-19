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
import org.activiti.cloud.organization.core.rest.client.model.ModelReference;
import org.activiti.cloud.organization.core.rest.client.service.ModelReferenceService;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.context.WebApplicationContext;

import static org.activiti.cloud.organization.api.ProcessModelType.PROCESS;
import static org.activiti.cloud.services.common.util.FileUtils.resourceAsByteArray;
import static org.activiti.cloud.services.organization.mock.MockFactory.application;
import static org.activiti.cloud.services.organization.mock.MockFactory.processModelWithContent;
import static org.activiti.cloud.services.organization.mock.ModelingArgumentMatchers.modelReferenceNamed;
import static org.activiti.cloud.services.organization.rest.config.RepositoryRestConfig.API_VERSION;
import static org.activiti.cloud.services.test.asserts.AssertResponseContent.assertThatResponseContent;
import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasSize;
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

        mockMvc.perform(delete("/v1/applications/{applicationId}",
                               applicationWithModelsId))
                .andExpect(status().isNoContent());
    }

    @Test
    public void testExportApplication() throws Exception {
        // GIVEN
        String processName1 = "process-model-1";
        String processName2 = "process-model-2";

        Application application = application("application-with-models");
        ModelEntity processModel1 = processModelWithContent(processName1,
                                                            "Process Model Content 1");
        ModelEntity processModel2 = processModelWithContent(processName2,
                                                            "Process Model Content 2");

        doReturn(processModel1.getData())
                .when(modelReferenceService)
                .getResource(eq(PROCESS),
                             eq(processModel1.getId()));
        doReturn(processModel2.getData())
                .when(modelReferenceService)
                .getResource(eq(PROCESS),
                             eq(processModel2.getId()));

        mockMvc.perform(post("{version}/applications",
                             RepositoryRestConfig.API_VERSION)
                                .contentType(MediaType.APPLICATION_JSON_UTF8)
                                .content(mapper.writeValueAsString(application)))
                .andDo(print())
                .andExpect(status().isCreated());

        mockMvc.perform(post("{version}/applications/{applicationId}/models",
                             RepositoryRestConfig.API_VERSION,
                             application.getId())
                                .contentType(MediaType.APPLICATION_JSON_UTF8)
                                .content(mapper.writeValueAsString(processModel1)))
                .andDo(print())
                .andExpect(status().isCreated());

        mockMvc.perform(post("{version}/applications/{applicationId}/models",
                             RepositoryRestConfig.API_VERSION,
                             application.getId())
                                .contentType(MediaType.APPLICATION_JSON_UTF8)
                                .content(mapper.writeValueAsString(processModel2)))
                .andDo(print())
                .andExpect(status().isCreated());

        // WHEN
        MvcResult response = mockMvc.perform(
                get("{version}/applications/{applicationId}/export",
                    API_VERSION,
                    application.getId()))
                .andExpect(status().isOk())
                .andReturn();

        // THEN
        assertThatResponseContent(response)
                .isFile()
                .isZip()
                .hasName("application-with-models.zip")
                .hasEntries(
                        "application-with-models.json",
                        "processes/",
                        "processes/process-model-1.bpmn20.xml",
                        "processes/process-model-1.json",
                        "processes/process-model-2.bpmn20.xml",
                        "processes/process-model-2.json")
                .hasJsonContentSatisfying("application-with-models.json",
                                          jsonContent -> jsonContent
                                                  .node("name").isEqualTo("application-with-models"))
                .hasContent("processes/process-model-1.bpmn20.xml",
                            "Process Model Content 1")
                .hasJsonContentSatisfying("processes/process-model-1.json",
                                          jsonContent -> jsonContent
                                                  .node("name").isEqualTo("process-model-1")
                                                  .node("type").isEqualTo("PROCESS")
                                                  .node("version").isEqualTo("0.0.1"))
                .hasContent("processes/process-model-2.bpmn20.xml",
                            "Process Model Content 2")
                .hasJsonContentSatisfying("processes/process-model-2.json",
                                          jsonContent -> jsonContent
                                                  .node("name").isEqualTo("process-model-2")
                                                  .node("type").isEqualTo("PROCESS")
                                                  .node("version").isEqualTo("0.0.1"));
    }

    @Test
    public void testImportApplication() throws Exception {
        //GIVEN
        MockMultipartFile zipFile = new MockMultipartFile("file",
                                                          "application-xy.zip",
                                                          "application/zip",
                                                          resourceAsByteArray("application/application-xy.zip"));

        doReturn(mock(ModelReference.class))
                .when(modelReferenceService)
                .getResource(eq(PROCESS),
                             anyString());

        // WHEN
        mockMvc.perform(multipart("{version}/applications/import",
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
    public void testImportApplicationNoApplicationJsonFile() throws Exception {
        //GIVEN
        MockMultipartFile zipFile = new MockMultipartFile("file",
                                                          "application-xy.zip",
                                                          "application/zip",
                                                          resourceAsByteArray("application/application-xy-no-app.zip"));

        // WHEN
        mockMvc.perform(multipart("{version}/applications/import",
                                  API_VERSION)
                                .file(zipFile)
                                .accept(APPLICATION_JSON_VALUE))
                // THEN
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(status().reason(is("No valid application entry found to import: application-xy.zip")));
    }

    @Test
    public void testImportApplicationInvalidProcessJsonFile() throws Exception {
        //GIVEN
        MockMultipartFile zipFile = new MockMultipartFile("file",
                                                          "application-xy.zip",
                                                          "application/zip",
                                                          resourceAsByteArray("application/application-xy-invalid-process-json.zip"));

        doReturn(mock(ModelReference.class))
                .when(modelReferenceService)
                .getResource(eq(PROCESS),
                             anyString());

        // WHEN
        mockMvc.perform(multipart("{version}/applications/import",
                                  API_VERSION)
                                .file(zipFile)
                                .accept(APPLICATION_JSON_VALUE))
                // THEN
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(status().reason(containsString("Cannot convert json file content to model")));
    }
}