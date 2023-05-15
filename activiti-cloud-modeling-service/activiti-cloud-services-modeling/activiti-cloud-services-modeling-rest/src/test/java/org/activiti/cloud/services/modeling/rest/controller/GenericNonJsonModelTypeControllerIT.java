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
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.activiti.cloud.modeling.api.Model;
import org.activiti.cloud.modeling.api.ModelType;
import org.activiti.cloud.modeling.api.Project;
import org.activiti.cloud.modeling.repository.ModelRepository;
import org.activiti.cloud.modeling.repository.ProjectRepository;
import org.activiti.cloud.services.modeling.config.ModelingRestApplication;
import org.activiti.cloud.services.modeling.entity.ModelEntity;
import org.activiti.cloud.services.modeling.security.WithMockModelerUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.ResourceLocks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for models rest api dealing with a non JSON models
 */
@ActiveProfiles(profiles = { "test", "generic" })
@SpringBootTest(classes = ModelingRestApplication.class)
@Transactional
@WebAppConfiguration
@WithMockModelerUser
@AutoConfigureMockMvc
public class GenericNonJsonModelTypeControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ModelRepository modelRepository;

    @Autowired
    ModelType genericNonJsonModelType;

    private static final String GENERIC_MODEL_NAME = "simple-model";

    private static final String GENERIC_PROJECT_NAME = "project-with-generic-model";

    private Project project;

    private Model genericNonJsonModel;

    @BeforeEach
    public void setUp() {
        project = null;
        genericNonJsonModel = null;
    }

    @AfterEach
    public void cleanUp() {
        if (genericNonJsonModel != null) {
            modelRepository.deleteModel(genericNonJsonModel);
        }

        if (project != null) {
            projectRepository.deleteProject(project);
        }
    }

    @Test
    @ResourceLock(value = GENERIC_PROJECT_NAME)
    public void should_returnStatusCreatedAndModelName_when_creatingGenericNonJsonModel() throws Exception {
        String name = GENERIC_MODEL_NAME;

        project = projectRepository.createProject(project(GENERIC_PROJECT_NAME));

        mockMvc
            .perform(
                post("/v1/projects/{projectId}/models", project.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(new ModelEntity(name, genericNonJsonModelType.getName())))
            )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name", equalTo(GENERIC_MODEL_NAME)));
    }

    @Test
    @ResourceLock(value = GENERIC_PROJECT_NAME)
    public void should_throwRequiredFieldException_when_creatingGenericNonJsonModelWithNameNull() throws Exception {
        String name = null;

        project = projectRepository.createProject(project(GENERIC_PROJECT_NAME));

        ResultActions resultActions = mockMvc.perform(
            post("/v1/projects/{projectId}/models", project.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ModelEntity(name, genericNonJsonModelType.getName())))
        );

        resultActions.andExpect(status().isBadRequest());
        assertThatResponse(resultActions.andReturn())
            .isValidationException()
            .hasValidationErrorCodes("field.required")
            .hasValidationErrorMessages("The model name is required");
    }

    @Test
    @ResourceLock(value = GENERIC_PROJECT_NAME)
    public void should_throwEmptyNameException_when_creatingGenericNonJsonModelWithNameEmpty() throws Exception {
        String name = "";

        project = projectRepository.createProject(project(GENERIC_PROJECT_NAME));

        ResultActions resultActions = mockMvc.perform(
            post("/v1/projects/{projectId}/models", project.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ModelEntity(name, genericNonJsonModelType.getName())))
        );

        resultActions.andExpect(status().isBadRequest());
        assertThatResponse(resultActions.andReturn())
            .isValidationException()
            .hasValidationErrorCodes("field.empty")
            .hasValidationErrorMessages("The model name cannot be empty");
    }

    @Test
    public void should_throwTooLongNameException_when_creatingGenericNonJsonModelWithNameTooLong() throws Exception {
        String name = "123456789_123456789_1234567";

        project = projectRepository.createProject(project(GENERIC_PROJECT_NAME));

        ResultActions resultActions = mockMvc.perform(
            post("/v1/projects/{projectId}/models", project.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ModelEntity(name, genericNonJsonModelType.getName())))
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
    @ResourceLock(value = GENERIC_PROJECT_NAME)
    public void should_update_when_creatingGenericNonJsonModelWithNameWithUnderscore() throws Exception {
        String name = "name_with_underscore";

        project = projectRepository.createProject(project(GENERIC_PROJECT_NAME));

        ResultActions resultActions = mockMvc.perform(
            post("/v1/projects/{projectId}/models", project.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ModelEntity(name, genericNonJsonModelType.getName())))
        );

        resultActions.andExpect(status().isCreated());
    }

    @Test
    @ResourceLock(value = GENERIC_PROJECT_NAME)
    public void should_create_when_creatingGenericNonJsonModelWithNameWithUppercase() throws Exception {
        String name = "NameWithUppercase";

        project = projectRepository.createProject(project(GENERIC_PROJECT_NAME));

        ResultActions resultActions = mockMvc.perform(
            post("/v1/projects/{projectId}/models", project.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ModelEntity(name, genericNonJsonModelType.getName())))
        );

        resultActions.andExpect(status().isCreated());
    }

    @Test
    @ResourceLocks({ @ResourceLock(value = GENERIC_MODEL_NAME), @ResourceLock(value = "updated-connector-name") })
    public void should_returnStatusOKAndModelName_when_updatingGenericNonJsonModel() throws Exception {
        String name = "updated-connector-name";

        genericNonJsonModel =
            modelRepository.createModel(new ModelEntity(GENERIC_MODEL_NAME, genericNonJsonModelType.getName()));

        mockMvc
            .perform(
                put("/v1/models/{modelId}", genericNonJsonModel.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(new ModelEntity(name, genericNonJsonModelType.getName())))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name", equalTo(name)));
    }

    @Test
    @ResourceLocks({ @ResourceLock(value = GENERIC_MODEL_NAME) })
    public void should_returnStatusOKAndModelName_when_updatingGenericNonJsonModelWithNameNull() throws Exception {
        String name = null;

        genericNonJsonModel =
            modelRepository.createModel(new ModelEntity(GENERIC_MODEL_NAME, genericNonJsonModelType.getName()));

        mockMvc
            .perform(
                put("/v1/models/{modelId}", genericNonJsonModel.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(new ModelEntity(name, genericNonJsonModelType.getName())))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name", equalTo(GENERIC_MODEL_NAME)));
    }

    @Test
    @ResourceLocks({ @ResourceLock(value = GENERIC_MODEL_NAME) })
    public void should_throwBadNameException_when_updatingGenericNonJsonModelWithNameEmpty() throws Exception {
        String name = "";

        genericNonJsonModel =
            modelRepository.createModel(new ModelEntity(GENERIC_MODEL_NAME, genericNonJsonModelType.getName()));

        ResultActions resultActions = mockMvc.perform(
            put("/v1/models/{modelId}", genericNonJsonModel.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ModelEntity(name, genericNonJsonModelType.getName())))
        );

        resultActions.andExpect(status().isBadRequest());
        assertThatResponse(resultActions.andReturn())
            .isValidationException()
            .hasValidationErrorCodes("field.empty")
            .hasValidationErrorMessages("The model name cannot be empty");
    }

    @Test
    @ResourceLocks({ @ResourceLock(value = GENERIC_MODEL_NAME) })
    public void should_throwBadNameException_when_updatingGenericNonJsonModelWithNameTooLong() throws Exception {
        String name = "123456789_123456789_1234567";

        genericNonJsonModel =
            modelRepository.createModel(new ModelEntity(GENERIC_MODEL_NAME, genericNonJsonModelType.getName()));

        ResultActions resultActions = mockMvc.perform(
            put("/v1/models/{modelId}", genericNonJsonModel.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ModelEntity(name, genericNonJsonModelType.getName())))
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
    @ResourceLocks({ @ResourceLock(value = GENERIC_MODEL_NAME), @ResourceLock(value = "name_with_underscore") })
    public void should_update_when_updatingGenericNonJsonModelWithNameWithUnderscore() throws Exception {
        String name = "name_with_underscore";

        genericNonJsonModel =
            modelRepository.createModel(new ModelEntity(GENERIC_MODEL_NAME, genericNonJsonModelType.getName()));

        ResultActions resultActions = mockMvc.perform(
            put("/v1/models/{modelId}", genericNonJsonModel.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ModelEntity(name, genericNonJsonModelType.getName())))
        );

        resultActions.andExpect(status().isOk());
    }

    @Test
    @ResourceLocks({ @ResourceLock(value = GENERIC_MODEL_NAME), @ResourceLock(value = "NameWithUppercase") })
    public void should_update_when_updatingGenericNonJsonModelWithNameWithUppercase() throws Exception {
        String name = "NameWithUppercase";

        genericNonJsonModel =
            modelRepository.createModel(new ModelEntity(GENERIC_MODEL_NAME, genericNonJsonModelType.getName()));

        ResultActions resultActions = mockMvc.perform(
            put("/v1/models/{modelId}", genericNonJsonModel.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ModelEntity(name, genericNonJsonModelType.getName())))
        );

        resultActions.andExpect(status().isOk());
    }

    @Test
    @ResourceLocks({ @ResourceLock(value = GENERIC_MODEL_NAME), @ResourceLock(value = GENERIC_PROJECT_NAME) })
    public void should_returnStatusCreatedAndNullExtensions_when_creatingGenericNonJsonModelWithNullExtensions()
        throws Exception {
        Project project = projectRepository.createProject(project(GENERIC_PROJECT_NAME));

        genericNonJsonModel =
            modelRepository.createModel(new ModelEntity(GENERIC_MODEL_NAME, genericNonJsonModelType.getName()));
        Map<String, Object> extensions = null;

        genericNonJsonModel.setExtensions(extensions);

        mockMvc
            .perform(
                post("/v1/projects/{projectId}/models", project.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(genericNonJsonModel))
            )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.extensions").doesNotExist());
    }

    @Test
    @ResourceLocks({ @ResourceLock(value = GENERIC_MODEL_NAME), @ResourceLock(value = GENERIC_PROJECT_NAME) })
    public void should_returnStatusCreatedAndNotNullExtensions_when_creatingGenericNonJsonModelWithEmptyExtensions()
        throws Exception {
        Project project = projectRepository.createProject(project(GENERIC_PROJECT_NAME));

        genericNonJsonModel =
            modelRepository.createModel(new ModelEntity(GENERIC_MODEL_NAME, genericNonJsonModelType.getName()));
        Map<String, Object> extensions = new HashMap();

        genericNonJsonModel.setExtensions(extensions);

        mockMvc
            .perform(
                post("/v1/projects/{projectId}/models", project.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(genericNonJsonModel))
            )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.extensions", notNullValue()));
    }

    @Test
    @ResourceLocks({ @ResourceLock(value = GENERIC_MODEL_NAME), @ResourceLock(value = GENERIC_PROJECT_NAME) })
    public void should_returnStatusCreatedAndExtensions_when_creatingGenericNonJsonModelWithValidExtensions()
        throws Exception {
        Project project = projectRepository.createProject(project(GENERIC_PROJECT_NAME));

        genericNonJsonModel =
            modelRepository.createModel(new ModelEntity(GENERIC_MODEL_NAME, genericNonJsonModelType.getName()));
        Map<String, Object> extensions = new HashMap();
        extensions.put("string", "value");
        extensions.put("number", 2);
        extensions.put("array", new String[] { "a", "b", "c" });
        extensions.put("list", Arrays.asList("a", "b", "c", "d"));

        genericNonJsonModel.setExtensions(extensions);

        mockMvc
            .perform(
                post("/v1/projects/{projectId}/models", project.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(genericNonJsonModel))
            )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.extensions.string", equalTo("value")))
            .andExpect(jsonPath("$.extensions.number", equalTo(2)))
            .andExpect(jsonPath("$.extensions.array", hasSize(3)))
            .andExpect(jsonPath("$.extensions.list", hasSize(4)));
    }
}
