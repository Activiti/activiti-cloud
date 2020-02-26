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

package org.activiti.cloud.services.modeling.rest.controller;

import static org.activiti.cloud.services.modeling.mock.MockMultipartRequestBuilder.putMultipart;
import static org.activiti.cloud.services.modeling.rest.config.RepositoryRestConfig.API_VERSION;
import static org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.activiti.cloud.modeling.api.ContentUpdateListener;
import org.activiti.cloud.modeling.api.JsonModelType;
import org.activiti.cloud.modeling.api.Model;
import org.activiti.cloud.modeling.repository.ModelRepository;
import org.activiti.cloud.services.modeling.config.ModelingRestApplication;
import org.activiti.cloud.services.modeling.entity.ModelEntity;
import org.activiti.cloud.services.modeling.security.WithMockModelerUser;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

/**
 * Integration tests for models rest api dealing with Json models
 */
@ActiveProfiles(profiles = { "test", "generic" })
@RunWith(SpringRunner.class)
@SpringBootTest(classes = ModelingRestApplication.class)
@WebAppConfiguration
@DirtiesContext(classMode = AFTER_EACH_TEST_METHOD)
@WithMockModelerUser
public class GenericJsonModelTypeContentUpdateListenerControllerIT {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ModelRepository modelRepository;

    @Autowired
    JsonModelType genericJsonModelType;

    @SpyBean(name = "genericJsonContentUpdateListener")
    ContentUpdateListener genericJsonContentUpdateListener;

    @SpyBean(name = "genericNonJsonContentUpdateListener")
    ContentUpdateListener genericNonJsonContentUpdateListener;

    private MockMvc mockMvc;

    private static final String GENERIC_MODEL_NAME = "simple-model";

    @Before
    public void setUp() {
        this.mockMvc = webAppContextSetup(context).build();
    }

    @Test
    public void should_callJsonContentUpdateListener_when_updatingModelContent() throws Exception {
        Model genericJsonModel = modelRepository.createModel(new ModelEntity(GENERIC_MODEL_NAME,
                                                                             genericJsonModelType.getName()));

        String stringModel = objectMapper.writeValueAsString(genericJsonModel);

        mockMvc.perform(putMultipart("{version}/models/{modelId}/content",
                                     API_VERSION,
                                     genericJsonModel.getId()).file("file",
                                                                    "simple-model.json",
                                                                    "application/json",
                                                                    stringModel.getBytes()))
                .andExpect(status().isNoContent());

        Mockito.verify(genericJsonContentUpdateListener,
                       Mockito.times(1))
                .execute(Mockito.argThat(model -> model.getId().equals(genericJsonModel.getId())),
                         Mockito.argThat(content -> new String(content.getFileContent()).equals(stringModel)));
    }

    @Test
    public void should_notCallNonJsonContentUpdateListener_when_updatingModelContent() throws Exception {
        Model genericJsonModel = modelRepository.createModel(new ModelEntity(GENERIC_MODEL_NAME,
                                                                             genericJsonModelType.getName()));

        String stringModel = objectMapper.writeValueAsString(genericJsonModel);

        mockMvc.perform(putMultipart("{version}/models/{modelId}/content",
                                     API_VERSION,
                                     genericJsonModel.getId()).file("file",
                                                                    "simple-model.json",
                                                                    "application/json",
                                                                    stringModel.getBytes()))
                .andExpect(status().isNoContent());

        Mockito.verify(genericNonJsonContentUpdateListener,
                       Mockito.times(0))
                .execute(Mockito.any(),
                         Mockito.any());
    }
}
