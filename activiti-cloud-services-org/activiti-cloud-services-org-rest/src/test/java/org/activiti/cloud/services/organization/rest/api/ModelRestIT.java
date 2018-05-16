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
import org.activiti.cloud.organization.core.model.Model;
import org.activiti.cloud.organization.core.model.ModelReference;
import org.activiti.cloud.organization.core.rest.client.ModelService;
import org.activiti.cloud.services.organization.config.Application;
import org.activiti.cloud.services.organization.config.RepositoryRestConfig;
import org.activiti.cloud.services.organization.jpa.ModelRepository;
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

import static org.activiti.cloud.organization.core.model.Model.ModelType.FORM;
import static org.activiti.cloud.organization.core.model.Model.ModelType.PROCESS_MODEL;
import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class)
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
    private ModelRepository modelRepository;

    @Before
    public void setUp() {

        this.mockMvc = webAppContextSetup(webApplicationContext).build();
    }

    @After
    public void tearDown() {
        modelRepository.deleteAllInBatch();
    }

    @Test
    public void getModels() throws Exception {
        final ModelReference expectedFormModel = new ModelReference("form_model_refId",
                                                                    "Form Model");
        final ModelReference expectedProcessModel = new ModelReference("process_model_refId",
                                                                       "Process Model");

        doReturn(expectedFormModel).when(modelService).getResource(FORM,
                                                                   expectedFormModel.getModelId());
        doReturn(expectedProcessModel).when(modelService).getResource(PROCESS_MODEL,
                                                                      expectedProcessModel.getModelId());

        //given
        final String formModelId = "form_model_id";
        final String formModelName = "Form Model";
        Model formModel = new Model(formModelId,
                                    formModelName,
                                    Model.ModelType.FORM,
                                    "form_model_refId");
        formModel = modelRepository.save(formModel);
        assertThat(formModel).isNotNull();

        final String processModelId = "process_model_id";
        final String processModelName = "Process Model";
        Model processModel = new Model(processModelId,
                                       processModelName,
                                       Model.ModelType.PROCESS_MODEL,
                                       "process_model_refId");
        processModel = modelRepository.save(processModel);
        assertThat(processModel).isNotNull();

        //when
        ResultActions resultActions = mockMvc.perform(get("{version}/models/form_model_id",
                                                          RepositoryRestConfig.API_VERSION))
                .andDo(print());

        //then
        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("name",
                                    is(formModelName)));

        //when
        resultActions = mockMvc.perform(get("{version}/models/process_model_id",
                                            RepositoryRestConfig.API_VERSION))
                .andDo(print());

        //then
        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("name",
                                    is(processModelName)));
    }

    @Test
    public void createModel() throws Exception {

        //given
        final String formModelId = "form_model_id";
        final String formModelName = "Form Model";
        Model formModel = new Model(formModelId,
                                    formModelName,
                                    Model.ModelType.FORM,
                                    "form_model_refId");

        mockMvc.perform(post("{version}/models",
                             RepositoryRestConfig.API_VERSION)
                                .contentType(MediaType.APPLICATION_JSON_UTF8)
                                .content(mapper.writeValueAsString(formModel)))
                .andDo(print())
                .andExpect(status().isCreated());
    }
}
