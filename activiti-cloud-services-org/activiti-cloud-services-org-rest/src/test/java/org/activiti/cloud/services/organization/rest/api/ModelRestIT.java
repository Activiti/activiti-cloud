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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.activiti.cloud.organization.api.Model;
import org.activiti.cloud.organization.core.model.ModelReference;
import org.activiti.cloud.organization.core.rest.client.ModelService;
import org.activiti.cloud.organization.repository.ApplicationRepository;
import org.activiti.cloud.organization.repository.ModelRepository;
import org.activiti.cloud.services.organization.entity.ApplicationEntity;
import org.activiti.cloud.services.organization.entity.ModelEntity;
import org.activiti.cloud.services.organization.jpa.ApplicationJpaRepository;
import org.activiti.cloud.services.organization.jpa.ModelJpaRepository;
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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.context.WebApplicationContext;

import static org.activiti.cloud.organization.api.ModelType.FORM;
import static org.activiti.cloud.organization.api.ModelType.PROCESS;
import static org.activiti.cloud.services.organization.rest.config.RepositoryRestConfig.API_VERSION;
import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
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
public class ModelRestIT {

    private MockMvc mockMvc;

    @MockBean
    private ModelService modelService;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private ModelRepository modelRepository;

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
        ModelReference expectedFormModel = new ModelReference("form_model_refId",
                                                              "Form Model");
        ModelReference expectedProcessModel = new ModelReference("process_model_refId",
                                                                 "Process Model");

        doReturn(expectedFormModel).when(modelService).getResource(eq(FORM),
                                                                   eq(expectedFormModel.getModelId()));
        doReturn(expectedProcessModel).when(modelService).getResource(eq(PROCESS),
                                                                      eq(expectedProcessModel.getModelId()));

        //given
        final String formModelId = "form_model_id";
        final String formModelName = "Form Model";
        Model formModel = new ModelEntity(formModelId,
                                          formModelName,
                                          FORM,
                                          "form_model_refId");
        formModel = modelRepository.createModel(formModel);
        assertThat(formModel).isNotNull();

        final String processModelId = "process_model_id";
        final String processModelName = "Process Model";
        Model processModel = new ModelEntity(processModelId,
                                             processModelName,
                                             PROCESS,
                                             "process_model_refId");
        processModel = modelRepository.createModel(processModel);
        assertThat(processModel).isNotNull();

        //when
        final ResultActions resultActions = mockMvc.perform(get("{version}/models",
                                                                API_VERSION))
                .andDo(print());

        //then
        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.models",
                                    hasSize(2)))
                .andExpect(jsonPath("$._embedded.models[0].name",
                                    is(formModelName)))
                .andExpect(jsonPath("$._embedded.models[1].name",
                                    is(processModelName)));
    }

    @Test
    public void testCreateModel() throws Exception {

        //given
        final String formModelId = "form_model_id";
        final String formModelName = "Form Model";
        Model formModel = new ModelEntity(formModelId,
                                          formModelName,
                                          FORM,
                                          "form_model_refId");

        ModelReference expectedProcessModel = new ModelReference("form_model_refId",
                                                                 "Form Model");
        doReturn(expectedProcessModel).when(modelService).getResource(eq(FORM),
                                                                      eq("form_model_refId"));

        mockMvc.perform(post("{version}/models",
                             API_VERSION)
                                .contentType(MediaType.APPLICATION_JSON_UTF8)
                                .content(mapper.writeValueAsString(formModel)))
                .andDo(print())
                .andExpect(status().isCreated());
    }

    @Test
    public void testGetModel() throws Exception {
        //given
        final String processModelId = "process_model_id";
        Model processModel = new ModelEntity(processModelId,
                                             "Process Model",
                                             PROCESS,
                                             processModelId);

        ModelReference expectedProcessModel = new ModelReference(processModelId,
                                                                 "Process Model");
        doReturn(expectedProcessModel).when(modelService).getResource(eq(PROCESS),
                                                                      eq(processModelId));
        //when
        assertThat(modelRepository.createModel(processModel)).isNotNull();

        //then
        mockMvc.perform(get("{version}/models/{modelId}",
                            API_VERSION,
                            processModelId))
                .andExpect(status().isOk());
    }

    @Test
    public void testCreateProcessModelInApplication() throws Exception {
        //given
        final String processModelId = "process_model_id";
        Model processModel = new ModelEntity(processModelId,
                                             "Process Model",
                                             PROCESS,
                                             "process_model_refId");

        ModelReference expectedProcessModel = new ModelReference("process_model_refId",
                                                                 "Process Model");
        doReturn(expectedProcessModel).when(modelService).getResource(eq(PROCESS),
                                                                      eq("process_model_refId"));

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
                                             PROCESS,
                                             "process_model_refId");
        assertThat(modelRepository.createModel(processModel)).isNotNull();

        Model newModel = new ModelEntity(processModelId,
                                         "New Process Model",
                                         PROCESS,
                                         "process_model_refId");

        //when
        mockMvc.perform(put("{version}/models/{modelId}",
                            API_VERSION,
                            processModelId)
                                .contentType(MediaType.APPLICATION_JSON_UTF8)
                                .content(mapper.writeValueAsString(newModel)))
                .andExpect(status().isNoContent());
    }

    @Test
    public void testDeleteApplication() throws Exception {
        //given
        final String processModelId = "process_model_id";
        Model processModel = new ModelEntity(processModelId,
                                             "Process Model",
                                             PROCESS,
                                             "process_model_refId");
        assertThat(modelRepository.createModel(processModel)).isNotNull();

        //when
        mockMvc.perform(delete("{version}/models/{modelId}",
                               API_VERSION,
                               processModelId))
                .andExpect(status().isNoContent());

        //then
        assertThat(modelRepository.findModelById(processModelId)).isEmpty();
    }
}
