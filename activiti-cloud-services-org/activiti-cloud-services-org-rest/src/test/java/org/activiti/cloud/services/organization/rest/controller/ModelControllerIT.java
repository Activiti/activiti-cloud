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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.activiti.cloud.organization.api.Application;
import org.activiti.cloud.organization.api.Model;
import org.activiti.cloud.organization.api.ModelValidationError;
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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.util.NestedServletException;

import static org.activiti.cloud.organization.api.ProcessModelType.PROCESS;
import static org.activiti.cloud.services.common.util.ContentTypeUtils.CONTENT_TYPE_XML;
import static org.activiti.cloud.services.common.util.FileUtils.resourceAsByteArray;
import static org.activiti.cloud.services.organization.mock.MockFactory.application;
import static org.activiti.cloud.services.organization.mock.MockFactory.processModelWithContent;
import static org.activiti.cloud.services.organization.mock.ModelingArgumentMatchers.modelReferenceNamed;
import static org.activiti.cloud.services.organization.rest.config.RepositoryRestConfig.API_VERSION;
import static org.activiti.cloud.services.test.asserts.AssertResponseContent.assertThatResponseContent;
import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
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
public class ModelControllerIT {

    private MockMvc mockMvc;

    @MockBean
    private ModelReferenceService modelReferenceService;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private ModelRepository modelRepository;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

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
    public void testGetModels() throws Exception {
        final String processModelId1 = "process_model_id1";
        final String processModelName1 = "Process Model 1";

        final String processModelId2 = "process_model_id2";
        final String processModelName2 = "Process Model 2";

        ModelReference expectedProcessModel1 = new ModelReference(processModelId1,
                                                                  "Process Model 1");
        ModelReference expectedProcessModel2 = new ModelReference(processModelId2,
                                                                  "Process Model 2");

        doReturn(expectedProcessModel1).when(modelReferenceService).getResource(eq(PROCESS),
                                                                                eq(expectedProcessModel1.getModelId()));
        doReturn(expectedProcessModel2).when(modelReferenceService).getResource(eq(PROCESS),
                                                                                eq(expectedProcessModel2.getModelId()));

        //given
        Model processModel1 = new ModelEntity(processModelId1,
                                              processModelName1,
                                              PROCESS);
        processModel1 = modelRepository.createModel(processModel1);
        assertThat(processModel1).isNotNull();

        Model processModel2 = new ModelEntity(processModelId2,
                                              processModelName2,
                                              PROCESS);
        processModel2 = modelRepository.createModel(processModel2);
        assertThat(processModel2).isNotNull();

        //when
        final ResultActions resultActions = mockMvc.perform(get("{version}/models",
                                                                API_VERSION))
                .andDo(print());

        //then
        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.models",
                                    hasSize(2)))
                .andExpect(jsonPath("$._embedded.models[0].name",
                                    is(processModelName1)))
                .andExpect(jsonPath("$._embedded.models[1].name",
                                    is(processModelName2)));
    }

    @Test
    public void testCreateModel() throws Exception {

        //given
        final String processModelId = "process_model_id";
        final String processModelName = "Process Model";
        Model processModel = new ModelEntity(processModelId,
                                             processModelName,
                                             PROCESS);

        ModelReference expectedProcessModel = new ModelReference(processModelId,
                                                                 "Process Model");
        doReturn(expectedProcessModel).when(modelReferenceService).getResource(eq(PROCESS),
                                                                               eq(processModelId));

        mockMvc.perform(post("{version}/models",
                             API_VERSION)
                                .contentType(MediaType.APPLICATION_JSON_UTF8)
                                .content(mapper.writeValueAsString(processModel)))
                .andDo(print())
                .andExpect(status().isCreated());
    }

    @Test
    public void testCreateModelOfUnknownType() throws Exception {

        //given
        Model formModel = new ModelEntity("id",
                                          "name",
                                          "FORM");

        mockMvc.perform(post("{version}/models",
                             API_VERSION)
                                .contentType(MediaType.APPLICATION_JSON_UTF8)
                                .content(mapper.writeValueAsString(formModel)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testCreateModelProducerException() throws Exception {

        //given
        final String processModelId = "process_model_id";
        final String processModelName = "Process Model";
        Model processModel = new ModelEntity(processModelId,
                                             processModelName,
                                             PROCESS);

        doThrow(new RuntimeException()).when(modelReferenceService).createResource(eq(PROCESS),
                                                                                   any(ModelReference.class));

        expectedException.expect(NestedServletException.class);
        mockMvc.perform(post("{version}/models",
                             API_VERSION)
                                .contentType(MediaType.APPLICATION_JSON_UTF8)
                                .content(mapper.writeValueAsString(processModel)))
                .andDo(print())
                .andExpect(status().isInternalServerError());
    }

    @Test
    public void testGetModel() throws Exception {
        //given
        final String processModelId = "process_model_id";
        Model processModel = new ModelEntity(processModelId,
                                             "Process Model",
                                             PROCESS);

        ModelReference expectedProcessModel = new ModelReference(processModelId,
                                                                 "Process Model");
        doReturn(expectedProcessModel).when(modelReferenceService).getResource(eq(PROCESS),
                                                                               eq(processModelId));
        //when
        assertThat(modelRepository.createModel(processModel)).isNotNull();

        //then
        mockMvc.perform(get("{version}/models/{modelId}",
                            API_VERSION,
                            processModelId))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    public void testCreateProcessModelInApplication() throws Exception {
        //given
        final String processModelId = "process_model_id";
        Model processModel = new ModelEntity(processModelId,
                                             "Process Model",
                                             PROCESS);

        ModelReference expectedProcessModel = new ModelReference(processModelId,
                                                                 "Process Model");
        doReturn(expectedProcessModel).when(modelReferenceService).getResource(eq(PROCESS),
                                                                               eq(processModelId));

        String parentApplicationId = "parent_application_id";
        applicationRepository.createApplication(new ApplicationEntity(parentApplicationId,
                                                                      "Parent Application"));

        //when
        mockMvc.perform(post("{version}/applications/{applicationId}/models",
                             API_VERSION,
                             parentApplicationId)
                                .contentType(MediaType.APPLICATION_JSON_UTF8)
                                .content(mapper.writeValueAsString(processModel)))
                .andExpect(status().isCreated());

        //then
        assertThat(modelRepository.findModelById(processModelId)).isNotEmpty();
    }

    @Test
    public void testUpdateApplication() throws Exception {
        //given
        final String processModelId = "process_model_id";
        Model processModel = new ModelEntity(processModelId,
                                             "Process Model",
                                             PROCESS);
        assertThat(modelRepository.createModel(processModel)).isNotNull();

        ModelReference expectedProcessModel = new ModelReference(processModelId,
                                                                 "Process Model");

        doReturn(expectedProcessModel).when(modelReferenceService).getResource(eq(PROCESS),
                                                                               eq(expectedProcessModel.getModelId()));

        Model newModel = new ModelEntity();
        newModel.setType(PROCESS);
        newModel.setName("New Process Model");

        //when
        mockMvc.perform(put("{version}/models/{modelId}",
                            API_VERSION,
                            processModelId)
                                .contentType(MediaType.APPLICATION_JSON_UTF8)
                                .content(mapper.writeValueAsString(newModel)))
                .andExpect(status().isOk());
    }

    @Test
    public void testDeleteApplication() throws Exception {
        //given
        final String processModelId = "process_model_id";
        Model processModel = new ModelEntity(processModelId,
                                             "Process Model",
                                             PROCESS);
        assertThat(modelRepository.createModel(processModel)).isNotNull();

        //when
        mockMvc.perform(delete("{version}/models/{modelId}",
                               API_VERSION,
                               processModelId))
                .andExpect(status().isNoContent());

        //then
        assertThat(modelRepository.findModelById(processModelId)).isEmpty();
    }

    @Test
    public void testGetModelTypes() throws Exception {

        mockMvc.perform(get("{version}/model-types",
                            API_VERSION))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.model-types",
                                    hasSize(1)))
                .andExpect(jsonPath("$._embedded.model-types[0].name",
                                    is(PROCESS)));
    }

    @Test
    public void validateProcessModel() throws Exception {

        // given
        MockMultipartFile file = new MockMultipartFile("file",
                                                       "diagram.bpm",
                                                       "text/plain",
                                                       "BPMN diagram".getBytes());
        Model processModel = new ModelEntity("model_id",
                                             "Process-Model",
                                             PROCESS);
        assertThat(modelRepository.createModel(processModel)).isNotNull();

        List<ModelValidationError> expectedValidationErrors =
                Arrays.asList(new ModelValidationError(),
                              new ModelValidationError());

        doReturn(expectedValidationErrors).when(modelReferenceService).validateResourceContent(PROCESS,
                                                                                               file.getBytes());

        // when
        final ResultActions resultActions = mockMvc
                .perform(multipart("{version}/models/{model_id}/validate",
                                   RepositoryRestConfig.API_VERSION,
                                   "model_id").file(file))
                .andDo(print());

        // then
        resultActions.andExpect(status().isBadRequest());
    }

    @Test
    public void validateModelThatNotExistsShouldThrowException() throws Exception {
        // given
        MockMultipartFile file = new MockMultipartFile("file",
                                                       "diagram.bpm",
                                                       "text/plain",
                                                       "BPMN diagram".getBytes());
        // when
        final ResultActions resultActions = mockMvc
                .perform(multipart("{version}/models/{model_id}/validate",
                                   RepositoryRestConfig.API_VERSION,
                                   "model_id").file(file))
                .andDo(print());

        // then
        resultActions.andExpect(status().isNotFound());
    }

    @Test
    public void testExportModel() throws Exception {
        //GIVEN
        ModelEntity processModel1 = processModelWithContent("process_model_id",
                                                            "Process Model Content 1");
        assertThat(modelRepository.createModel(processModel1)).isNotNull();

        doReturn(processModel1.getData())
                .when(modelReferenceService)
                .getResource(eq(PROCESS),
                             eq(processModel1.getId()));

        // WHEN
        MvcResult response = mockMvc.perform(
                get("{version}/models/{modelId}/export",
                    API_VERSION,
                    processModel1.getId()))
                .andExpect(status().isOk())
                .andReturn();

        // THEN
        assertThatResponseContent(response)
                .isFile()
                .hasName("process_model_id.bpmn20.xml")
                .hasContentType(CONTENT_TYPE_XML)
                .hasContent("Process Model Content 1");
    }

    @Test
    public void testExportModelNotFound() throws Exception {
        // WHEN
        mockMvc.perform(
                get("{version}/models/not_existing_model/export",
                    API_VERSION))
                // THEN
                .andExpect(status().isNotFound())
                .andReturn();
    }

    @Test
    public void testImportModel() throws Exception {
        //GIVEN
        Application parentApplication = application("Parent Application");
        applicationRepository.createApplication(parentApplication);

        MockMultipartFile zipFile = new MockMultipartFile("file",
                                                          "x-19022.bpmn20.xml",
                                                          "application/xml",
                                                          resourceAsByteArray("process/x-19022.bpmn20.xml"));

        doReturn(mock(ModelReference.class))
                .when(modelReferenceService)
                .getResource(eq(PROCESS),
                             anyString());

        // WHEN
        mockMvc.perform(multipart("{version}/applications/{applicationId}/models/import",
                                  API_VERSION,
                                  parentApplication.getId())
                                .file(zipFile)
                                .param("type",
                                       PROCESS)
                                .accept(APPLICATION_JSON_VALUE))
                .andDo(print())
                .andExpect(status().isCreated());

        // THEN
        verify(modelReferenceService,
               times(1))
                .createResource(eq(PROCESS),
                                modelReferenceNamed("x-19022"));
    }

    @Test
    public void testImportModelWrongFileName() throws Exception {
        //GIVEN
        Application parentApplication = application("Parent Application");
        applicationRepository.createApplication(parentApplication);

        MockMultipartFile zipFile = new MockMultipartFile("file",
                                                          "x-19022",
                                                          "application/xml",
                                                          resourceAsByteArray("process/x-19022.bpmn20.xml"));

        // WHEN
        mockMvc.perform(multipart("{version}/applications/{applicationId}/models/import",
                                  API_VERSION,
                                  parentApplication.getId())
                                .file(zipFile)
                                .param("type",
                                       PROCESS)
                                .accept(APPLICATION_JSON_VALUE))
                // THEN
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(status().reason(is("Unexpected extension was found for file to import model of type PROCESS: x-19022")));
    }

    @Test
    public void testImportModelWrongModelType() throws Exception {
        //GIVEN
        Application parentApplication = application("Parent Application");
        applicationRepository.createApplication(parentApplication);

        MockMultipartFile zipFile = new MockMultipartFile("file",
                                                          "x-19022.xml",
                                                          "application/xml",
                                                          resourceAsByteArray("process/x-19022.bpmn20.xml"));

        // WHEN
        mockMvc.perform(multipart("{version}/applications/{applicationId}/models/import",
                                  API_VERSION,
                                  parentApplication.getId())
                                .file(zipFile)
                                .param("type",
                                       "WRONG_TYPE"))
                // THEN
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(status().reason(is("Unknown model type: WRONG_TYPE")));
    }

    @Test
    public void testImportModelApplicationNotFound() throws Exception {
        //GIVEN
        MockMultipartFile zipFile = new MockMultipartFile("file",
                                                          "x-19022.xml",
                                                          "application/xml",
                                                          resourceAsByteArray("process/x-19022.bpmn20.xml"));

        // WHEN
        mockMvc.perform(multipart("{version}/applications/not_existing_application/models/import",
                                  API_VERSION)
                                .file(zipFile)
                                .param("type",
                                       PROCESS))
                // THEN
                .andDo(print())
                .andExpect(status().isNotFound());
    }
}
