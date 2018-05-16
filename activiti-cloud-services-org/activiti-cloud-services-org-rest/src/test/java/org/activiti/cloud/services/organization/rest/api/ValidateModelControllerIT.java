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

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.activiti.cloud.organization.core.model.Model;
import org.activiti.cloud.organization.core.rest.client.ModelService;
import org.activiti.cloud.organization.core.service.ValidationErrorRepresentation;
import org.activiti.cloud.services.organization.config.Application;
import org.activiti.cloud.services.organization.config.RepositoryRestConfig;
import org.activiti.cloud.services.organization.jpa.ModelRepository;
import org.activiti.validation.ValidationError;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;
import static org.activiti.cloud.organization.core.model.Model.ModelType.PROCESS_MODEL;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class)
@WebAppConfiguration
public class ValidateModelControllerIT {

    private MockMvc mockMvc;

    @MockBean
    private ModelRepository modelRepository;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @MockBean
    private ModelService modelService;

    @Before
    public void setUp() {

        this.mockMvc = webAppContextSetup(webApplicationContext).build();
    }

    @Test
    public void validateModel() throws Exception {

        // given
        MockMultipartFile file = new MockMultipartFile("file",
                                                       "diagram.bpm",
                                                       "text/plain",
                                                       "BPMN diagram".getBytes());
        when(modelRepository.findById("model_id")).thenReturn(Optional.of(new Model("model_id",
                                                                                    "Process-Model",
                                                                                    Model.ModelType.PROCESS_MODEL,
                                                                                    "model_ref_id")));

        List<ValidationErrorRepresentation> expectedValidationErrors =
                Arrays.asList(new ValidationErrorRepresentation(new ValidationError()),
                              new ValidationErrorRepresentation(new ValidationError()));

        doReturn(expectedValidationErrors).when(modelService).validateResourceContent(PROCESS_MODEL,
                                                                                      file.getBytes());

        // when
        final ResultActions resultActions = mockMvc.perform(multipart("{version}/models/{model_id}/validate",
                                                                      RepositoryRestConfig.API_VERSION,
                                                                      "model_id").file(file))
                .andDo(print());

        // then
        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.validationErrorRepresentations",
                                    hasSize(2)));
    }

    @Test
    public void validateModelThatNotExistsShouldThrowException() throws Exception {
        // given
        MockMultipartFile file = new MockMultipartFile("file",
                                                       "diagram.bpm",
                                                       "text/plain",
                                                       "BPMN diagram".getBytes());
        // when
        final ResultActions resultActions = mockMvc.perform(multipart("{version}/models/{model_id}/validate",
                                                                      RepositoryRestConfig.API_VERSION,
                                                                      "model_id").file(file))
                .andDo(print());

        // then
        resultActions.andExpect(status().isNotFound());
    }
}
