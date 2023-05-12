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

import static org.activiti.cloud.services.modeling.mock.MockMultipartRequestBuilder.putMultipart;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

import com.fasterxml.jackson.core.PrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.activiti.cloud.modeling.api.ContentUpdateListener;
import org.activiti.cloud.modeling.api.JsonModelType;
import org.activiti.cloud.modeling.api.Model;
import org.activiti.cloud.modeling.repository.ModelRepository;
import org.activiti.cloud.services.modeling.config.ModelingRestApplication;
import org.activiti.cloud.services.modeling.entity.ModelEntity;
import org.activiti.cloud.services.modeling.security.WithMockModelerUser;
import org.activiti.cloud.services.modeling.service.utils.ModelExtensionsPrettyPrinter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/**
 * Integration tests for models rest api dealing with Json models
 */
@ActiveProfiles(profiles = { "test", "generic" })
@SpringBootTest(classes = ModelingRestApplication.class)
@Transactional
@WebAppConfiguration
@WithMockModelerUser
@ResourceLock(value = GenericJsonModelTypeContentUpdateListenerControllerIT.GENERIC_MODEL_NAME)
public class GenericJsonModelTypeContentUpdateListenerControllerIT {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    private final PrettyPrinter jsonPrettyPrinter = new ModelExtensionsPrettyPrinter().withEmptyObjectSeparator(false);

    @Autowired
    private ModelRepository modelRepository;

    @Autowired
    JsonModelType genericJsonModelType;

    @SpyBean(name = "genericJsonContentUpdateListener")
    ContentUpdateListener genericJsonContentUpdateListener;

    @SpyBean(name = "genericNonJsonContentUpdateListener")
    ContentUpdateListener genericNonJsonContentUpdateListener;

    private MockMvc mockMvc;

    protected static final String GENERIC_MODEL_NAME = "simple-model";

    private Model genericJsonModel;

    @BeforeEach
    public void setUp() {
        this.mockMvc = webAppContextSetup(context).build();
        genericJsonModel =
            modelRepository.createModel(new ModelEntity(GENERIC_MODEL_NAME, genericJsonModelType.getName()));
    }

    @AfterEach
    public void cleanUp() {
        modelRepository.deleteModel(genericJsonModel);
    }

    @Test
    public void should_callJsonContentUpdateListener_when_updatingModelContent() throws Exception {
        genericJsonModel =
            modelRepository.createModel(new ModelEntity(GENERIC_MODEL_NAME, genericJsonModelType.getName()));

        String stringModel = objectMapper.writer(jsonPrettyPrinter).writeValueAsString(genericJsonModel);

        mockMvc
            .perform(
                putMultipart("/v1/models/{modelId}/content", genericJsonModel.getId())
                    .file("file", "simple-model.json", "application/json", stringModel.getBytes())
            )
            .andExpect(status().isNoContent());

        verify(genericJsonContentUpdateListener, times(1))
            .execute(
                argThat(model -> model.getId().equals(genericJsonModel.getId())),
                argThat(content -> new String(content.getFileContent()).equals(stringModel))
            );
    }

    @Test
    public void should_notCallNonJsonContentUpdateListener_when_updatingModelContent() throws Exception {
        String stringModel = objectMapper.writer(jsonPrettyPrinter).writeValueAsString(genericJsonModel);

        mockMvc
            .perform(
                putMultipart("/v1/models/{modelId}/content", genericJsonModel.getId())
                    .file("file", "simple-model.json", "application/json", stringModel.getBytes())
            )
            .andExpect(status().isNoContent());

        verify(genericNonJsonContentUpdateListener, times(0)).execute(any(), any());
    }
}
