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
import org.activiti.cloud.services.organization.config.Application;
import org.activiti.cloud.services.organization.config.RepositoryRestConfig;
import org.activiti.cloud.services.organization.jpa.ModelRepository;
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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
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

    @Autowired
    private RestTemplate restTemplate;

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

        MockModelRestServiceServer.createServer(restTemplate)
                .expectFormModelRequest(expectedFormModel)
                .expectProcessModelRequest(expectedProcessModel);

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
        final ResultActions resultActions = mockMvc.perform(get("{version}/models",
                                                                RepositoryRestConfig.API_VERSION))
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
    public void createModel() throws Exception {
        MockModelRestServiceServer.createServer(restTemplate)
                .expectFormModelCreation();

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
