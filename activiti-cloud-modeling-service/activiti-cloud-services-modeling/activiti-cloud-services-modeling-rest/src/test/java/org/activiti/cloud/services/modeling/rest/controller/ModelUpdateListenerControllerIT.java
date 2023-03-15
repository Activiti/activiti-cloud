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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.activiti.cloud.modeling.api.JsonModelType;
import org.activiti.cloud.modeling.api.Model;
import org.activiti.cloud.modeling.api.ModelUpdateListener;
import org.activiti.cloud.modeling.repository.ModelRepository;
import org.activiti.cloud.services.modeling.config.ModelingRestApplication;
import org.activiti.cloud.services.modeling.entity.ModelEntity;
import org.activiti.cloud.services.modeling.security.WithMockModelerUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

/**
 * Integration tests for models rest api dealing with Json models
 */
@ActiveProfiles(profiles = { "test", "generic" })
@SpringBootTest(classes = ModelingRestApplication.class)
@WebAppConfiguration
@WithMockModelerUser
public class ModelUpdateListenerControllerIT {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ModelRepository modelRepository;

    @Autowired
    private JsonModelType genericJsonModelType;

    @SpyBean(name = "genericJsonModelUpdateListener")
    private ModelUpdateListener genericJsonModelUpdateListener;

    @SpyBean(name = "genericNonJsonModelUpdateListener")
    private ModelUpdateListener genericNonJsonModelUpdateListener;

    private MockMvc mockMvc;

    private static final String GENERIC_MODEL_NAME = "simple-model";

    @BeforeEach
    public void setUp() {
        mockMvc = webAppContextSetup(context).build();
    }

    @Test
    public void should_callUpdateListenerMatchingWithModelType_when_updatingModelContent() throws Exception {
        String name = "updated-model-name";
        Model genericJsonModel = modelRepository.createModel(
            new ModelEntity(GENERIC_MODEL_NAME, genericJsonModelType.getName())
        );

        Model updatedModel = new ModelEntity(name, genericJsonModelType.getName());

        mockMvc
            .perform(
                put("/v1/models/{modelId}", genericJsonModel.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updatedModel))
            )
            .andExpect(status().is2xxSuccessful());

        verify(genericJsonModelUpdateListener, times(1))
            .execute(
                argThat(modelToBeUpdated -> modelToBeUpdated.getId().equals(genericJsonModel.getId())),
                argThat(newModel -> newModel.getName().equals(name))
            );

        verify(genericNonJsonModelUpdateListener, never()).execute(any(), any());
    }
}
