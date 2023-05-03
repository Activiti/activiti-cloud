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

import static org.activiti.cloud.services.common.util.FileUtils.resourceAsByteArray;
import static org.activiti.cloud.services.modeling.Resources.MODEL_REPOSITORY;
import static org.activiti.cloud.services.modeling.mock.MockMultipartRequestBuilder.putMultipart;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.activiti.cloud.modeling.api.ContentUpdateListener;
import org.activiti.cloud.modeling.api.Model;
import org.activiti.cloud.modeling.api.ModelType;
import org.activiti.cloud.modeling.repository.ModelRepository;
import org.activiti.cloud.services.modeling.config.ModelingRestApplication;
import org.activiti.cloud.services.modeling.entity.ModelEntity;
import org.activiti.cloud.services.modeling.security.WithMockModelerUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceAccessMode;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.ResourceLocks;
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
@ResourceLocks({ @ResourceLock(value = MODEL_REPOSITORY, mode = ResourceAccessMode.READ_WRITE) })
public class GenericNonJsonModelTypeContentUpdateListenerControllerIT {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ModelRepository modelRepository;

    @Autowired
    ModelType genericNonJsonModelType;

    @SpyBean(name = "genericJsonContentUpdateListener")
    ContentUpdateListener genericJsonContentUpdateListener;

    @SpyBean(name = "genericNonJsonContentUpdateListener")
    ContentUpdateListener genericNonJsonContentUpdateListener;

    private MockMvc mockMvc;
    private Model genericNonJsonModel;

    private static final String GENERIC_MODEL_NAME = "simple-model";

    @BeforeEach
    public void setUp() {
        this.mockMvc = webAppContextSetup(context).build();
        genericNonJsonModel =
            modelRepository.createModel(new ModelEntity(GENERIC_MODEL_NAME, genericNonJsonModelType.getName()));
    }

    @AfterEach
    public void cleanUp() {
        modelRepository.deleteModel(genericNonJsonModel);
    }

    @Test
    public void should_notCallJsonContentUpdateListener_when_updatingModelContent() throws Exception {
        mockMvc
            .perform(
                putMultipart("/v1/models/{modelId}/content", genericNonJsonModel.getId())
                    .file(
                        "file",
                        "simple-model.bin",
                        "application/octet-stream",
                        resourceAsByteArray("generic/model-simple.bin")
                    )
            )
            .andExpect(status().isNoContent());

        verify(genericJsonContentUpdateListener, times(0)).execute(any(), any());
    }

    @Test
    public void should_callNonJsonContentUpdateListener_when_updatingModelContent() throws Exception {
        byte[] fileContent = resourceAsByteArray("generic/model-simple.bin");

        mockMvc
            .perform(
                putMultipart("/v1/models/{modelId}/content", genericNonJsonModel.getId())
                    .file("file", "simple-model.json", "application/octet-stream", fileContent)
            )
            .andExpect(status().isNoContent());

        verify(genericNonJsonContentUpdateListener, times(1))
            .execute(
                argThat(model -> model.getId().equals(genericNonJsonModel.getId())),
                argThat(content -> new String(content.getFileContent()).equals(new String(fileContent)))
            );
    }
}
