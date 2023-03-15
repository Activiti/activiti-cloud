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
import static org.activiti.cloud.services.modeling.mock.MockFactory.project;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.activiti.cloud.modeling.api.ContentUpdateListener;
import org.activiti.cloud.modeling.api.JsonModelType;
import org.activiti.cloud.modeling.api.Model;
import org.activiti.cloud.modeling.api.Project;
import org.activiti.cloud.modeling.repository.ModelRepository;
import org.activiti.cloud.modeling.repository.ProjectRepository;
import org.activiti.cloud.services.modeling.config.ModelingRestApplication;
import org.activiti.cloud.services.modeling.entity.ModelEntity;
import org.activiti.cloud.services.modeling.security.WithMockModelerUser;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Integration tests for models rest api dealing with Json models
 */
@ActiveProfiles(profiles = { "test", "generic" })
@SpringBootTest(classes = ModelingRestApplication.class)
@WebAppConfiguration
@DirtiesContext(classMode = AFTER_EACH_TEST_METHOD)
@WithMockModelerUser
public class GenericJsonModelTypeControllerIT {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ModelRepository modelRepository;

    @Autowired
    JsonModelType genericJsonModelType;

    @SpyBean(name = "genericJsonContentUpdateListener")
    ContentUpdateListener genericJsonContentUpdateListener;

    @SpyBean(name = "genericNonJsonContentUpdateListener")
    ContentUpdateListener genericNonJsonContentUpdateListener;

    private static final String GENERIC_MODEL_NAME = "simple-model";

    private static final String GENERIC_PROJECT_NAME = "project-with-generic-model";

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @BeforeEach
    public void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    public void should_returnStatusCreatedAndModelName_when_creatingGenericJsonModel() throws Exception {
        String name = GENERIC_MODEL_NAME;

        Project project = projectRepository.createProject(project(GENERIC_PROJECT_NAME));

        mockMvc
            .perform(
                post("/v1/projects/{projectId}/models", project.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(new ModelEntity(name, genericJsonModelType.getName())))
            )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name", equalTo(GENERIC_MODEL_NAME)));
    }

    @Test
    public void should_throwRequiredFieldException_when_creatingGenericJsonModelWithNameNull() throws Exception {
        String name = null;

        Project project = projectRepository.createProject(project(GENERIC_PROJECT_NAME));

        ResultActions resultActions = mockMvc.perform(
            post("/v1/projects/{projectId}/models", project.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ModelEntity(name, genericJsonModelType.getName())))
        );

        resultActions.andExpect(status().isBadRequest());
        assertThatResponse(resultActions.andReturn())
            .isValidationException()
            .hasValidationErrorCodes("field.required")
            .hasValidationErrorMessages("The model name is required");
    }

    @Test
    public void should_throwEmptyNameException_when_creatingGenericJsonModelWithNameEmpty() throws Exception {
        String name = "";

        Project project = projectRepository.createProject(project(GENERIC_PROJECT_NAME));

        ResultActions resultActions = mockMvc.perform(
            post("/v1/projects/{projectId}/models", project.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ModelEntity(name, genericJsonModelType.getName())))
        );

        resultActions.andExpect(status().isBadRequest());
        assertThatResponse(resultActions.andReturn())
            .isValidationException()
            .hasValidationErrorCodes("field.empty")
            .hasValidationErrorMessages("The model name cannot be empty");
    }

    @Test
    public void should_throwTooLongNameException_when_creatingGenericJsonModelWithNameTooLong() throws Exception {
        String name = "123456789_123456789_1234567";

        Project project = projectRepository.createProject(project(GENERIC_PROJECT_NAME));

        ResultActions resultActions = mockMvc.perform(
            post("/v1/projects/{projectId}/models", project.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ModelEntity(name, genericJsonModelType.getName())))
        );

        resultActions.andExpect(status().isBadRequest());
        assertThatResponse(resultActions.andReturn())
            .isValidationException()
            .hasValidationErrorCodes("length.greater")
            .hasValidationErrorMessages(
                "The model name length cannot be greater than 26: '123456789_123456789_1234567'"
            );
    }

    @Test
    public void should_create_when_creatingGenericJsonModelWithNameWithUnderscore() throws Exception {
        String name = "name_with_underscore";

        Project project = projectRepository.createProject(project(GENERIC_PROJECT_NAME));

        ResultActions resultActions = mockMvc.perform(
            post("/v1/projects/{projectId}/models", project.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ModelEntity(name, genericJsonModelType.getName())))
        );

        resultActions.andExpect(status().isCreated());
    }

    @Test
    public void should_create_when_creatingGenericJsonModelWithNameWithUppercase() throws Exception {
        String name = "NameWithUppercase";

        Project project = projectRepository.createProject(project(GENERIC_PROJECT_NAME));

        ResultActions resultActions = mockMvc.perform(
            post("/v1/projects/{projectId}/models", project.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ModelEntity(name, genericJsonModelType.getName())))
        );

        resultActions.andExpect(status().isCreated());
    }

    @Test
    public void should_returnStatusOKAndModelName_when_updatingGenericJsonModel() throws Exception {
        String name = "updated-connector-name";

        Model genericJsonModel = modelRepository.createModel(
            new ModelEntity(GENERIC_MODEL_NAME, genericJsonModelType.getName())
        );

        mockMvc
            .perform(
                put("/v1/models/{modelId}", genericJsonModel.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(new ModelEntity(name, genericJsonModelType.getName())))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name", equalTo("updated-connector-name")));
    }

    @Test
    public void should_returnStatusOKAndModelName_when_updatingGenericJsonModelWithNameNull() throws Exception {
        String name = null;

        Model genericJsonModel = modelRepository.createModel(
            new ModelEntity(GENERIC_MODEL_NAME, genericJsonModelType.getName())
        );

        mockMvc
            .perform(
                put("/v1/models/{modelId}", genericJsonModel.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(new ModelEntity(name, genericJsonModelType.getName())))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name", equalTo(GENERIC_MODEL_NAME)));
    }

    @Test
    public void should_throwBadNameException_when_updatingGenericJsonModelWithNameEmpty() throws Exception {
        String name = "";

        Model genericJsonModel = modelRepository.createModel(
            new ModelEntity(GENERIC_MODEL_NAME, genericJsonModelType.getName())
        );

        ResultActions resultActions = mockMvc.perform(
            put("/v1/models/{modelId}", genericJsonModel.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ModelEntity(name, genericJsonModelType.getName())))
        );

        resultActions.andExpect(status().isBadRequest());
        assertThatResponse(resultActions.andReturn())
            .isValidationException()
            .hasValidationErrorCodes("field.empty")
            .hasValidationErrorMessages("The model name cannot be empty");
    }

    @Test
    public void should_throwBadNameException_when_updatingGenericJsonModelWithNameTooLong() throws Exception {
        String name = "123456789_123456789_1234567";

        Model genericJsonModel = modelRepository.createModel(
            new ModelEntity(GENERIC_MODEL_NAME, genericJsonModelType.getName())
        );

        ResultActions resultActions = mockMvc.perform(
            put("/v1/models/{modelId}", genericJsonModel.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ModelEntity(name, genericJsonModelType.getName())))
        );

        resultActions.andExpect(status().isBadRequest());
        assertThatResponse(resultActions.andReturn())
            .isValidationException()
            .hasValidationErrorCodes("length.greater")
            .hasValidationErrorMessages(
                "The model name length cannot be greater than 26: '123456789_123456789_1234567'"
            );
    }

    @Test
    public void should_update_when_updatingGenericJsonModelWithNameWithUnderscore() throws Exception {
        String name = "name_with_underscore";

        Model genericJsonModel = modelRepository.createModel(
            new ModelEntity(GENERIC_MODEL_NAME, genericJsonModelType.getName())
        );

        ResultActions resultActions = mockMvc.perform(
            put("/v1/models/{modelId}", genericJsonModel.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ModelEntity(name, genericJsonModelType.getName())))
        );

        resultActions.andExpect(status().isOk());
    }

    @Test
    public void should_update_when_updatingGenericJsonModelWithNameWithUppercase() throws Exception {
        String name = "NameWithUppercase";

        Model genericJsonModel = modelRepository.createModel(
            new ModelEntity(GENERIC_MODEL_NAME, genericJsonModelType.getName())
        );

        ResultActions resultActions = mockMvc.perform(
            put("/v1/models/{modelId}", genericJsonModel.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ModelEntity(name, genericJsonModelType.getName())))
        );

        resultActions.andExpect(status().isOk());
    }

    @Test
    public void should_returnStatusCreatedAndNullExtensions_when_creatingGenericJsonModelWithNullExtensions()
        throws Exception {
        Project project = projectRepository.createProject(project(GENERIC_PROJECT_NAME));

        Model genericJsonModel = modelRepository.createModel(
            new ModelEntity(GENERIC_MODEL_NAME, genericJsonModelType.getName())
        );
        Map<String, Object> extensions = null;

        genericJsonModel.setExtensions(extensions);

        mockMvc
            .perform(
                post("/v1/projects/{projectId}/models", project.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(genericJsonModel))
            )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.extensions").doesNotExist());
    }

    @Test
    public void should_returnStatusCreatedAndNotNullExtensions_when_creatingGenericJsonModelWithEmptyExtensions()
        throws Exception {
        Project project = projectRepository.createProject(project(GENERIC_PROJECT_NAME));

        Model genericJsonModel = modelRepository.createModel(
            new ModelEntity(GENERIC_MODEL_NAME, genericJsonModelType.getName())
        );
        Map<String, Object> extensions = new HashMap();

        genericJsonModel.setExtensions(extensions);

        mockMvc
            .perform(
                post("/v1/projects/{projectId}/models", project.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(genericJsonModel))
            )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.extensions", Matchers.notNullValue()));
    }

    @Test
    public void should_returnStatusCreatedAndExtensions_when_creatingGenericJsonModelWithValidExtensions()
        throws Exception {
        Project project = projectRepository.createProject(project(GENERIC_PROJECT_NAME));

        Model genericJsonModel = modelRepository.createModel(
            new ModelEntity(GENERIC_MODEL_NAME, genericJsonModelType.getName())
        );
        Map<String, Object> extensions = new HashMap();
        extensions.put("string", "value");
        extensions.put("number", 2);
        extensions.put("array", new String[] { "a", "b", "c" });
        extensions.put("list", Arrays.asList("a", "b", "c", "d"));

        genericJsonModel.setExtensions(extensions);

        mockMvc
            .perform(
                post("/v1/projects/{projectId}/models", project.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(genericJsonModel))
            )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.extensions.string", equalTo("value")))
            .andExpect(jsonPath("$.extensions.number", equalTo(2)))
            .andExpect(jsonPath("$.extensions.array", hasSize(3)))
            .andExpect(jsonPath("$.extensions.list", hasSize(4)));
    }
}
