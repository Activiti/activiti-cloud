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

import static org.activiti.cloud.services.common.util.FileUtils.resourceAsByteArray;
import static org.activiti.cloud.services.organization.mock.MockMultipartRequestBuilder.putMultipart;
import static org.activiti.cloud.services.organization.rest.config.RepositoryRestConfig.API_VERSION;
import static org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

import org.activiti.cloud.organization.api.ContentUpdateListener;
import org.activiti.cloud.organization.api.Model;
import org.activiti.cloud.organization.api.ModelType;
import org.activiti.cloud.organization.repository.ModelRepository;
import org.activiti.cloud.services.organization.config.OrganizationRestApplication;
import org.activiti.cloud.services.organization.entity.ModelEntity;
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

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Integration tests for models rest api dealing with Json models
 */
@ActiveProfiles(profiles = { "test", "generic" })
@RunWith(SpringRunner.class)
@SpringBootTest(classes = OrganizationRestApplication.class)
@WebAppConfiguration
@DirtiesContext(classMode = AFTER_EACH_TEST_METHOD)
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

    private static final String GENERIC_MODEL_NAME = "simple-model";

    @Before
    public void setUp() {
        this.mockMvc = webAppContextSetup(context).build();
    }

    @Test
    public void should_notCallJsonContentUpdateListener_when_updatingModelContent() throws Exception {
        Model genericNonJsonModel = modelRepository.createModel(new ModelEntity(GENERIC_MODEL_NAME,
                                                                                genericNonJsonModelType.getName()));

        mockMvc.perform(putMultipart("{version}/models/{modelId}/content",
                                     API_VERSION,
                                     genericNonJsonModel.getId()).file("file",
                                                                       "simple-model.bin",
                                                                       "application/octet-stream",
                                                                       resourceAsByteArray("generic/model-simple.bin")))
                .andExpect(status().isNoContent());

        Mockito.verify(genericJsonContentUpdateListener,
                       Mockito.times(0))
                .execute(Mockito.any(),
                         Mockito.any());

    }

    @Test
    public void should_callNonJsonContentUpdateListener_when_updatingModelContent() throws Exception {

        Model genericNonJsonModel = modelRepository.createModel(new ModelEntity(GENERIC_MODEL_NAME,
                                                                                genericNonJsonModelType.getName()));
        
        byte[] fileContent = resourceAsByteArray("generic/model-simple.bin");

        mockMvc.perform(putMultipart("{version}/models/{modelId}/content",
                                     API_VERSION,
                                     genericNonJsonModel.getId()).file("file",
                                                                       "simple-model.json",
                                                                       "application/octet-stream",
                                                                       fileContent))
                .andExpect(status().isNoContent());

        Mockito.verify(genericNonJsonContentUpdateListener,
                       Mockito.times(1))
                .execute(Mockito.argThat(model -> model.getId().equals(genericNonJsonModel.getId())),
                         Mockito.argThat(content -> new String(content.getFileContent()).equals(new String(fileContent))));

    }
}
