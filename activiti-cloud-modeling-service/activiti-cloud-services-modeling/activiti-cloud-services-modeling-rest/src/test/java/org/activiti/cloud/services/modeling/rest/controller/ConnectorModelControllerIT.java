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

import static org.activiti.cloud.services.modeling.asserts.AssertResponse.assertThatResponse;
import static org.activiti.cloud.services.modeling.mock.MockFactory.connectorModel;
import static org.activiti.cloud.services.modeling.mock.MockFactory.project;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.activiti.cloud.modeling.api.Model;
import org.activiti.cloud.modeling.api.Project;
import org.activiti.cloud.modeling.repository.ModelRepository;
import org.activiti.cloud.modeling.repository.ProjectRepository;
import org.activiti.cloud.services.modeling.config.ModelingRestApplication;
import org.activiti.cloud.services.modeling.security.WithMockModelerUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Integration tests for models rest api dealing with connector models
 */
@ActiveProfiles("test")
@SpringBootTest(classes = ModelingRestApplication.class)
@WebAppConfiguration
@DirtiesContext(classMode = AFTER_EACH_TEST_METHOD)
@WithMockModelerUser
public class ConnectorModelControllerIT {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ModelRepository modelRepository;

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @BeforeEach
    public void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    public void should_returnStatusCreatedAndConnectorName_when_creatingConnectorModel() throws Exception {
        Project project = projectRepository.createProject(project("project-with-connectors"));

        mockMvc
            .perform(post("/v1/projects/{projectId}/models", project.getId()).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(connectorModel("connector-name"))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name", equalTo("connector-name")));
    }

    @Test
    public void should_throwRequiredFieldException_when_creatingConnectorWithNameNull() throws Exception {
        Project project = projectRepository.createProject(project("project-with-connectors"));

        ResultActions resultActions = mockMvc
            .perform(post("/v1/projects/{projectId}/models", project.getId()).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(connectorModel(null))));

        resultActions.andExpect(status().isBadRequest());
        assertThatResponse(resultActions.andReturn()).isValidationException().hasValidationErrorCodes("field.required")
            .hasValidationErrorMessages("The model name is required");
    }

    @Test
    public void should_throwEmptyFieldException_when_creatingConnectorModelWithNameEmpty() throws Exception {
        Project project = projectRepository.createProject(project("project-with-connectors"));

        ResultActions resultActions = mockMvc
            .perform(post("/v1/projects/{projectId}/models", project.getId()).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(connectorModel(""))));

        resultActions.andExpect(status().isBadRequest());
        assertThatResponse(resultActions.andReturn()).isValidationException().hasValidationErrorCodes("field.empty")
            .hasValidationErrorMessages("The model name cannot be empty");
    }

    @Test
    public void should_throwTooLongNameException_when_createConnectorModelWithNameTooLong() throws Exception {
        Project project = projectRepository.createProject(project("project-with-connectors"));

        ResultActions resultActions = mockMvc
            .perform(post("/v1/projects/{projectId}/models", project.getId()).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(connectorModel("123456789_123456789_1234567"))));

        resultActions.andExpect(status().isBadRequest());
        assertThatResponse(resultActions.andReturn()).isValidationException()
            .hasValidationErrorCodes("length.greater")
            .hasValidationErrorMessages("The model name length cannot be greater than 26: '123456789_123456789_1234567'");
    }

    @Test
    public void should_throwModelInvalidException_when_creatingConnectorModelWithNameWithUnderscore() throws Exception {
        Project project = projectRepository.createProject(project("project-with-connectors"));
        String name = "name_with_underscore";

        ResultActions resultActions = mockMvc
            .perform(post("/v1/projects/{projectId}/models", project.getId()).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(connectorModel(name))))
            .andExpect(status().isConflict());

        assertThat(resultActions.andReturn().getResponse().getErrorMessage())
            .isEqualTo("The model name should follow DNS-1035 conventions:"
                           + " it must consist of lower case alphanumeric characters or '-',"
                           + " and must start and end with an alphanumeric character: 'name_with_underscore'");
    }

    @Test
    public void should_throwModelNameInvalidException_when_creatingConnectorModelWithNameWithUppercase() throws Exception {
        Project project = projectRepository.createProject(project("project-with-connectors"));

        ResultActions resultActions = mockMvc
            .perform(post("/v1/projects/{projectId}/models", project.getId()).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(connectorModel("NameWithUppercase"))))
            .andExpect(status().isConflict());

        assertThat(resultActions.andReturn().getResponse().getErrorMessage())
            .isEqualTo("The model name should follow DNS-1035 conventions:"
                           + " it must consist of lower case alphanumeric characters or '-',"
                           + " and must start and end with an alphanumeric character: 'NameWithUppercase'");
    }

    @Test
    public void should_returnStatusOKAndConnectorName_when_updatingConnectorModel() throws Exception {
        Model connectorModel = modelRepository.createModel(connectorModel("connector-name"));

        mockMvc
            .perform(put("/v1/models/{modelId}", connectorModel.getId()).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(connectorModel("updated-connector-name"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name", equalTo("updated-connector-name")));
    }

    @Test
    public void should_returnStatusOKAndConnectorName_when_updatingConnectorModelWithNameNull() throws Exception {
        Model connectorModel = modelRepository.createModel(connectorModel("connector-name"));

        ResultActions updateResult = mockMvc
            .perform(put("/v1/models/{modelId}", connectorModel.getId()).contentType(MediaType.APPLICATION_JSON)
                         .content(objectMapper.writeValueAsString(connectorModel(null))))
            .andExpect(status().isConflict());

        assertThat(updateResult.andReturn().getResponse().getErrorMessage())
            .isEqualTo("The model name is required");
    }

    @Test
    public void should_throwEmptyNameException_when_updatingConnectorModelWithNameEmpty() throws Exception {
        Model connectorModel = modelRepository.createModel(connectorModel("connector-name"));

        ResultActions resultActions = mockMvc
            .perform(put("/v1/models/{modelId}", connectorModel.getId()).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(connectorModel(""))));

        resultActions.andExpect(status().isBadRequest());
        assertThatResponse(resultActions.andReturn()).isValidationException()
            .hasValidationErrorCodes("field.empty")
            .hasValidationErrorMessages("The model name cannot be empty");
    }

    @Test
    public void should_throwBadNameException_when_updatingConnectorModelWithNameTooLong() throws Exception {
        Model connectorModel = modelRepository.createModel(connectorModel("connector-name"));

        ResultActions resultActions = mockMvc
            .perform(put("/v1/models/{modelId}", connectorModel.getId()).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(connectorModel("123456789_123456789_1234567"))));

        resultActions.andExpect(status().isBadRequest());
        assertThatResponse(resultActions.andReturn()).isValidationException()
            .hasValidationErrorCodes("length.greater")
            .hasValidationErrorMessages(
                "The model name length cannot be greater than 26: '123456789_123456789_1234567'");
    }

    @Test
    public void should_throwModelInvalidException_when_updatingConnectorModelWithNameWithUnderscore() throws Exception {
        Model connectorModel = modelRepository.createModel(connectorModel("connector-name"));

        ResultActions resultActions = mockMvc
            .perform(put("/v1/models/{modelId}", connectorModel.getId()).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(connectorModel("name_with_underscore"))));

        resultActions.andExpect(status().isConflict());

        assertThat(resultActions.andReturn().getResponse().getErrorMessage())
            .isEqualTo("The model name should follow DNS-1035 conventions:"
                           + " it must consist of lower case alphanumeric characters or '-',"
                           + " and must start and end with an alphanumeric character: 'name_with_underscore'");
    }

    @Test
    public void should_throwModelInvalidException_when_updatingConnectorModelWithNameWithUppercase() throws Exception {
        Model connectorModel = modelRepository.createModel(connectorModel("connector-name"));

        ResultActions resultActions = mockMvc
            .perform(put("/v1/models/{modelId}", connectorModel.getId()).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(connectorModel("NameWithUppercase"))));

        resultActions.andExpect(status().isConflict());

        assertThat(resultActions.andReturn().getResponse().getErrorMessage())
            .isEqualTo("The model name should follow DNS-1035 conventions:"
                           + " it must consist of lower case alphanumeric characters or '-',"
                           + " and must start and end with an alphanumeric character: 'NameWithUppercase'");
    }
}
